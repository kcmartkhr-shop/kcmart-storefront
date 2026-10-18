package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class RazorpayRefundRecoveryConcurrencyTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayRefundApprovalService approval;

    @Autowired
    RazorpayRefundClaimService claiming;

    @Autowired
    RazorpayRefundRecoveryService recovery;

    private record Fixture(Long reconciliationId, Long attemptId) {}

    private Fixture fixture() {
        assertEquals(
            "kcmart_refund_recovery_concurrency_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "recoveryrace-" + suffix + "@example.invalid";
        String gatewayOrder = "order_recoveryrace_" + suffix;
        String gatewayPayment = "pay_recoveryrace_" + suffix;

        Long customerId = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class, suffix, email
        );

        Long orderId = jdbc.queryForObject(
            """
            INSERT INTO orders
                (order_number, customer_id, fulfillment_type,
                 customer_name, customer_email, customer_phone,
                 address_line1, city, state, postal_code, country,
                 subtotal, delivery_charge, total_amount,
                 payment_method, payment_status, order_status)
            VALUES (?, ?, 'DELIVERY',
                    'Recovery Race Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "RRC-" + suffix.substring(0, 24),
            customerId, email
        );

        Long paymentAttemptId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_attempts
                (order_id, razorpay_order_id, amount_paise, currency, status)
            VALUES (?, ?, 10000, 'INR', 'CREATED')
            RETURNING id
            """,
            Long.class, orderId, gatewayOrder
        );

        Long reconciliationId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_reconciliations
                (order_id, payment_attempt_id, razorpay_order_id,
                 razorpay_payment_id, amount_paise, currency,
                 reason, status)
            VALUES (?, ?, ?, ?, 10000, 'INR',
                    'CAPTURED_AFTER_ORDER_CANCELLATION', 'CAPTURED_REVIEW')
            RETURNING id
            """,
            Long.class,
            orderId,
            paymentAttemptId,
            gatewayOrder,
            gatewayPayment
        );

        RazorpayRefundAttempt approved = approval.approveFullRefund(
            reconciliationId,
            "recovery-race-" + suffix,
            "admin-race"
        );

        claiming.claimPendingRefund(
            approved.getId(),
            "worker-race"
        );

        assertEquals("PROCESSING", approvedStatus(approved.getId()));

        return new Fixture(reconciliationId, approved.getId());
    }

    private String approvedStatus(Long id) {
        return jdbc.queryForObject(
            "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
            String.class, id
        );
    }

    @Test
    void simultaneousRecoveryMustCreateOneUnknownAudit() throws Exception {
        Fixture f = fixture();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Long> worker = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timeout");
                }

                RazorpayRefundAttempt result = recovery.markOutcomeUnknown(
                    f.attemptId(),
                    Thread.currentThread().getName(),
                    "Concurrent operator recovery test."
                );

                return result.getId();
            };

            Future<Long> first = executor.submit(worker);
            Future<Long> second = executor.submit(worker);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Long firstId = first.get(30, TimeUnit.SECONDS);
            Long secondId = second.get(30, TimeUnit.SECONDS);

            assertEquals(f.attemptId(), firstId);
            assertEquals(firstId, secondId);

            assertEquals("UNKNOWN", approvedStatus(f.attemptId()));

            assertEquals(
                "MANUAL_REVIEW",
                jdbc.queryForObject(
                    """
                    SELECT status
                    FROM razorpay_payment_reconciliations
                    WHERE id = ?
                    """,
                    String.class, f.reconciliationId()
                )
            );

            Integer unknownEvents = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                  AND event_type = 'REFUND_OUTCOME_UNKNOWN'
                """,
                Integer.class, f.attemptId()
            );

            Integer totalEvents = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                """,
                Integer.class, f.attemptId()
            );

            assertEquals(1, unknownEvents);
            assertEquals(3, totalEvents);

            Integer attemptCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_attempts
                WHERE reconciliation_id = ?
                """,
                Integer.class, f.reconciliationId()
            );

            assertEquals(1, attemptCount);

            assertNull(
                jdbc.queryForObject(
                    """
                    SELECT razorpay_refund_id
                    FROM razorpay_refund_attempts
                    WHERE id = ?
                    """,
                    String.class, f.attemptId()
                )
            );

            System.out.println(
                "CONCURRENT RECOVERY VERIFIED: both operators returned same attempt"
            );
            System.out.println(
                "UNKNOWN AUDIT VERIFIED: exactly one event"
            );
            System.out.println(
                "AUDIT HISTORY VERIFIED: approved + claimed + unknown"
            );
            System.out.println(
                "REFUND STATE VERIFIED: UNKNOWN / MANUAL_REVIEW"
            );
            System.out.println(
                "GATEWAY REFUND ID VERIFIED: NULL"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();

            assertTrue(
                executor.awaitTermination(10, TimeUnit.SECONDS),
                "Worker threads did not terminate"
            );
        }
    }
}