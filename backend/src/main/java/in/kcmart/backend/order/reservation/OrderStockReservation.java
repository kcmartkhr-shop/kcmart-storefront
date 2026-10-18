package in.kcmart.backend.order.reservation;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "order_stock_reservations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_order_stock_reservations_order_product",
            columnNames = {"order_id", "product_id"}
        )
    }
)
public class OrderStockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
}
