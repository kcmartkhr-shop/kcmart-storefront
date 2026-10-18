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
class RazorpayRefundRecoveryDispatchRaceTest {

    private static final String TEST_DB =
        "kcmart_refund_recovery_dispatch_race_test";

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

    @Test
    void recoveryBeforeDispatchMustBlockDispatch() {
        Fixture f = fixture();

        recovery.markOutcomeUnknown(
            f.attemptId(), "operator-crossrace", "Recovery-first test."
        );

        IllegalStateException rejected = assertThrows(
            IllegalStateException.class,
            () -> dispatch.recordDispatchIntent(
                f.attemptId(), "worker-after-recovery"
            )
        );

        assertTrue(rejected.getMessage().contains("not in PROCESSING"));
        assertQuarantined(f, 0);
        System.out.println("RECOVERY-FIRST DISPATCH REJECTION VERIFIED");
    }

    @Test
    void dispatchBeforeRecoveryMustPreserveOneIntent() {
        Fixture f = fixture();

        dispatch.recordDispatchIntent(
            f.attemptId(), "worker-before-recovery"
        );

        recovery.markOutcomeUnknown(
            f.attemptId(), "operator-crossrace", "Dispatch-first test."
        );

        assertQuarantined(f, 1);
        System.out.println("DISPATCH-FIRST INTENT PRESERVATION VERIFIED");
    }

    @Test
    void simultaneousRecoveryAndDispatchMustRemainConsistent()
            throws Exception {

        for (int iteration = 0; iteration < 5; iteration++) {
            Fixture f = fixture();

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<Boolean> recoveryResult = executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Recovery start timeout");
                    }
                    recovery.markOutcomeUnknown(
                        f.attemptId(),
                        "operator-crossrace",
                        "Simultaneous recovery and dispatch."
                    );
                    return true;
                });

                Future<Boolean> dispatchResult = executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Dispatch start timeout");
                    }

                    try {
                        dispatch.recordDispatchIntent(
                            f.attemptId(), "worker-crossrace"
                        );
                        return true;
                    } catch (IllegalStateException expected) {
                        if (!expected.getMessage().contains(
                                "not in PROCESSING")) {
                            throw expected;
                        }
                        return false;
                    }
                });

                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();

                assertTrue(recoveryResult.get(30, TimeUnit.SECONDS));
                boolean dispatched =
                    dispatchResult.get(30, TimeUnit.SECONDS);

                assertQuarantined(f, dispatched ? 1 : 0);
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
            "SIMULTANEOUS RECOVERY/DISPATCH VERIFIED: 5 iterations"
        );
        System.out.println("DUPLICATE DISPATCH INTENTS: NONE");
        System.out.println("FINAL STATE: UNKNOWN / MANUAL_REVIEW");
        System.out.println("RAZORPAY API CALLS: NONE");
    }
}