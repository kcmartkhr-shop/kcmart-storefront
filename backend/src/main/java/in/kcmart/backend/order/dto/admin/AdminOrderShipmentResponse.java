package in.kcmart.backend.order.dto.admin;

import java.time.OffsetDateTime;

public record AdminOrderShipmentResponse(
    Long id,
    Long orderId,
    String carrierName,
    String trackingNumber,
    String trackingUrl,
    String shipmentStatus,
    OffsetDateTime shippedAt,
    OffsetDateTime outForDeliveryAt,
    OffsetDateTime deliveredAt,
    String adminNote,
    String createdBy,
    String updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}