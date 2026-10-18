package in.kcmart.backend.order.reservation.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.OrderStockReservation;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DeliveryReservationExpiryProcessor {

    private static final long FROZEN_ORDER_ID_MAX = 10L;

    private final OrderRepository orders;
    private final OrderStockReservationRepository reservations;
    private final OrderStatusHistoryRepository statusHistory;

    public DeliveryReservationExpiryProcessor(
            OrderRepository orders,
            OrderStockReservationRepository reservations,
            OrderStatusHistoryRepository statusHistory) {
        this.orders = orders;
        this.reservations = reservations;
        this.statusHistory = statusHistory;
    }

    @Transactional
    public void expireOneOrder(Long orderId) {
        if (orderId == null || orderId <= FROZEN_ORDER_ID_MAX) {
            return;
        }

        Order order = orders.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }

        if (!"DELIVERY".equals(order.getFulfillmentType())
                || !"ONLINE".equals(order.getPaymentMethod())
                || !"PENDING".equals(order.getPaymentStatus())
                || !"PENDING".equals(order.getOrderStatus())) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        List<OrderStockReservation> orderReservations =
                reservations.findByOrderIdForUpdate(orderId);

        boolean hasExpiredActiveReservation = orderReservations.stream()
                .anyMatch(reservation ->
                        "ACTIVE".equals(reservation.getStatus())
                                && !reservation.getExpiresAt().isAfter(now));

        if (!hasExpiredActiveReservation) {
            return;
        }

        for (OrderStockReservation reservation : orderReservations) {
            if ("ACTIVE".equals(reservation.getStatus())) {
                reservation.setStatus("RELEASED");
                reservation.setReleasedAt(now);
                reservations.save(reservation);
            }
        }

        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("FAILED");
        orders.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(order.getId());
        history.setPreviousStatus("PENDING");
        history.setNewStatus("CANCELLED");
        history.setActorType("SYSTEM");
        history.setActorIdentifier("DELIVERY_RESERVATION_EXPIRY");
        history.setNote(
                "Automatically cancelled because online payment was not completed within the 5-minute stock reservation window."
        );
        statusHistory.save(history);
    }
}