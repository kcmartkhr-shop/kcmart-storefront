package in.kcmart.backend.order.dto.admin;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String sku,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal
) {
}