package in.kcmart.backend.order.dto;

public record CheckoutPaymentMethodValidationResponse(
    boolean valid,
    String fulfillmentType,
    String paymentMethod
) {}