package in.kcmart.backend.catalog.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "is_cover", nullable = false)
    private boolean cover;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getImageUrl() { return imageUrl; }
    public boolean isCover() { return cover; }
    public int getDisplayOrder() { return displayOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCover(boolean cover) { this.cover = cover; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}