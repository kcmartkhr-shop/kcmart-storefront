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
class RazorpayRefundApprovalRetryRaceTest {

    private static final String TEST_DB =
        "kcmart_refund_approval_retry_race_test";

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayRefundApprovalService approval;
    @Autowired RazorpayRefundClaimService claiming;
    @Autowired RazorpayRefundRecoveryService recovery;
    @Autowired RazorpayRefundDispatchBarrierService dispatch;

    private record Fixture(Long reconciliationId, Long attemptId) {}

    private Fixture fixture() {
        assertEquals(
            TEST_DB,
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "crossrace-" + suffix + "@example.invalid";
        String gatewayOrder = "order_crossrace_" + suffix;
        String gatewayPayment = "pay_crossrace_" + suffix;

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
                    'Cross Race Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "XRC-" + suffix.substring(0, 24),
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
            Long.class, orderId, paymentAttemptId,
            gatewayOrder, gatewayPayment
        );

        RazorpayRefundAttempt attempt = approval.approveFullRefund(
            reconciliationId, "crossrace-" + suffix, "admin-crossrace"
        );

        claiming.claimPendingRefund(attempt.getId(), "worker-crossrace");

        return new Fixture(reconciliationId, attempt.getId());
    }

    private int eventCount(Long id, String type) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ? AND event_type = ?
            """,
            Integer.class, id, type
        );
    }

    private boolean hasDispatchTimestamp(Long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            """
            SELECT gateway_request_sent_at IS NOT NULL
            FROM razorpay_refund_attempts WHERE id = ?
            """,
            Boolean.class, id
        ));
    }

    private void assertQuarantined(Fixture f, int expectedDispatches) {
        assertEquals("UNKNOWN", jdbc.queryForObject(
            "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
            String.class, f.attemptId()
        ));

        assertEquals("MANUAL_REVIEW", jdbc.queryForObject(
            "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
            String.class, f.reconciliationId()
        ));

        assertEquals(1, eventCount(f.attemptId(), "REFUND_APPROVED"));
        assertEquals(1, eventCount(f.attemptId(), "REFUND_CLAIMED"));
        assertEquals(1, eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN"));
        assertEquals(
            expectedDispatches,
            eventCount(f.attemptId(), "REFUND_DISPATCH_INTENT_RECORDED")
        );
        assertEquals(expectedDispatches == 1,
                     hasDispatchTimestamp(f.attemptId()));

        assertEquals(1, jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM razorpay_refund_attempts
            WHERE reconciliation_id = ?
            """,
            Integer.class, f.reconciliationId()
        ));

        assertNull(jdbc.queryForObject(
            "SELECT razorpay_refund_id FROM razorpay_refund_attempts WHERE id = ?",
            String.class, f.attemptId()
        ));
    }

    private static final String ORIGINAL_KEY_PREFIX = "crossrace-";

    private String keyFor(Fixture f) {
        return jdbc.queryForObject(
            "SELECT idempotency_key FROM razorpay_refund_attempts WHERE id = ?",
            String.class,
            f.attemptId()
        );
    }

    private void assertSingleApproval(Fixture f) {
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM razorpay_refund_attempts WHERE reconciliation_id = ?",
            Integer.class,
            f.reconciliationId()
        ));

        assertEquals(1, eventCount(f.attemptId(), "REFUND_APPROVED"));
        assertEquals(1, eventCount(f.attemptId(), "REFUND_CLAIMED"));
    }

    @Test
    void retryAfterClaimReturnsSameAttempt() {
        Fixture f = fixture();

        RazorpayRefundAttempt retry = approval.approveFullRefund(
            f.reconciliationId(),
            keyFor(f),
            "admin-crossrace"
        );

        assertEquals(f.attemptId(), retry.getId());
        assertEquals("PROCESSING", retry.getStatus());

        assertSingleApproval(f);

        assertEquals("REFUND_IN_PROGRESS", jdbc.queryForObject(
            "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
            String.class,
            f.reconciliationId()
        ));

        System.out.println("APPROVAL RETRY AFTER CLAIM VERIFIED");
    }

    @Test
    void retryAfterRecoveryReturnsSameAttempt() {
        Fixture f = fixture();

        recovery.markOutcomeUnknown(
            f.attemptId(),
            "operator-retry",
            "Retry after recovery test."
        );

        RazorpayRefundAttempt retry = approval.approveFullRefund(
            f.reconciliationId(),
            keyFor(f),
            "admin-crossrace"
        );

        assertEquals(f.attemptId(), retry.getId());
        assertEquals("UNKNOWN", retry.getStatus());

        assertQuarantined(f, 0);
        assertSingleApproval(f);

        System.out.println("APPROVAL RETRY AFTER RECOVERY VERIFIED");
    }

    @Test
    void conflictingRetryAfterRecoveryIsRejected() {
        Fixture f = fixture();

        recovery.markOutcomeUnknown(
            f.attemptId(),
            "operator-retry",
            "Conflicting retry test."
        );

        assertThrows(
            IllegalStateException.class,
            () -> approval.approveFullRefund(
                f.reconciliationId(),
                "different-" + UUID.randomUUID(),
                "admin-crossrace"
            )
        );

        assertThrows(
            IllegalStateException.class,
            () -> approval.approveFullRefund(
                f.reconciliationId(),
                keyFor(f),
                "different-admin"
            )
        );

        assertQuarantined(f, 0);
        assertSingleApproval(f);

        System.out.println("CONFLICTING APPROVAL RETRIES REJECTED");
    }

    @Test
    void simultaneousRetryAndRecoveryRemainConsistent() throws Exception {
        for (int iteration = 0; iteration < 5; iteration++) {
            Fixture f = fixture();
            String key = keyFor(f);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<Long> retryFuture = executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Retry start timeout");
                    }

                    return approval.approveFullRefund(
                        f.reconciliationId(),
                        key,
                        "admin-crossrace"
                    ).getId();
                });

                Future<Boolean> recoveryFuture = executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Recovery start timeout");
                    }

                    recovery.markOutcomeUnknown(
                        f.attemptId(),
                        "operator-retry",
                        "Concurrent approval retry and recovery."
                    );

                    return true;
                });

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                assertEquals(f.attemptId(), retryFuture.get(30, TimeUnit.SECONDS));
                assertTrue(recoveryFuture.get(30, TimeUnit.SECONDS));

                assertQuarantined(f, 0);
                assertSingleApproval(f);
            } finally {
                start.countDown();
                executor.shutdownNow();

                assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Worker threads did not terminate"
                );
            }
        }

        System.out.println(
            "APPROVAL RETRY VS RECOVERY VERIFIED: 5 iterations"
        );
    }
}