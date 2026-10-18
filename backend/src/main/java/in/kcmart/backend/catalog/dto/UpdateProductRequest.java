package in.kcmart.backend.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateProductRequest(
    @NotNull Long categoryId,
    @NotBlank String name,
    @NotBlank String slug,
    String description,
    String imageUrl,
    String brand,
    @NotBlank String sku,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @DecimalMin("0.00") BigDecimal mrp
) {}