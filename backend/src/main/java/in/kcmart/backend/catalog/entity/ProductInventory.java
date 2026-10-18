package in.kcmart.backend.catalog.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_inventory")
public class ProductInventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "online_stock", nullable = false)
    private int onlineStock;

    @Column(name = "store_stock", nullable = false)
    private int storeStock;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getProductId() { return productId; }
    public int getOnlineStock() { return onlineStock; }
    public int getStoreStock() { return storeStock; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setProductId(Long productId) { this.productId = productId; }
    public void setOnlineStock(int onlineStock) { this.onlineStock = onlineStock; }
    public void setStoreStock(int storeStock) { this.storeStock = storeStock; }
}