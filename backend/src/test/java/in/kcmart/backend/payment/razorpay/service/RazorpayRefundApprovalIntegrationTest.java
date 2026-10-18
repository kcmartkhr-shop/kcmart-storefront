package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
@Transactional
class RazorpayRefundApprovalIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RazorpayRefundApprovalService approval;

    private record Fixture(Long reconciliationId, String paymentId) {}

    private Fixture createFixture(String status) {
        assertEquals(
            "kcmart_test",
            jdbc.queryForObject("SELECT current_database()", String.class),
            "SAFETY STOP: Tests must run only on kcmart_test"
        );

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "refund-" + suffix + "@example.invalid";
        String gatewayOrder = "order_refund_" + suffix;
        String gatewayPayment = "pay_refund_" + suffix;

        Long customerId = jdbc.queryForObject(
            """
            INSERT INTO customers (provider, provider_subject, email)
            VALUES ('TEST', ?, ?)
            RETURNING id
            """,
            Long.class,
            suffix,
            email
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
                    'Refund Test Customer', ?, '9999999999',
                    'Test Address', 'Test City', 'Odisha', '751001', 'India',
                    100.00, 0.00, 100.00,
                    'ONLINE', 'FAILED', 'CANCELLED')
            RETURNING id
            """,
            Long.class,
            "REFUND-" + suffix,
            customerId,
            email
        );

        assertTrue(orderId > 10L);

        Long paymentAttemptId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_attempts
                (order_id, razorpay_order_id, amount_paise, currency, status)
            VALUES (?, ?, 10000, 'INR', 'CREATED')
            RETURNING id
            """,
            Long.class,
            orderId,
            gatewayOrder
        );

        Long reconciliationId = jdbc.queryForObject(
            """
            INSERT INTO razorpay_payment_reconciliations
                (order_id, payment_attempt_id, razorpay_order_id,
                 razorpay_payment_id, amount_paise, currency,
                 reason, status)
            VALUES (?, ?, ?, ?, 10000, 'INR',
                    'CAPTURED_AFTER_ORDER_CANCELLATION', ?)
            RETURNING id
            """,
            Long.class,
            orderId,
            paymentAttemptId,
            gatewayOrder,
            gatewayPayment,
            status
        );

        return new Fixture(reconciliationId, gatewayPayment);
    }

    private int count(String sql, Object value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }

    @Test
    void approvalCreatesOneAttemptAndOneAuditEvent() {
        Fixture fixture = createFixture("CAPTURED_REVIEW");
        String key = "refund-approval-" + UUID.randomUUID();

        RazorpayRefundAttempt first = approval.approveFullRefund(
            fixture.reconciliationId(), key, "admin-test"
        );

        assertNotNull(first.getId());
        assertEquals("PENDING", first.getStatus());
        assertEquals(10000L, first.getAmountPaise());
        assertEquals("INR", first.getCurrency());
        assertEquals(fixture.paymentId(), first.getRazorpayPaymentId());

        assertEquals(
            "REFUND_PENDING",
            jdbc.queryForObject(
                "SELECT status FROM razorpay_payment_reconciliations WHERE id = ?",
                String.class,
                fixture.reconciliationId()
            )
        );

        RazorpayRefundAttempt repeated = approval.approveFullRefund(
            fixture.reconciliationId(), key, "admin-test"
        );

        assertEquals(first.getId(), repeated.getId());

        assertEquals(
            1,
            count(
                "SELECT COUNT(*) FROM razorpay_refund_attempts WHERE reconciliation_id = ?",
                fixture.reconciliationId()
            )
        );

        assertEquals(
            1,
            count(
                "SELECT COUNT(*) FROM razorpay_refund_audit_events WHERE refund_attempt_id = ?",
                first.getId()
            )
        );

        assertEquals(
            "REFUND_APPROVED",
            jdbc.queryForObject(
                "SELECT event_type FROM razorpay_refund_audit_events WHERE refund_attempt_id = ?",
                String.class,
                first.getId()
            )
        );

        assertNull(
            jdbc.queryForObject(
                "SELECT razorpay_refund_id FROM razorpay_refund_attempts WHERE id = ?",
                String.class,
                first.getId()
            )
        );

        System.out.println(
            "REFUND APPROVAL VERIFIED: one PENDING attempt, one audit event, duplicate safe"
        );
    }

    @Test
    void conflictingApprovalMustBeRejected() {
        Fixture fixture = createFixture("CAPTURED_REVIEW");

        approval.approveFullRefund(
            fixture.reconciliationId(),
            "refund-" + UUID.randomUUID(),
            "admin-test"
        );

        assertThrows(
            IllegalStateException.class,
            () -> approval.approveFullRefund(
                fixture.reconciliationId(),
                "different-" + UUID.randomUUID(),
                "admin-test"
            )
        );

        assertEquals(
            1,
            count(
                "SELECT COUNT(*) FROM razorpay_refund_attempts WHERE reconciliation_id = ?",
                fixture.reconciliationId()
            )
        );

        System.out.println("CONFLICTING APPROVAL VERIFIED: rejected");
    }

    @Test
    void invalidReconciliationStatusMustBeRejected() {
        Fixture fixture = createFixture("MANUAL_REVIEW");

        assertThrows(
            IllegalStateException.class,
            () -> approval.approveFullRefund(
                fixture.reconciliationId(),
                "refund-" + UUID.randomUUID(),
                "admin-test"
            )
        );

        assertEquals(
            0,
            count(
                "SELECT COUNT(*) FROM razorpay_refund_attempts WHERE reconciliation_id = ?",
                fixture.reconciliationId()
            )
        );

        System.out.println("INVALID STATUS VERIFIED: rejected");
    }

    @Test
    void repeatedApprovalWithTrimmedActorMustBeIdempotent() {
        Fixture fixture = createFixture("CAPTURED_REVIEW");
        String key = "refund-" + UUID.randomUUID();

        RazorpayRefundAttempt first = approval.approveFullRefund(
            fixture.reconciliationId(), key, " admin-test "
        );

        RazorpayRefundAttempt repeated = approval.approveFullRefund(
            fixture.reconciliationId(), key, "admin-test"
        );

        assertEquals(first.getId(), repeated.getId());
        assertEquals("admin-test", first.getApprovedBy());

        System.out.println("ACTOR NORMALIZATION VERIFIED");
    }
}