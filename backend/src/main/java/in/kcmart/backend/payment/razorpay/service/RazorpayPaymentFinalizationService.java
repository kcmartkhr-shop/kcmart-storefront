package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.service.OrderPaymentService;
import in.kcmart.backend.order.reservation.service.OrderStockReservationService;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class RazorpayPaymentFinalizationService {

    private final OrderRepository orders;
    private final RazorpayPaymentAttemptRepository attempts;
    private final OrderPaymentService orderPaymentService;
    private final OrderStockReservationService stockReservations;

    public RazorpayPaymentFinalizationService(
            OrderRepository orders,
            RazorpayPaymentAttemptRepository attempts,
            OrderPaymentService orderPaymentService,
            OrderStockReservationService stockReservations) {

        this.orders = orders;
        this.attempts = attempts;
        this.orderPaymentService = orderPaymentService;
        this.stockReservations = stockReservations;
    }

    @Transactional
    public FinalizationResult finalizeCapturedPayment(
            Long orderId,
            String razorpayOrderId,
            String razorpayPaymentId,
            long amountPaise,
            String currency,
            boolean signatureVerified) {

        if (orderId == null || orderId <= 0) {
            throw paymentConflict("Invalid order.");
        }

        String safeRazorpayOrderId =
                requiredId(
                        razorpayOrderId,
                        "Razorpay order ID"
                );

        String safeRazorpayPaymentId =
                requiredId(
                        razorpayPaymentId,
                        "Razorpay payment ID"
                );

        if (amountPaise <= 0) {
            throw paymentConflict(
                    "Payment amount is invalid."
            );
        }

        if (!"INR".equals(currency)) {
            throw paymentConflict(
                    "Payment currency is invalid."
            );
        }

        /*
         * LOCK ORDER:
         *
         * 1. KCMART order
         * 2. Razorpay payment attempt
         *
         * Keep this order consistent for every payment
         * finalization path.
         */
        Order order =
                orders.findByIdForUpdate(orderId)
                        .orElseThrow(() ->
                                paymentConflict(
                                        "Order not found."
                                )
                        );

        RazorpayPaymentAttempt attempt =
                attempts.findForFinalization(
                        orderId,
                        safeRazorpayOrderId
                ).orElseThrow(() ->
                        paymentConflict(
                                "Payment attempt not found."
                        )
                );

        if (!"ONLINE".equals(
                order.getPaymentMethod())) {

            throw paymentConflict(
                    "Order does not use online payment."
            );
        }

        if ("CANCELLED".equals(
                order.getOrderStatus())) {

            throw paymentConflict(
                    "Payment cannot be finalized for a cancelled order."
            );
        }

        if (!Long.valueOf(amountPaise)
                .equals(attempt.getAmountPaise())) {

            throw paymentConflict(
                    "Payment amount does not match the payment attempt."
            );
        }

        if (!currency.equals(
                attempt.getCurrency())) {

            throw paymentConflict(
                    "Payment currency does not match the payment attempt."
            );
        }

        /*
         * Fully finalized retry.
         *
         * This is the important idempotent path for:
         * - repeated browser callback
         * - repeated webhook
         * - callback followed by webhook
         */
        if ("CAPTURED".equals(
                attempt.getStatus())) {

            if (attempt.getRazorpayPaymentId() == null
                    || !safeRazorpayPaymentId.equals(
                        attempt.getRazorpayPaymentId())) {

                throw paymentConflict(
                        "Captured payment attempt belongs to a different payment."
                );
            }

            if (!"PAID".equals(
                    order.getPaymentStatus())) {

                throw paymentConflict(
                        "Payment state is inconsistent."
                );
            }

            /*
             * A webhook may finalize a payment without a
             * browser checkout signature. Never downgrade
             * true back to false.
             */
            if (signatureVerified
                    && !attempt.isSignatureVerified()) {

                attempt.setSignatureVerified(true);
                attempts.save(attempt);
            }

            return new FinalizationResult(
                    order.getId(),
                    attempt.getRazorpayOrderId(),
                    attempt.getRazorpayPaymentId(),
                    true
            );
        }

        /*
         * A legitimately paid order may later become COMPLETED,
         * so the CAPTURED idempotent branch above remains valid.
         *
         * But a brand-new CREATED payment attempt must never
         * transition an already-completed order from PENDING
         * payment to PAID.
         */
        if ("COMPLETED".equals(
                order.getOrderStatus())) {

            throw paymentConflict(
                    "Payment cannot be finalized for a completed order."
            );
        }

        if (!"CREATED".equals(
                attempt.getStatus())) {

            throw paymentConflict(
                    "Payment attempt cannot be finalized from its current state."
            );
        }

        if (!"PENDING".equals(
                order.getPaymentStatus())) {

            throw paymentConflict(
                    "Order payment cannot be finalized from its current state."
            );
        }

        /*
         * DELIVERY + ONLINE orders use a temporary stock reservation.
         *
         * Consume the reservation before the payment attempt is
         * marked CAPTURED and before the order becomes PAID.
         *
         * This permanently deducts online stock only after the
         * payment has been verified as captured by Razorpay.
         *
         * STORE_PICKUP orders do not have this reservation because
         * their existing store-stock reservation behavior remains
         * unchanged.
         */
        if ("DELIVERY".equals(order.getFulfillmentType())) {
            stockReservations.consumeForPaidOrder(order.getId());
        }

        attempt.setRazorpayPaymentId(
                safeRazorpayPaymentId
        );

        if (signatureVerified) {
            attempt.setSignatureVerified(true);
        }

        attempt.setStatus("CAPTURED");
        attempt.setPaidAt(OffsetDateTime.now());

        attempts.save(attempt);

        /*
         * OrderPaymentService locks the same order again
         * inside this transaction. Because we already hold
         * that row lock, it does not reverse our lock order.
         *
         * It also owns creation of the PENDING -> PAID
         * payment-history record.
         */
        orderPaymentService.markOnlinePaymentReceived(
                order.getId(),
                safeRazorpayPaymentId
        );

        return new FinalizationResult(
                order.getId(),
                attempt.getRazorpayOrderId(),
                safeRazorpayPaymentId,
                false
        );
    }

    private String requiredId(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw paymentConflict(
                    fieldName + " is required."
            );
        }

        String normalized = value.trim();

        if (normalized.length() > 100) {
            throw paymentConflict(
                    fieldName + " is invalid."
            );
        }

        return normalized;
    }

    private ResponseStatusException paymentConflict(
            String message) {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    public record FinalizationResult(
            Long orderId,
            String razorpayOrderId,
            String razorpayPaymentId,
            boolean alreadyFinalized) {
    }
}