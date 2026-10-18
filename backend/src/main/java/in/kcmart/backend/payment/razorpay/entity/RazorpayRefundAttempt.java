package in.kcmart.backend.payment.razorpay.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "razorpay_refund_attempts")
public class RazorpayRefundAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_id", nullable = false)
    private Long reconciliationId;

    @Column(name = "razorpay_payment_id", nullable = false)
    private String razorpayPaymentId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private String currency = "INR";

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "approved_by", nullable = false)
    private String approvedBy;

    @Column(name = "approved_at", insertable = false, updatable = false)
    private OffsetDateTime approvedAt;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    public OffsetDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(OffsetDateTime value) { claimedAt = value; }

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "gateway_request_sent_at", insertable = false, updatable = false)
    private OffsetDateTime gatewayRequestSentAt;

    @Column(name = "gateway_response_received_at")
    private OffsetDateTime gatewayResponseReceivedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public String getRazorpayRefundId() { return razorpayRefundId; }
    public void setRazorpayRefundId(String value) { razorpayRefundId = value; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime value) { completedAt = value; }

    public OffsetDateTime getGatewayRequestSentAt() { return gatewayRequestSentAt; }

    public OffsetDateTime getGatewayResponseReceivedAt() { return gatewayResponseReceivedAt; }
    public void setGatewayResponseReceivedAt(OffsetDateTime value) {
        gatewayResponseReceivedAt = value;
    }

    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public Long getId() { return id; }

    public Long getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(Long value) { reconciliationId = value; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String value) { razorpayPaymentId = value; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }

    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long value) { amountPaise = value; }

    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String value) { approvedBy = value; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
}