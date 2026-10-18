package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.client.RazorpayClient;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayPaymentResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;
import org.springframework.stereotype.Service;

@Service
public class RazorpayWebhookProcessingService {

    private final RazorpayPaymentAttemptRepository attempts;
    private final RazorpayClient razorpayClient;
    private final RazorpayPaymentFinalizationService paymentFinalizer;
    private final RazorpayWebhookEventService eventService;
    private final RazorpayPaymentReconciliationService reconciliationService;

    public RazorpayWebhookProcessingService(
            RazorpayPaymentAttemptRepository attempts,
            RazorpayClient razorpayClient,
            RazorpayPaymentFinalizationService paymentFinalizer,
            RazorpayWebhookEventService eventService,
            RazorpayPaymentReconciliationService reconciliationService) {

        this.attempts = attempts;
        this.razorpayClient = razorpayClient;
        this.paymentFinalizer = paymentFinalizer;
        this.eventService = eventService;
        this.reconciliationService = reconciliationService;
    }

    public ProcessingResult process(
            Long eventDatabaseId,
            String eventType,
            String razorpayOrderId,
            String razorpayPaymentId) {

        if (eventDatabaseId == null || eventDatabaseId <= 0) {
            throw new IllegalArgumentException(
                    "Webhook database event ID is required."
            );
        }

        String safeEventType =
                normalize(eventType);

        /*
         * Only payment.captured is allowed to enter the
         * payment-finalization path.
         */
        if (!"payment.captured".equals(safeEventType)) {

            eventService.markIgnored(
                    eventDatabaseId,
                    "Unsupported Razorpay webhook event type: "
                            + safeEventType
            );

            return new ProcessingResult(
                    "IGNORED",
                    false,
                    false
            );
        }

        String safeRazorpayOrderId =
                requiredIdentifier(
                        razorpayOrderId,
                        "Razorpay order ID"
                );

        String safeRazorpayPaymentId =
                requiredIdentifier(
                        razorpayPaymentId,
                        "Razorpay payment ID"
                );

        try {

            /*
             * NON-LOCKING local lookup.
             *
             * The webhook is never allowed to create a local
             * payment attempt. A KCMART checkout attempt must
             * already exist.
             */
            RazorpayPaymentAttempt attempt =
                    attempts.findByRazorpayOrderId(
                            safeRazorpayOrderId
                    ).orElseThrow(() ->
                            new IllegalStateException(
                                    "No local payment attempt exists "
                                            + "for the Razorpay order."
                            )
                    );

            /*
             * If the attempt is already CAPTURED, the payment
             * ID must still agree with this webhook.
             */
            if ("CAPTURED".equals(attempt.getStatus())
                    && attempt.getRazorpayPaymentId() != null
                    && !safeRazorpayPaymentId.equals(
                            attempt.getRazorpayPaymentId())) {

                throw new IllegalStateException(
                        "Webhook payment ID does not match "
                                + "the captured payment attempt."
                );
            }

            /*
             * IMPORTANT:
             *
             * This call is outside any transaction and no
             * pessimistic database lock is held.
             *
             * Never trust the webhook payload alone to mark
             * a KCMART order PAID.
             */
            RazorpayPaymentResponse gatewayPayment =
                    razorpayClient.fetchPayment(
                            safeRazorpayPaymentId
                    );

            validateAuthoritativePayment(
                    gatewayPayment,
                    attempt,
                    safeRazorpayPaymentId
            );

            /*
             * Short atomic transaction starts only here.
             *
             * PaymentFinalizationService locks:
             *
             * ORDER -> PAYMENT ATTEMPT
             *
             * and revalidates the local payment state.
             *
             * false means:
             * webhook HMAC authenticated this request, but a
             * browser checkout signature was not involved.
             */
            boolean paymentFinalized = false;
            boolean alreadyFinalized = false;

            try {
                RazorpayPaymentFinalizationService.FinalizationResult result =
                        paymentFinalizer.finalizeCapturedPayment(
                                attempt.getOrderId(),
                                attempt.getRazorpayOrderId(),
                                gatewayPayment.id(),
                                gatewayPayment.amount(),
                                gatewayPayment.currency(),
                                false
                        );

                paymentFinalized = true;
                alreadyFinalized = result.alreadyFinalized();

            } catch (org.springframework.web.server.ResponseStatusException ex) {

                if (ex.getStatusCode().value() != 409) {
                    throw ex;
                }

                /*
                 * Only a verified captured payment can reach here.
                 *
                 * Reconciliation re-locks ORDER -> ATTEMPT and
                 * requires a cancelled, unpaid delivery order.
                 *
                 * Other payment conflicts are NOT treated as
                 * successful reconciliation.
                 */
                reconciliationService.recordCapturedForCancelledOrder(
                        attempt.getOrderId(),
                        attempt.getRazorpayOrderId(),
                        gatewayPayment
                );
            }

            /*
             * PROCESSED means the captured payment was either
             * finalized or durably recorded for reconciliation.
             *
             * It does not mean a refund has been issued.
             */
            eventService.markProcessed(eventDatabaseId);

            return new ProcessingResult(
                    "PROCESSED",
                    paymentFinalized,
                    alreadyFinalized
            );

        } catch (Exception ex) {

            /*
             * Preserve the failure in the webhook ledger.
             *
             * The event-status transaction is independent
             * from payment finalization.
             */
            try {
                eventService.markFailed(
                        eventDatabaseId,
                        safeMessage(ex)
                );
            } catch (Exception ignored) {
                /*
                 * Do not hide the original payment-processing
                 * failure if ledger failure recording itself
                 * encounters a problem.
                 */
            }

            throw ex;
        }
    }

    private void validateAuthoritativePayment(
            RazorpayPaymentResponse payment,
            RazorpayPaymentAttempt attempt,
            String expectedPaymentId) {

        if (payment == null) {
            throw new IllegalStateException(
                    "Razorpay returned no payment."
            );
        }

        if (payment.id() == null
                || !expectedPaymentId.equals(
                        payment.id())) {

            throw new IllegalStateException(
                    "Razorpay payment ID does not match."
            );
        }

        if (payment.order_id() == null
                || !attempt.getRazorpayOrderId().equals(
                        payment.order_id())) {

            throw new IllegalStateException(
                    "Razorpay order ID does not match."
            );
        }

        if (payment.amount() == null
                || !payment.amount().equals(
                        attempt.getAmountPaise())) {

            throw new IllegalStateException(
                    "Razorpay payment amount does not match."
            );
        }

        if (!"INR".equals(payment.currency())
                || !payment.currency().equals(
                        attempt.getCurrency())) {

            throw new IllegalStateException(
                    "Razorpay payment currency does not match."
            );
        }

        if (!"captured".equalsIgnoreCase(
                payment.status())) {

            throw new IllegalStateException(
                    "Razorpay payment is not captured."
            );
        }

        if (!Boolean.TRUE.equals(
                payment.captured())) {

            throw new IllegalStateException(
                    "Razorpay has not confirmed capture."
            );
        }
    }

    private String requiredIdentifier(
            String value,
            String fieldName) {

        String normalized =
                normalize(value);

        if (normalized.isBlank()
                || normalized.length() > 100) {

            throw new IllegalArgumentException(
                    fieldName + " is missing or invalid."
            );
        }

        return normalized;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private String safeMessage(Exception ex) {

        String message =
                ex.getMessage();

        if (message == null
                || message.isBlank()) {

            return ex.getClass()
                    .getSimpleName();
        }

        message = message.trim();

        if (message.length() > 1900) {
            return message.substring(0, 1900);
        }

        return message;
    }

    public record ProcessingResult(
            String status,
            boolean paymentFinalized,
            boolean alreadyFinalized) {
    }
}