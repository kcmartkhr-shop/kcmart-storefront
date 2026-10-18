package in.kcmart.backend.order.dto.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CustomerOrderSummaryResponse(
    Long id,
    String orderNumber,
    String fulfillmentType,
    BigDecimal totalAmount,
    String paymentMethod,
    String paymentStatus,
    String orderStatus,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}