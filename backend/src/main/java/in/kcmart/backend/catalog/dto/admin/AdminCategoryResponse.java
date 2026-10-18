package in.kcmart.backend.catalog.dto.admin;

import in.kcmart.backend.catalog.entity.Category;

import java.time.OffsetDateTime;

public record AdminCategoryResponse(
    Long id,
    String name,
    String slug,
    Long parentId,
    boolean active,
    long productCount,
    OffsetDateTime createdAt
) {
    public static AdminCategoryResponse from(
        Category category,
        long productCount
    ) {
        return new AdminCategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getParentId(),
            category.isActive(),
            productCount,
            category.getCreatedAt()
        );
    }
}