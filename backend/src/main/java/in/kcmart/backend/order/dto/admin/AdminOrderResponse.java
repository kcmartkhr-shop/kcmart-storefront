package in.kcmart.backend.order.dto.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminOrderResponse(
    Long id,
    String orderNumber,

    Long customerId,
    String customerName,
    String customerEmail,
    String customerPhone,

    String fulfillmentType,

    String addressLine1,
    String addressLine2,
    String landmark,
    String city,
    String state,
    String postalCode,
    String country,

    BigDecimal subtotal,
    BigDecimal deliveryCharge,
    BigDecimal totalAmount,

    String paymentMethod,
    String paymentStatus,
    String orderStatus,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,

    List<AdminOrderItemResponse> items,
    List<AdminOrderHistoryResponse> history
) {
}