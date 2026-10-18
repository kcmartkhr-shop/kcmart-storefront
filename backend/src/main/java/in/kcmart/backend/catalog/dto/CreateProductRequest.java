package in.kcmart.backend.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateProductRequest(
    @NotNull Long categoryId,
    @NotBlank String name,
    @NotBlank String slug,
    String description,
    String brand,
    @NotBlank String sku,
    @NotNull @DecimalMin("0.00") BigDecimal price,
    @DecimalMin("0.00") BigDecimal mrp,
    String imageUrl,
    @Min(0) int onlineStock,
    @Min(0) int storeStock
) {}