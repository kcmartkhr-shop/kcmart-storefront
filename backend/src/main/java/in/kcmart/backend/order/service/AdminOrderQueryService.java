package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.admin.AdminOrderHistoryResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderItemResponse;
import in.kcmart.backend.order.dto.admin.AdminOrderResponse;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderItem;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderItemRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminOrderQueryService {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository history;

    public AdminOrderQueryService(
            OrderRepository orders,
            OrderItemRepository orderItems,
            OrderStatusHistoryRepository history) {

        this.orders = orders;
        this.orderItems = orderItems;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderResponse> listAll() {
        return orders.findAll()
            .stream()
            .sorted((a, b) -> b.getId().compareTo(a.getId()))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse getById(Long orderId) {
        Order order = orders.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        return toResponse(order);
    }

    private AdminOrderResponse toResponse(Order order) {

        List<AdminOrderItemResponse> itemResponses =
            orderItems.findByOrderIdOrderByIdAsc(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();

        List<AdminOrderHistoryResponse> historyResponses =
            history.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId())
                .stream()
                .map(this::toHistoryResponse)
                .toList();

        return new AdminOrderResponse(
            order.getId(),
            order.getOrderNumber(),

            order.getCustomerId(),
            order.getCustomerName(),
            order.getCustomerEmail(),
            order.getCustomerPhone(),

            order.getFulfillmentType(),

            order.getAddressLine1(),
            order.getAddressLine2(),
            order.getLandmark(),
            order.getCity(),
            order.getState(),
            order.getPostalCode(),
            order.getCountry(),

            order.getSubtotal(),
            order.getDeliveryCharge(),
            order.getTotalAmount(),

            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getOrderStatus(),

            order.getCreatedAt(),
            order.getUpdatedAt(),

            itemResponses,
            historyResponses
        );
    }

    private AdminOrderItemResponse toItemResponse(OrderItem item) {
        return new AdminOrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getSku(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal()
        );
    }

    private AdminOrderHistoryResponse toHistoryResponse(
            OrderStatusHistory event) {

        return new AdminOrderHistoryResponse(
            event.getId(),
            event.getPreviousStatus(),
            event.getNewStatus(),
            event.getActorType(),
            event.getActorIdentifier(),
            event.getNote(),
            event.getCreatedAt()
        );
    }
}