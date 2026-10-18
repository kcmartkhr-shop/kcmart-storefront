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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.task.scheduling.enabled=false",
    "kcmart.reservation.expiry.enabled=false"
})
@ActiveProfiles("test")
class RazorpayRefundClaimTimestampTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired RazorpayRefundApprovalService approval;
    @Autowired RazorpayRefundClaimService claiming;
    @Autowired RazorpayRefundDispatchBarrierService barrier;
    @Autowired PlatformTransactionManager transactionManager;

    private record Fixture(Long reconciliationId, Long attemptId) {}

    private Fixture fixture(boolean claim) {
        assertEquals(
            "kcmart_refund_claim_timestamp_test",
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
    void successfulClaimMustPopulateTimestamp() {
        Fixture f = fixture(false);

        assertNull(
            jdbc.queryForObject(
                """
                SELECT claimed_at
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                java.time.OffsetDateTime.class,
                f.attemptId()
            )
        );

        claiming.claimPendingRefund(
            f.attemptId(),
            "timestamp-worker"
        );

        java.time.OffsetDateTime claimedAt =
            jdbc.queryForObject(
                """
                SELECT claimed_at
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                java.time.OffsetDateTime.class,
                f.attemptId()
            );

        assertNotNull(claimedAt);

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

        System.out.println("SUCCESSFUL CLAIM TIMESTAMP VERIFIED");
    }

    @Test
    void duplicateClaimMustNotChangeTimestamp() {
        Fixture f = fixture(true);

        java.time.OffsetDateTime original =
            jdbc.queryForObject(
                """
                SELECT claimed_at
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                java.time.OffsetDateTime.class,
                f.attemptId()
            );

        assertNotNull(original);

        assertThrows(
            IllegalStateException.class,
            () -> claiming.claimPendingRefund(
                f.attemptId(),
                "duplicate-worker"
            )
        );

        java.time.OffsetDateTime after =
            jdbc.queryForObject(
                """
                SELECT claimed_at
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                java.time.OffsetDateTime.class,
                f.attemptId()
            );

        assertEquals(original, after);

        System.out.println("DUPLICATE CLAIM TIMESTAMP PROTECTED");
    }

    @Test
    void invalidWorkerMustNotPopulateTimestamp() {
        Fixture f = fixture(false);

        assertThrows(
            IllegalArgumentException.class,
            () -> claiming.claimPendingRefund(
                f.attemptId(),
                " "
            )
        );

        assertNull(
            jdbc.queryForObject(
                """
                SELECT claimed_at
                FROM razorpay_refund_attempts
                WHERE id = ?
                """,
                java.time.OffsetDateTime.class,
                f.attemptId()
            )
        );

        assertEquals(
            "PENDING",
            jdbc.queryForObject(
                "SELECT status FROM razorpay_refund_attempts WHERE id = ?",
                String.class,
                f.attemptId()
            )
        );

        System.out.println("INVALID CLAIM TIMESTAMP PROTECTED");
    }
}