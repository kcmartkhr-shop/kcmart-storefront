package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class RazorpayRefundOutcomeIntegrationTest {

    private static final String TEST_DB = "kcmart_refund_outcome_test";

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayRefundApprovalService approval;
    @Autowired RazorpayRefundClaimService claiming;
    @Autowired RazorpayRefundDispatchBarrierService dispatch;
    @Autowired RazorpayRefundRecoveryService recovery;
    @Autowired RazorpayRefundOutcomeService outcome;

    private record Fixture(
        Long reconciliationId,
        Long attemptId,
        String paymentId,
        String refundId
    ) {}

    private Fixture fixture(boolean recordDispatch) {
        assertEquals(
            TEST_DB,
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "outcome-" + suffix + "@example.invalid";
        String orderId = "order_outcome_" + suffix;
        String paymentId = "pay_outcome_" + suffix;
        String refundId = "rfnd_outcome_" + suffix;

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
                    'Outcome Test Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "OUT-" + suffix.substring(0, 24),
            customer, email
        );

        Long paymentAttempt = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_attempts
                (order_id, razorpay_order_id, amount_paise, currency, status)
            VALUES (?, ?, 10000, 'INR', 'CREATED')
            RETURNING id
            """,
            Long.class, order, orderId
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
            Long.class, order, paymentAttempt, orderId, paymentId
        );

        RazorpayRefundAttempt approved = approval.approveFullRefund(
            reconciliation, "outcome-" + suffix, "admin-outcome"
        );

        claiming.claimPendingRefund(approved.getId(), "worker-outcome");

        if (recordDispatch) {
            dispatch.recordDispatchIntent(approved.getId(), "worker-outcome");
        }

        return new Fixture(
            reconciliation, approved.getId(), paymentId, refundId
        );
    }

    private RazorpayRefundAttempt complete(Fixture f) {
        return outcome.recordVerifiedSuccess(
            f.attemptId(),
            f.refundId(),
            f.paymentId(),
            10000L,
            "INR",
            "verified-test-evidence"
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

    private int completionEvents(Fixture f) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ?
              AND event_type = 'REFUND_VERIFIED_SUCCEEDED'
            """,
            Integer.class, f.attemptId()
        );
    }

    @Test
    void verifiedSuccessCompletesBothRecords() {
        Fixture f = fixture(true);

        RazorpayRefundAttempt result = complete(f);

        assertEquals(f.attemptId(), result.getId());
        assertEquals("SUCCEEDED", attemptStatus(f));
        assertEquals("REFUNDED", reconciliationStatus(f));
        assertEquals(1, completionEvents(f));

        assertEquals(
            f.refundId(),
            jdbc.queryForObject(
                "SELECT razorpay_refund_id FROM razorpay_refund_attempts WHERE id = ?",
                String.class, f.attemptId()
            )
        );

        assertEquals(
            f.refundId(),
            jdbc.queryForObject(
                "SELECT razorpay_refund_id FROM razorpay_payment_reconciliations WHERE id = ?",
                String.class, f.reconciliationId()
            )
        );

        assertTrue(Boolean.TRUE.equals(jdbc.queryForObject(
            """
            SELECT completed_at IS NOT NULL
               AND gateway_response_received_at IS NOT NULL
            FROM razorpay_refund_attempts
            WHERE id = ?
            """,
            Boolean.class, f.attemptId()
        )));

        System.out.println("VERIFIED REFUND SUCCESS RECORDED");
    }

    @Test
    void identicalRetryDoesNotDuplicateAudit() {
        Fixture f = fixture(true);

        complete(f);
        complete(f);

        assertEquals("SUCCEEDED", attemptStatus(f));
        assertEquals("REFUNDED", reconciliationStatus(f));
        assertEquals(1, completionEvents(f));

        System.out.println("SUCCESS RETRY IDEMPOTENT");
    }

    @Test
    void conflictingRefundIdIsRejected() {
        Fixture f = fixture(true);

        complete(f);

        assertThrows(
            IllegalStateException.class,
            () -> outcome.recordVerifiedSuccess(
                f.attemptId(),
                "rfnd_conflicting_" + UUID.randomUUID(),
                f.paymentId(),
                10000L,
                "INR",
                "conflicting-evidence"
            )
        );

        assertEquals(1, completionEvents(f));
        assertEquals("SUCCEEDED", attemptStatus(f));

        System.out.println("CONFLICTING REFUND ID BLOCKED");
    }

    @Test
    void missingDispatchIntentBlocksCompletion() {
        Fixture f = fixture(false);

        assertThrows(
            IllegalStateException.class,
            () -> complete(f)
        );

        assertEquals("PROCESSING", attemptStatus(f));
        assertEquals("REFUND_IN_PROGRESS", reconciliationStatus(f));
        assertEquals(0, completionEvents(f));

        System.out.println("MISSING DISPATCH INTENT BLOCKED");
    }

    @Test
    void mismatchedPaymentAndAmountAreRejected() {
        Fixture f = fixture(true);

        assertThrows(
            IllegalStateException.class,
            () -> outcome.recordVerifiedSuccess(
                f.attemptId(),
                f.refundId(),
                "pay_wrong",
                10000L,
                "INR",
                "mismatch-test"
            )
        );

        assertThrows(
            IllegalStateException.class,
            () -> outcome.recordVerifiedSuccess(
                f.attemptId(),
                f.refundId(),
                f.paymentId(),
                9999L,
                "INR",
                "mismatch-test"
            )
        );

        assertEquals("PROCESSING", attemptStatus(f));
        assertEquals(0, completionEvents(f));

        System.out.println("PAYMENT AND AMOUNT MISMATCH BLOCKED");
    }

    @Test
    void recoveredUnknownRefundCannotBeAutomaticallyCompleted() {
        Fixture f = fixture(true);

        recovery.markOutcomeUnknown(
            f.attemptId(),
            "operator-outcome",
            "Uncertain refund outcome test"
        );

        assertThrows(
            IllegalStateException.class,
            () -> complete(f)
        );

        assertEquals("UNKNOWN", attemptStatus(f));
        assertEquals("MANUAL_REVIEW", reconciliationStatus(f));
        assertEquals(0, completionEvents(f));

        System.out.println("UNKNOWN REFUND REMAINS QUARANTINED");
    }
}