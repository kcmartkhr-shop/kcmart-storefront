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
class RazorpayRefundClaimConcurrencyTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayRefundApprovalService approval;

    @Autowired
    RazorpayRefundClaimService claiming;

    private record Fixture(Long reconciliationId, Long attemptId) {}

    private Fixture createFixture() {
        assertEquals(
            "kcmart_refund_claim_concurrency_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "claimrace-" + suffix + "@example.invalid";
        String gatewayOrder = "order_claimrace_" + suffix;
        String gatewayPayment = "pay_claimrace_" + suffix;

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
                    'Claim Race Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "CRC-" + suffix.substring(0, 24),
            customerId,
            email
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
            "claim-race-" + suffix,
            "admin-claim-race"
        );

        assertEquals("PENDING", approved.getStatus());

        return new Fixture(reconciliationId, approved.getId());
    }

    @Test
    void twoWorkersMustNotClaimSameRefund() throws Exception {
        Fixture f = createFixture();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> worker = () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start timeout");
            }

            try {
                RazorpayRefundAttempt claimed =
                    claiming.claimPendingRefund(
                        f.attemptId(),
                        Thread.currentThread().getName()
                    );

                assertEquals(f.attemptId(), claimed.getId());
                return true;

            } catch (IllegalStateException expected) {
                if (!"Refund attempt is not pending."
                        .equals(expected.getMessage())) {
                    throw expected;
                }

                return false;
            }
        };

        try {
            Future<Boolean> first = executor.submit(worker);
            Future<Boolean> second = executor.submit(worker);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            int successes =
                (firstResult ? 1 : 0) + (secondResult ? 1 : 0);

            assertEquals(1, successes, "Exactly one worker must win");

            assertEquals(
                "PROCESSING",
                jdbc.queryForObject(
                    "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
                    String.class,
                    f.attemptId()
                )
            );

            assertEquals(
                "REFUND_IN_PROGRESS",
                jdbc.queryForObject(
                    """
                    SELECT status
                    FROM razorpay_payment_reconciliations
                    WHERE id = ?
                    """,
                    String.class,
                    f.reconciliationId()
                )
            );

            Integer claimedEvents = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                  AND event_type = 'REFUND_CLAIMED'
                """,
                Integer.class,
                f.attemptId()
            );

            Integer approvalEvents = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                  AND event_type = 'REFUND_APPROVED'
                """,
                Integer.class,
                f.attemptId()
            );

            assertEquals(1, claimedEvents);
            assertEquals(1, approvalEvents);

            Integer attempts = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM razorpay_refund_attempts
                WHERE reconciliation_id = ?
                """,
                Integer.class,
                f.reconciliationId()
            );

            assertEquals(1, attempts);

            System.out.println(
                "CONCURRENT CLAIM VERIFIED: one worker succeeded"
            );
            System.out.println(
                "SECOND WORKER VERIFIED: rejected safely"
            );
            System.out.println(
                "CLAIM AUDIT VERIFIED: exactly one REFUND_CLAIMED event"
            );
            System.out.println(
                "REFUND STATE VERIFIED: PROCESSING / REFUND_IN_PROGRESS"
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