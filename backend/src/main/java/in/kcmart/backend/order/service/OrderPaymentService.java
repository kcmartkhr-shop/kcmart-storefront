package in.kcmart.backend.order.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderPaymentHistory;
import in.kcmart.backend.order.repository.OrderPaymentHistoryRepository;
import in.kcmart.backend.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderPaymentService {

    private final OrderRepository orders;
    private final OrderPaymentHistoryRepository paymentHistory;

    public OrderPaymentService(
            OrderRepository orders,
            OrderPaymentHistoryRepository paymentHistory) {

        this.orders = orders;
        this.paymentHistory = paymentHistory;
    }

    @Transactional
    public Order markPayAtStoreReceived(
            Long orderId,
            String actorIdentifier,
            String note) {

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        if (!"STORE_PICKUP".equals(order.getFulfillmentType())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Manual payment receipt is only available for store pickup orders."
            );
        }

        if (!"PAY_AT_STORE".equals(order.getPaymentMethod())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Manual payment receipt is only available for PAY_AT_STORE orders."
            );
        }

        if (!"READY_FOR_PICKUP".equals(order.getOrderStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order must be READY_FOR_PICKUP before payment can be marked received."
            );
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment is not pending."
            );
        }

        String safeActor = normalizeActor(actorIdentifier);
        String safeNote = normalizeNote(note);

        String previousStatus = order.getPaymentStatus();

        order.setPaymentStatus("PAID");
        Order savedOrder = orders.save(order);

        OrderPaymentHistory history = new OrderPaymentHistory();
        history.setOrderId(savedOrder.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus("PAID");
        history.setPaymentMethod(savedOrder.getPaymentMethod());
        history.setActorType("ADMIN");
        history.setActorIdentifier(safeActor);
        history.setNote(safeNote);

        paymentHistory.save(history);

        return savedOrder;
    }

    @Transactional
    public Order markOnlinePaymentReceived(
            Long orderId,
            String razorpayPaymentId) {

        Order order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Order not found."
            ));

        if (!"ONLINE".equals(order.getPaymentMethod())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Order does not use online payment."
            );
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            return order;
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment cannot be completed from the current state."
            );
        }

        if ("CANCELLED".equals(order.getOrderStatus())
                || "COMPLETED".equals(order.getOrderStatus())) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment cannot be completed for this order."
            );
        }

        String safePaymentId =
                normalizeGatewayPaymentId(
                    razorpayPaymentId
                );

        String previousStatus =
                order.getPaymentStatus();

        order.setPaymentStatus("PAID");

        Order savedOrder =
                orders.save(order);

        OrderPaymentHistory history =
                new OrderPaymentHistory();

        history.setOrderId(savedOrder.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus("PAID");
        history.setPaymentMethod("ONLINE");
        history.setActorType("SYSTEM");
        history.setActorIdentifier("RAZORPAY");
        history.setNote(
            "Razorpay payment captured: "
                + safePaymentId
        );

        paymentHistory.save(history);

        return savedOrder;
    }

    private String normalizeGatewayPaymentId(
            String paymentId) {

        if (paymentId == null
                || paymentId.isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Gateway payment ID is required."
            );
        }

        String value = paymentId.trim();

        if (value.length() > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Gateway payment ID is invalid."
            );
        }

        return value;
    }
    private String normalizeActor(String actorIdentifier) {
        if (actorIdentifier == null || actorIdentifier.isBlank()) {
            return "admin";
        }

        String value = actorIdentifier.trim();

        if (value.length() > 320) {
            return value.substring(0, 320);
        }

        return value;
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }

        String value = note.trim();

        if (value.length() > 1000) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Payment note must not exceed 1000 characters."
            );
        }

        return value;
    }
}