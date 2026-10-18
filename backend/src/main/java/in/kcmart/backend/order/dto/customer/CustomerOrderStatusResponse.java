package in.kcmart.backend.order.dto.customer;

import java.time.OffsetDateTime;

public record CustomerOrderStatusResponse(
    String status,
    OffsetDateTime createdAt
) {
}