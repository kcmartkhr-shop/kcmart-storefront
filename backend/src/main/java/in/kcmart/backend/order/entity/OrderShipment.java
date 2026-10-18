package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "order_shipments")
public class OrderShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "tracking_number", length = 150)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 1000)
    private String trackingUrl;

    @Column(name = "shipment_status", nullable = false, length = 30)
    private String shipmentStatus;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "out_for_delivery_at")
    private OffsetDateTime outForDeliveryAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(
        name = "created_at",
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public OffsetDateTime getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public void setShippedAt(OffsetDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public void setOutForDeliveryAt(OffsetDateTime outForDeliveryAt) {
        this.outForDeliveryAt = outForDeliveryAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}