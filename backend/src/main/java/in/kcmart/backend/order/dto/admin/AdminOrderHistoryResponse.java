package in.kcmart.backend.order.dto.admin;

import java.time.OffsetDateTime;

public record AdminOrderHistoryResponse(
    Long id,
    String previousStatus,
    String newStatus,
    String actorType,
    String actorIdentifier,
    String note,
    OffsetDateTime createdAt
) {
}