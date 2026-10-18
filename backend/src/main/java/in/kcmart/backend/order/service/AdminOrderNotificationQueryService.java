package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.admin.AdminOrderNotificationResponse;
import in.kcmart.backend.order.entity.OrderNotification;
import in.kcmart.backend.order.repository.OrderNotificationRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminOrderNotificationQueryService {

    private final OrderRepository orders;
    private final OrderNotificationRepository notifications;

    public AdminOrderNotificationQueryService(
            OrderRepository orders,
            OrderNotificationRepository notifications) {

        this.orders = orders;
        this.notifications = notifications;
    }

    public List<AdminOrderNotificationResponse> getByOrderId(
            Long orderId) {

        if (!orders.existsById(orderId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            );
        }

        return notifications
            .findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private AdminOrderNotificationResponse toResponse(
            OrderNotification notification) {

        return new AdminOrderNotificationResponse(
            notification.getId(),
            notification.getNotificationType(),
            notification.getChannel(),
            notification.getRecipient(),
            notification.getSubject(),
            notification.getStatus(),
            notification.getAttemptCount(),
            notification.getLastError(),
            notification.getCreatedAt(),
            notification.getSentAt()
        );
    }
}