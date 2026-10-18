package in.kcmart.backend.order.dto.admin;

public record AdminOrderShipmentUpsertRequest(
    String carrierName,
    String trackingNumber,
    String trackingUrl,
    String adminNote
) {
}