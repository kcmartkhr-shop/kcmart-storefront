package in.kcmart.backend.catalog.dto;

import in.kcmart.backend.catalog.entity.Product;
import in.kcmart.backend.catalog.entity.ProductInventory;
import java.math.BigDecimal;

public record AdminProductResponse(
    Long id,
    Long categoryId,
    String name,
    String slug,
    String description,
    String imageUrl,
    String brand,
    String sku,
    BigDecimal price,
    BigDecimal mrp,
    boolean active,
    int onlineStock,
    int storeStock
) {
    public static AdminProductResponse from(
            Product product,
            ProductInventory stock) {
        return new AdminProductResponse(
            product.getId(),
            product.getCategoryId(),
            product.getName(),
            product.getSlug(),
            product.getDescription(),
            product.getImageUrl(),
            product.getBrand(),
            product.getSku(),
            product.getPrice(),
            product.getMrp(),
            product.isActive(),
            stock == null ? 0 : stock.getOnlineStock(),
            stock == null ? 0 : stock.getStoreStock()
        );
    }
}