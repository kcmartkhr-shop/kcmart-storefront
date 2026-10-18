package in.kcmart.backend.order.dto.customer;

import java.time.OffsetDateTime;

public record CustomerOrderInvoiceResponse(
    boolean available,
    String invoiceNumber,
    String originalFilename,
    OffsetDateTime uploadedAt
) {
}