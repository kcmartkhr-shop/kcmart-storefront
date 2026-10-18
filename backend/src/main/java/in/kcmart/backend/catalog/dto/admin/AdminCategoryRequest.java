package in.kcmart.backend.catalog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryRequest(

    @NotBlank(message = "Category name is required.")
    @Size(max = 120, message = "Category name must not exceed 120 characters.")
    String name,

    @NotBlank(message = "Category slug is required.")
    @Size(max = 150, message = "Category slug must not exceed 150 characters.")
    String slug,

    Long parentId,

    boolean active
) {
}