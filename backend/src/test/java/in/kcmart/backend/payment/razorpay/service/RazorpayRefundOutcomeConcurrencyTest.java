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
class RazorpayRefundOutcomeConcurrencyTest {

    private static final String TEST_DB =
        "kcmart_refund_outcome_concurrency_test";

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayRefundApprovalService approval;
    @Autowired RazorpayRefundClaimService claiming;
    @Autowired RazorpayRefundDispatchBarrierService dispatch;
    @Autowired RazorpayRefundRecoveryService recovery;
    @Autowired RazorpayRefundOutcomeService outcome;
    @Autowired PlatformTransactionManager transactionManager;

    private record Fixture(
        Long reconciliationId,
        Long attemptId,
        String paymentId,
        String refundId
    ) {}

    private Fixture fixture() {
        assertEquals(
            TEST_DB,
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "outcomerace-" + suffix + "@example.invalid";
        String gatewayOrder = "order_outcomerace_" + suffix;
        String paymentId = "pay_outcomerace_" + suffix;
        String refundId = "rfnd_outcomerace_" + suffix;

        Long customer = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class, suffix, email
        );

        Long order = jdbc.queryForObject(
            """
            INSERT INTO orders
                (order_number, customer_id, fulfillment_type,
                 customer_name, customer_email, customer_phone,
                 address_line1, city, state, postal_code, country,
                 subtotal, delivery_charge, total_amount,
                 payment_method, payment_status, order_status)
            VALUES (?, ?, 'DELIVERY',
                    'Outcome Race Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "ORC-" + suffix.substring(0, 24),
            customer, email
        );

        Long paymentAttempt = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_attempts
                (order_id, razorpay_order_id, amount_paise, currency, status)
            VALUES (?, ?, 10000, 'INR', 'CREATED')
            RETURNING id
            """,
            Long.class, order, gatewayOrder
        );

        Long reconciliation = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_reconciliations
                (order_id, payment_attempt_id, razorpay_order_id,
                 razorpay_payment_id, amount_paise, currency,
                 reason, status)
            VALUES (?, ?, ?, ?, 10000, 'INR',
                    'CAPTURED_AFTER_ORDER_CANCELLATION', 'CAPTURED_REVIEW')
            RETURNING id
            """,
            Long.class, order, paymentAttempt, gatewayOrder, paymentId
        );

        RazorpayRefundAttempt attempt = approval.approveFullRefund(
            reconciliation, "outcomerace-" + suffix, "admin-outcomerace"
        );

        claiming.claimPendingRefund(attempt.getId(), "worker-outcomerace");
        dispatch.recordDispatchIntent(attempt.getId(), "worker-outcomerace");

        return new Fixture(reconciliation, attempt.getId(), paymentId, refundId);
    }

    private RazorpayRefundAttempt complete(Fixture f, String refundId) {
        return outcome.recordVerifiedSuccess(
            f.attemptId(), refundId, f.paymentId(),
            10000L, "INR", "test-verified-evidence"
        );
    }

    private String attemptStatus(Fixture f) {
        return jdbc.queryForObject(
            "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
            String.class, f.attemptId()
        );
    }

    private String reconciliationStatus(Fixture f) {
        return jdbc.queryForObject(
            "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
            String.class, f.reconciliationId()
        );
    }

    private int eventCount(Fixture f, String type) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ? AND event_type = ?
            """,
            Integer.class, f.attemptId(), type
        );
    }

    private <T> Future<T> submit(
        ExecutorService executor,
        CountDownLatch ready,
        CountDownLatch start,
        Callable<T> operation
    ) {
        return executor.submit(() -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Race start timeout.");
            }

            return operation.call();
        });
    }

    @Test
    void simultaneousIdenticalSuccessCreatesOneAudit() throws Exception {
        Fixture f = fixture();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Long> operation = () ->
                complete(f, f.refundId()).getId();

            Future<Long> first = submit(executor, ready, start, operation);
            Future<Long> second = submit(executor, ready, start, operation);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            assertEquals(f.attemptId(), first.get(30, TimeUnit.SECONDS));
            assertEquals(f.attemptId(), second.get(30, TimeUnit.SECONDS));

            assertEquals("SUCCEEDED", attemptStatus(f));
            assertEquals("REFUNDED", reconciliationStatus(f));
            assertEquals(1, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));

            System.out.println("CONCURRENT IDENTICAL SUCCESS VERIFIED");
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void simultaneousConflictingRefundIdsCannotBothSucceed() throws Exception {
        Fixture f = fixture();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> firstOperation = () -> {
                try {
                    complete(f, f.refundId());
                    return true;
                } catch (IllegalStateException expected) {
                    return false;
                }
            };

            Callable<Boolean> secondOperation = () -> {
                try {
                    complete(f, "rfnd_conflict_" + f.refundId());
                    return true;
                } catch (IllegalStateException expected) {
                    return false;
                }
            };

            Future<Boolean> first =
                submit(executor, ready, start, firstOperation);
            Future<Boolean> second =
                submit(executor, ready, start, secondOperation);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successes =
                (first.get(30, TimeUnit.SECONDS) ? 1 : 0) +
                (second.get(30, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, successes);
            assertEquals("SUCCEEDED", attemptStatus(f));
            assertEquals("REFUNDED", reconciliationStatus(f));
            assertEquals(1, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));

            String storedId = jdbc.queryForObject(
                "SELECT razorpay_refund_id FROM razorpay_refund_attempts WHERE id = ?",
                String.class, f.attemptId()
            );

            String reconciliationRefundId = jdbc.queryForObject(
                "SELECT razorpay_refund_id FROM razorpay_payment_reconciliations WHERE id = ?",
                String.class, f.reconciliationId()
            );

            assertEquals(storedId, reconciliationRefundId);

            System.out.println("CONFLICTING CONCURRENT REFUNDS SAFELY SERIALIZED");
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void rollbackRemovesCompletionAndAudit() {
        Fixture f = fixture();

        TransactionTemplate transaction =
            new TransactionTemplate(transactionManager);

        assertThrows(
            IllegalStateException.class,
            () -> transaction.execute(status -> {
                complete(f, f.refundId());

                assertEquals("SUCCEEDED", attemptStatus(f));
                assertEquals(1, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));

                throw new IllegalStateException("Intentional rollback.");
            })
        );

        assertEquals("PROCESSING", attemptStatus(f));
        assertEquals("REFUND_IN_PROGRESS", reconciliationStatus(f));
        assertEquals(0, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));

        System.out.println("REFUND COMPLETION ROLLBACK VERIFIED");
    }

    @Test
    void simultaneousRecoveryAndSuccessLeaveConsistentState() throws Exception {
        for (int iteration = 0; iteration < 5; iteration++) {
            Fixture f = fixture();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<Boolean> success = submit(
                    executor, ready, start, () -> {
                        try {
                            complete(f, f.refundId());
                            return true;
                        } catch (IllegalStateException expected) {
                            return false;
                        }
                    }
                );

                Future<Boolean> quarantine = submit(
                    executor, ready, start, () -> {
                        try {
                            recovery.markOutcomeUnknown(
                                f.attemptId(),
                                "operator-outcomerace",
                                "Concurrent recovery and success test"
                            );
                            return true;
                        } catch (IllegalStateException expected) {
                            return false;
                        }
                    }
                );

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                boolean completed = success.get(30, TimeUnit.SECONDS);
                boolean recovered = quarantine.get(30, TimeUnit.SECONDS);

                assertNotEquals(completed, recovered);

                if (completed) {
                    assertEquals("SUCCEEDED", attemptStatus(f));
                    assertEquals("REFUNDED", reconciliationStatus(f));
                    assertEquals(1, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));
                    assertEquals(0, eventCount(f, "REFUND_OUTCOME_UNKNOWN"));
                } else {
                    assertEquals("UNKNOWN", attemptStatus(f));
                    assertEquals("MANUAL_REVIEW", reconciliationStatus(f));
                    assertEquals(0, eventCount(f, "REFUND_VERIFIED_SUCCEEDED"));
                    assertEquals(1, eventCount(f, "REFUND_OUTCOME_UNKNOWN"));
                }
            } finally {
                start.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            }
        }

        System.out.println("RECOVERY VS SUCCESS RACE VERIFIED: 5 ITERATIONS");
    }
}