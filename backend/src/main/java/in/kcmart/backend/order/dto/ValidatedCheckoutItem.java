package in.kcmart.backend.order.dto;

import java.math.BigDecimal;

public record ValidatedCheckoutItem(

    Long productId,
    String slug,
    String name,
    String sku,
    String imageUrl,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal lineTotal,
    int availableOnlineStock

) {
}