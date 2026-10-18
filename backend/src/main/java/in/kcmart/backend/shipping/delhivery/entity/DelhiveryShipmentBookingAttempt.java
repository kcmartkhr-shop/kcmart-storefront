package in.kcmart.backend.shipping.delhivery.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "delhivery_shipment_booking_attempts")
public class DelhiveryShipmentBookingAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "attempt_status", nullable = false, length = 20)
    private String attemptStatus = "PENDING";

    @Column(name = "waybill", length = 150)
    private String waybill;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected DelhiveryShipmentBookingAttempt() {
    }

    public DelhiveryShipmentBookingAttempt(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Valid order ID required.");
        }
        this.orderId = orderId;
        this.attemptStatus = "PENDING";
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public String getWaybill() {
        return waybill;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void requirePending() {
        if (!"PROCESSING".equals(attemptStatus)) {
            throw new IllegalStateException(
                "Booking attempt is not PROCESSING. Reconciliation required."
            );
        }
    }

    public void markSucceeded(String waybill) {
        if (!"PROCESSING".equals(attemptStatus)) {
            throw new IllegalStateException(
                "Booking success transition is not allowed."
            );
        }
        if (waybill == null || !waybill.matches("[A-Za-z0-9-]{5,50}")) {
            throw new IllegalArgumentException("Invalid Delhivery waybill.");
        }
        this.waybill = waybill;
        this.attemptStatus = "SUCCEEDED";
        this.failureReason = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markUnknown(String reason) {
        requirePending();
        this.attemptStatus = "UNKNOWN";
        this.failureReason = limitReason(reason);
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        requirePending();
        this.attemptStatus = "FAILED";
        this.failureReason = limitReason(reason);
        this.updatedAt = OffsetDateTime.now();
    }

    private String limitReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unspecified";
        }
        return reason.length() > 500
            ? reason.substring(0, 500)
            : reason;
    }
}