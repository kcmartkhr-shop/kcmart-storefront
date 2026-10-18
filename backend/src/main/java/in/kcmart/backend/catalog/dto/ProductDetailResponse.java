package in.kcmart.backend.catalog.dto;

import in.kcmart.backend.catalog.entity.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ProductDetailResponse(
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ProductImageResponse> images
) {
    public static ProductDetailResponse from(
            Product product,
            List<ProductImageResponse> images
    ) {
        return new ProductDetailResponse(
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
                product.getCreatedAt(),
                product.getUpdatedAt(),
                images
        );
    }
}