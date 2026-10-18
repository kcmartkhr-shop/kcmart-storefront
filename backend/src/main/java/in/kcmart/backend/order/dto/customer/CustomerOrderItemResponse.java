package in.kcmart.backend.order.dto.customer;

import java.math.BigDecimal;

public record CustomerOrderItemResponse(
    Long productId,
    String productName,
    String sku,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal
) {
}