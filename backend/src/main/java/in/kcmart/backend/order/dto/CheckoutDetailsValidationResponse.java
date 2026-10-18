package in.kcmart.backend.order.dto;

public record CheckoutDetailsValidationResponse(
    boolean valid,
    String fulfillmentType,
    String customerName,
    String customerPhone,
    String addressLine1,
    String addressLine2,
    String landmark,
    String city,
    String state,
    String postalCode,
    String country
) {
}