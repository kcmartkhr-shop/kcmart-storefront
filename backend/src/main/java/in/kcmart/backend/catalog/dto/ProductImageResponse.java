package in.kcmart.backend.catalog.dto;

import in.kcmart.backend.catalog.entity.ProductImage;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        boolean cover,
        int displayOrder
) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.isCover(),
                image.getDisplayOrder()
        );
    }
}