package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class RazorpayRefundDispatchBarrierConcurrencyTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayRefundApprovalService approval;
    @Autowired RazorpayRefundClaimService claiming;
    @Autowired RazorpayRefundDispatchBarrierService barrier;
    @Autowired PlatformTransactionManager transactionManager;

    private record Fixture(Long reconciliationId, Long attemptId) {}

    private Fixture fixture(boolean claim) {
        assertEquals(
            "kcmart_refund_dispatch_concurrency_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "dispatch-" + suffix + "@example.invalid";
        String gatewayOrder = "order_dispatch_" + suffix;
        String gatewayPayment = "pay_dispatch_" + suffix;

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
                    'Dispatch Test Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "DSP-" + suffix.substring(0, 24),
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
            orderId, paymentAttemptId, gatewayOrder, gatewayPayment
        );

        RazorpayRefundAttempt attempt = approval.approveFullRefund(
            reconciliationId, "dispatch-" + suffix, "admin-dispatch-test"
        );

        if (claim) {
            claiming.claimPendingRefund(attempt.getId(), "worker-dispatch-test");
        }

        return new Fixture(reconciliationId, attempt.getId());
    }

    private int countEvents(Long id) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ?
              AND event_type = 'REFUND_DISPATCH_INTENT_RECORDED'
            """,
            Integer.class, id
        );
    }

    private boolean hasTimestamp(Long id) {
        return Boolean.TRUE.equals(
            jdbc.queryForObject(
                """
                SELECT gateway_request_sent_at IS NOT NULL
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                Boolean.class, id
            )
        );
    }

    @Test
    void twoWorkersMustNotRecordTwoDispatchIntents() throws Exception {
        Fixture f = fixture(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> worker = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timeout");
                }

                try {
                    barrier.recordDispatchIntent(
                        f.attemptId(),
                        Thread.currentThread().getName()
                    );
                    return true;
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().contains(
                            "Dispatch intent already recorded")) {
                        throw expected;
                    }
                    return false;
                }
            };

            Future<Boolean> first = executor.submit(worker);
            Future<Boolean> second = executor.submit(worker);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            boolean firstResult = first.get(30, TimeUnit.SECONDS);
            boolean secondResult = second.get(30, TimeUnit.SECONDS);

            int successes =
                (firstResult ? 1 : 0) + (secondResult ? 1 : 0);

            assertEquals(1, successes);
            assertTrue(hasTimestamp(f.attemptId()));
            assertEquals(1, countEvents(f.attemptId()));

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
                "CONCURRENT DISPATCH VERIFIED: exactly one worker succeeded"
            );
            System.out.println(
                "SECOND DISPATCH VERIFIED: safely rejected"
            );
            System.out.println(
                "DISPATCH AUDIT VERIFIED: exactly one intent event"
            );
            System.out.println(
                "REFUND STATE VERIFIED: PROCESSING / REFUND_IN_PROGRESS"
            );
            System.out.println(
                "GATEWAY NETWORK CALLS: NONE"
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