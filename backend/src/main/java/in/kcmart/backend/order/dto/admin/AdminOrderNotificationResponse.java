package in.kcmart.backend.order.dto.admin;

import java.time.OffsetDateTime;

public record AdminOrderNotificationResponse(
    Long id,
    String notificationType,
    String channel,
    String recipient,
    String subject,
    String status,
    Integer attemptCount,
    String lastError,
    OffsetDateTime createdAt,
    OffsetDateTime sentAt
) {
}