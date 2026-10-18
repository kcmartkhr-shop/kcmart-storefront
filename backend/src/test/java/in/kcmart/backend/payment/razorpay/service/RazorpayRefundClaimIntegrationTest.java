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
class RazorpayRefundClaimIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayRefundApprovalService approval;

    @Autowired
    RazorpayRefundClaimService claiming;

    private record Fixture(Long reconciliationId, Long refundAttemptId) {}

    private Fixture fixture() {
        assertEquals(
            "kcmart_refund_claim_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Wrong database"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "claim-" + suffix + "@example.invalid";
        String gatewayOrder = "order_claim_" + suffix;
        String gatewayPayment = "pay_claim_" + suffix;

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
                    'Claim Test Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "CLM-" + suffix.substring(0, 24),
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
            "claim-test-" + suffix,
            "admin-claim-test"
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
            "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
            String.class, id
        );
    }

    private int claimedEvents(Long id) {
        return jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM razorpay_refund_audit_events
            WHERE refund_attempt_id = ?
              AND event_type = 'REFUND_CLAIMED'
            """,
            Integer.class, id
        );
    }

    @Test
    void validClaimTransitionsBothStatusesAndWritesAudit() {
        Fixture f = fixture();

        RazorpayRefundAttempt result = claiming.claimPendingRefund(
            f.refundAttemptId(), "worker-test-1"
        );

        assertEquals(f.refundAttemptId(), result.getId());
        assertEquals("PROCESSING", attemptStatus(f.refundAttemptId()));
        assertEquals("REFUND_IN_PROGRESS", reconciliationStatus(f.reconciliationId()));
        assertEquals(1, claimedEvents(f.refundAttemptId()));

        assertEquals(
            "SYSTEM",
            jdbc.queryForObject(
                """
                SELECT actor_type
                FROM razorpay_refund_audit_events
                WHERE refund_attempt_id = ?
                  AND event_type = 'REFUND_CLAIMED'
                """,
                String.class, f.refundAttemptId()
            )
        );

        System.out.println("VALID REFUND CLAIM VERIFIED");
    }

    @Test
    void repeatedClaimIsRejectedWithoutDuplicateAudit() {
        Fixture f = fixture();

        claiming.claimPendingRefund(f.refundAttemptId(), "worker-first");

        assertThrows(
            IllegalStateException.class,
            () -> claiming.claimPendingRefund(
                f.refundAttemptId(), "worker-second"
            )
        );

        assertEquals("PROCESSING", attemptStatus(f.refundAttemptId()));
        assertEquals(1, claimedEvents(f.refundAttemptId()));

        System.out.println("DUPLICATE REFUND CLAIM REJECTED");
    }

    @Test
    void invalidWorkerMustNotClaimRefund() {
        Fixture f = fixture();

        assertThrows(
            IllegalArgumentException.class,
            () -> claiming.claimPendingRefund(f.refundAttemptId(), " ")
        );

        assertEquals("PENDING", attemptStatus(f.refundAttemptId()));
        assertEquals("REFUND_PENDING", reconciliationStatus(f.reconciliationId()));
        assertEquals(0, claimedEvents(f.refundAttemptId()));

        System.out.println("INVALID WORKER REJECTED");
    }

    @Test
    void mismatchedReconciliationMustRollBackClaim() {
        Fixture f = fixture();

        jdbc.update(
            """
            UPDATE razorpay_payment_reconciliations
            SET status = 'MANUAL_REVIEW'
            WHERE id = ?
            """,
            f.reconciliationId()
        );

        assertThrows(
            IllegalStateException.class,
            () -> claiming.claimPendingRefund(
                f.refundAttemptId(), "worker-invalid-state"
            )
        );

        assertEquals("PENDING", attemptStatus(f.refundAttemptId()));
        assertEquals("MANUAL_REVIEW", reconciliationStatus(f.reconciliationId()));
        assertEquals(0, claimedEvents(f.refundAttemptId()));

        System.out.println("INVALID RECONCILIATION STATE VERIFIED");
    }
}