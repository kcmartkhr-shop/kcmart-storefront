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
class RazorpayRefundRecoveryIntegrationTest {

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
            "kcmart_refund_recovery_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "recovery-" + suffix + "@example.invalid";
        String gatewayOrder = "order_recovery_" + suffix;
        String gatewayPayment = "pay_recovery_" + suffix;

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
                    'Recovery Test Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "RCV-" + suffix.substring(0, 24),
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

        RazorpayRefundAttempt attempt = approval.approveFullRefund(
            reconciliationId,
            "recovery-test-" + suffix,
            "admin-recovery-test"
        );

        return new Fixture(reconciliationId, attempt.getId());
    }

    private String attemptStatus(Long id) {
        return jdbc.queryForObject(
            "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
            String.class, id
        );
    }

    private String reconciliationStatus(Long id) {
        return jdbc.queryForObject(
            """
            SELECT status
            FROM razorpay_payment_reconciliations
            WHERE id = ?
            """,
            String.class, id
        );
    }

    private int eventCount(Long id, String type) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ?
              AND event_type = ?
            """,
            Integer.class, id, type
        );
    }

    @Test
    void processingRefundCanBeQuarantined() {
        Fixture f = fixture();

        claiming.claimPendingRefund(f.attemptId(), "worker-recovery-test");

        RazorpayRefundAttempt result = recovery.markOutcomeUnknown(
            f.attemptId(),
            "admin-recovery-test",
            "Gateway response uncertain; manual verification required."
        );

        assertEquals(f.attemptId(), result.getId());
        assertEquals("UNKNOWN", attemptStatus(f.attemptId()));
        assertEquals("MANUAL_REVIEW", reconciliationStatus(f.reconciliationId()));

        assertEquals(
            1,
            eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN")
        );

        assertEquals(
            1,
            eventCount(f.attemptId(), "REFUND_CLAIMED")
        );

        assertEquals(
            1,
            eventCount(f.attemptId(), "REFUND_APPROVED")
        );

        System.out.println("UNKNOWN OUTCOME QUARANTINE VERIFIED");
    }

    @Test
    void repeatedQuarantineDoesNotCreateDuplicateAudit() {
        Fixture f = fixture();

        claiming.claimPendingRefund(f.attemptId(), "worker-recovery-test");

        recovery.markOutcomeUnknown(
            f.attemptId(),
            "admin-first",
            "Uncertain gateway response."
        );

        recovery.markOutcomeUnknown(
            f.attemptId(),
            "admin-second",
            "Repeated quarantine request."
        );

        assertEquals("UNKNOWN", attemptStatus(f.attemptId()));
        assertEquals(1, eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN"));

        System.out.println("IDEMPOTENT RECOVERY VERIFIED");
    }

    @Test
    void pendingRefundCannotBeMarkedUnknown() {
        Fixture f = fixture();

        assertThrows(
            IllegalStateException.class,
            () -> recovery.markOutcomeUnknown(
                f.attemptId(),
                "admin-recovery-test",
                "Cannot quarantine an unclaimed refund."
            )
        );

        assertEquals("PENDING", attemptStatus(f.attemptId()));
        assertEquals("REFUND_PENDING", reconciliationStatus(f.reconciliationId()));
        assertEquals(0, eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN"));

        System.out.println("PENDING REFUND PROTECTION VERIFIED");
    }

    @Test
    void invalidOperatorMustNotChangeRefund() {
        Fixture f = fixture();

        claiming.claimPendingRefund(f.attemptId(), "worker-recovery-test");

        assertThrows(
            IllegalArgumentException.class,
            () -> recovery.markOutcomeUnknown(
                f.attemptId(),
                " ",
                "Uncertain gateway response."
            )
        );

        assertEquals("PROCESSING", attemptStatus(f.attemptId()));
        assertEquals("REFUND_IN_PROGRESS", reconciliationStatus(f.reconciliationId()));
        assertEquals(0, eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN"));

        System.out.println("INVALID RECOVERY OPERATOR REJECTED");
    }

    @Test
    void inconsistentReconciliationMustRollBackRecovery() {
        Fixture f = fixture();

        claiming.claimPendingRefund(f.attemptId(), "worker-recovery-test");

        jdbc.update(
            """
            UPDATE razorpay_payment_reconciliations
            SET status = 'REFUND_FAILED'
            WHERE id = ?
            """,
            f.reconciliationId()
        );

        assertThrows(
            IllegalStateException.class,
            () -> recovery.markOutcomeUnknown(
                f.attemptId(),
                "admin-recovery-test",
                "Unexpected reconciliation state."
            )
        );

        assertEquals("PROCESSING", attemptStatus(f.attemptId()));
        assertEquals("REFUND_FAILED", reconciliationStatus(f.reconciliationId()));
        assertEquals(0, eventCount(f.attemptId(), "REFUND_OUTCOME_UNKNOWN"));

        System.out.println("INCONSISTENT STATE RECOVERY REJECTED");
    }
}