package in.kcmart.backend.order.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "order_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_order_items_order_product",
            columnNames = {
                "order_id",
                "product_id"
            }
        )
    }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(
        name = "product_name",
        nullable = false,
        length = 255
    )
    private String productName;

    @Column(
        nullable = false,
        length = 100
    )
    private String sku;

    @Column(
        name = "unit_price",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(
        name = "line_total",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal lineTotal;

    @Column(
        name = "created_at",
        insertable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}