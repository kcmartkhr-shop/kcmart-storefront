package in.kcmart.backend.order.dto.admin;

import java.time.OffsetDateTime;

public record AdminOrderInvoiceResponse(
    Long id,
    Long orderId,
    String invoiceNumber,
    String originalFilename,
    String contentType,
    Long fileSize,
    String uploadedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}