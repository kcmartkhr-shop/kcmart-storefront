package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.OrderStockReservation;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayPaymentResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentReconciliationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayPaymentReconciliationService {

    private static final long FROZEN_ORDER_ID_MAX = 10L;

    private final OrderRepository orders;
    private final RazorpayPaymentAttemptRepository attempts;
    private final RazorpayPaymentReconciliationRepository reconciliations;
    private final OrderStockReservationRepository reservations;
    private final OrderStatusHistoryRepository statusHistory;

    public RazorpayPaymentReconciliationService(
            OrderRepository orders,
            RazorpayPaymentAttemptRepository attempts,
            RazorpayPaymentReconciliationRepository reconciliations,
            OrderStockReservationRepository reservations,
            OrderStatusHistoryRepository statusHistory) {

        this.orders = orders;
        this.attempts = attempts;
        this.reconciliations = reconciliations;
        this.reservations = reservations;
        this.statusHistory = statusHistory;
    }

    @Transactional
    public RazorpayPaymentReconciliation recordCapturedForCancelledOrder(
            Long orderId,
            String razorpayOrderId,
            RazorpayPaymentResponse payment) {

        if (orderId == null || orderId <= FROZEN_ORDER_ID_MAX) {
            throw new IllegalStateException(
                    "Order is not eligible for reconciliation."
            );
        }

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || payment == null
                || payment.id() == null || payment.id().isBlank()) {

            throw new IllegalArgumentException(
                    "Valid Razorpay payment details are required."
            );
        }

        if (!"captured".equalsIgnoreCase(payment.status())
                || !Boolean.TRUE.equals(payment.captured())) {

            throw new IllegalStateException(
                    "Payment is not confirmed as captured."
            );
        }

        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new IllegalStateException("Order not found.")
                );

        RazorpayPaymentAttempt attempt =
                attempts.findForFinalization(orderId, razorpayOrderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment attempt not found."
                                )
                        );

        if (!"DELIVERY".equals(order.getFulfillmentType())
                || !"ONLINE".equals(order.getPaymentMethod())) {

            throw new IllegalStateException(
                    "Only online delivery orders can be reconciled."
            );
        }

        if (!payment.id().equals(payment.id().trim())
                || !razorpayOrderId.equals(payment.order_id())
                || !razorpayOrderId.equals(attempt.getRazorpayOrderId())
                || payment.amount() == null
                || !payment.amount().equals(attempt.getAmountPaise())
                || !"INR".equals(payment.currency())
                || !payment.currency().equals(attempt.getCurrency())) {

            throw new IllegalStateException(
                    "Razorpay payment details do not match the attempt."
            );
        }

        if ("CAPTURED".equals(attempt.getStatus())
                || (attempt.getRazorpayPaymentId() != null
                    && !payment.id().equals(attempt.getRazorpayPaymentId()))) {

            throw new IllegalStateException(
                    "Payment attempt has a conflicting finalization state."
            );
        }

        if ("PENDING".equals(order.getOrderStatus())
                && "PENDING".equals(order.getPaymentStatus())) {

            cancelExpiredPendingOrder(order);
        }

        if (!"CANCELLED".equals(order.getOrderStatus())
                || !"FAILED".equals(order.getPaymentStatus())) {

            throw new IllegalStateException(
                    "Order is not eligible for late-payment reconciliation."
            );
        }

        var existing = reconciliations.findByRazorpayPaymentId(
                payment.id()
        );

        if (existing.isPresent()) {
            RazorpayPaymentReconciliation record = existing.get();

            if (!orderId.equals(record.getOrderId())
                    || !attempt.getId().equals(record.getPaymentAttemptId())
                    || !payment.amount().equals(record.getAmountPaise())
                    || !razorpayOrderId.equals(record.getRazorpayOrderId())) {

                throw new IllegalStateException(
                        "Conflicting reconciliation record exists."
                );
            }

            return record;
        }

        RazorpayPaymentReconciliation record =
                new RazorpayPaymentReconciliation();

        record.setOrderId(orderId);
        record.setPaymentAttemptId(attempt.getId());
        record.setRazorpayOrderId(razorpayOrderId);
        record.setRazorpayPaymentId(payment.id());
        record.setAmountPaise(payment.amount());
        record.setCurrency(payment.currency());
        record.setReason("CAPTURED_AFTER_ORDER_CANCELLATION");
        record.setStatus("CAPTURED_REVIEW");

        return reconciliations.saveAndFlush(record);
    }

    private void cancelExpiredPendingOrder(Order order) {

        OffsetDateTime now = OffsetDateTime.now();

        List<OrderStockReservation> items =
                reservations.findByOrderIdForUpdate(order.getId());

        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "No reservation exists for this order."
            );
        }

        boolean expired = false;

        for (OrderStockReservation item : items) {

            String status = item.getStatus();

            if (!"ACTIVE".equals(status)
                    && !"RELEASED".equals(status)) {
                throw new IllegalStateException(
                        "Reservation is in an unexpected state."
                );
            }

            if (!item.getExpiresAt().isAfter(now)) {
                expired = true;
            } else if ("RELEASED".equals(status)) {
                throw new IllegalStateException(
                        "Reservation was released before expiry."
                );
            }
        }

        if (!expired) {
            throw new IllegalStateException(
                    "Reservation has not expired."
            );
        }

        for (OrderStockReservation item : items) {

            if ("ACTIVE".equals(item.getStatus())) {
                item.setStatus("RELEASED");
                item.setReleasedAt(now);
                reservations.save(item);
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
        history.setActorIdentifier("LATE_RAZORPAY_CAPTURE");
        history.setNote(
                "Stock reservation expired. Captured payment recorded for reconciliation."
        );

        statusHistory.save(history);
    }
}