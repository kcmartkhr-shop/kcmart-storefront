package in.kcmart.backend.order.dto.customer;

import java.time.OffsetDateTime;

public record CustomerOrderShipmentResponse(
    String carrierName,
    String trackingNumber,
    String trackingUrl,
    String shipmentStatus,
    OffsetDateTime shippedAt,
    OffsetDateTime outForDeliveryAt,
    OffsetDateTime deliveredAt
) {
}