package in.kcmart.backend.payment.razorpay.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "razorpay_payment_reconciliations")
public class RazorpayPaymentReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "payment_attempt_id", nullable = false)
    private Long paymentAttemptId;

    @Column(name = "razorpay_order_id", nullable = false, length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", nullable = false, unique = true, length = 100)
    private String razorpayPaymentId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false, length = 30)
    private String status = "CAPTURED_REVIEW";

    @Column(name = "razorpay_refund_id", unique = true, length = 100)
    private String razorpayRefundId;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "verified_at", insertable = false, updatable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "refund_requested_at")
    private OffsetDateTime refundRequestedAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }

    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public void setPaymentAttemptId(Long value) { paymentAttemptId = value; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String value) { razorpayOrderId = value; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String value) { razorpayPaymentId = value; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long value) { amountPaise = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency = value; }

    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getRazorpayRefundId() { return razorpayRefundId; }
    public void setRazorpayRefundId(String value) { razorpayRefundId = value; }

    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }

    public OffsetDateTime getRefundRequestedAt() { return refundRequestedAt; }
    public void setRefundRequestedAt(OffsetDateTime value) {
        refundRequestedAt = value;
    }

    public OffsetDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(OffsetDateTime value) { refundedAt = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { updatedAt = value; }
}