package in.kcmart.backend.order.dto;

import java.math.BigDecimal;

public record OrderCreateResponse(

    Long orderId,
    String orderNumber,
    String fulfillmentType,
    String paymentMethod,
    String paymentStatus,
    String orderStatus,
    BigDecimal subtotal,
    BigDecimal deliveryCharge,
    BigDecimal totalAmount,
    boolean idempotentReplay

) {
}