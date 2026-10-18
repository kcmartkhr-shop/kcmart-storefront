package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "order_stock_restorations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_order_stock_restorations_order",
            columnNames = "order_id"
        )
    }
)
public class OrderStockRestoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "order_id",
        nullable = false
    )
    private Long orderId;

    @Column(
        name = "fulfillment_type",
        nullable = false,
        length = 30
    )
    private String fulfillmentType;

    @Column(
        name = "restored_by",
        nullable = false,
        length = 255
    )
    private String restoredBy;

    @Column(
        name = "restored_at",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private OffsetDateTime restoredAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getFulfillmentType() {
        return fulfillmentType;
    }

    public String getRestoredBy() {
        return restoredBy;
    }

    public OffsetDateTime getRestoredAt() {
        return restoredAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setFulfillmentType(
            String fulfillmentType) {
        this.fulfillmentType =
            fulfillmentType;
    }

    public void setRestoredBy(
            String restoredBy) {
        this.restoredBy = restoredBy;
    }
}