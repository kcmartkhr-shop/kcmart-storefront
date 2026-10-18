package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayWebhookEvent;
import in.kcmart.backend.payment.razorpay.repository.RazorpayWebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class RazorpayWebhookEventService {

    private final RazorpayWebhookEventRepository events;

    public RazorpayWebhookEventService(
            RazorpayWebhookEventRepository events) {

        this.events = events;
    }

    @Transactional
    public RecordResult recordAuthenticatedEvent(
            String eventId,
            String eventType,
            String razorpayOrderId,
            String razorpayPaymentId) {

        String safeEventId =
                required(eventId, "eventId", 150);

        String safeEventType =
                required(eventType, "eventType", 100);

        String safeOrderId =
                optional(razorpayOrderId, 100);

        String safePaymentId =
                optional(razorpayPaymentId, 100);

        int inserted =
                events.insertAuthenticatedEventIfAbsent(
                        safeEventId,
                        safeEventType,
                        safeOrderId,
                        safePaymentId
                );

        RazorpayWebhookEvent recorded =
                events.findByEventId(safeEventId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Webhook event was not available after atomic insert."
                                )
                        );

        return new RecordResult(
                recorded.getId(),
                inserted == 0
        );
    }
    /*
     * These state changes use their own short transactions.
     *
     * Payment finalization is deliberately NOT part of these
     * transactions.
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claimForProcessing(Long databaseId) {

        if (databaseId == null || databaseId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Webhook event not found."
            );
        }

        RazorpayWebhookEvent event =
                events.findByIdForUpdate(databaseId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Webhook event not found."
                                )
                        );

        String status = event.getProcessingStatus();

        if ("PROCESSED".equals(status)
                || "IGNORED".equals(status)) {

            return new ClaimResult(
                    "TERMINAL",
                    status,
                    event.getAttemptCount()
            );
        }

        if ("PROCESSING".equals(status)) {

            OffsetDateTime processingStartedAt =
                    event.getProcessingStartedAt();

            OffsetDateTime staleBefore =
                    OffsetDateTime.now().minusMinutes(5);

            boolean stale =
                    processingStartedAt == null
                            || !processingStartedAt.isAfter(staleBefore);

            if (!stale) {
                return new ClaimResult(
                        "BUSY",
                        status,
                        event.getAttemptCount()
                );
            }

            /*
             * The previous worker claimed this event but did not
             * finish within the recovery window. Allow this delivery
             * to reclaim it below.
             */
            status = "FAILED";
        }

        if (!"RECEIVED".equals(status)
                && !"FAILED".equals(status)) {

            throw new IllegalStateException(
                    "Unsupported webhook processing status: "
                            + status
            );
        }

        int nextAttempt = Math.addExact(
                event.getAttemptCount(),
                1
        );

        event.setProcessingStatus("PROCESSING");
        event.setAttemptCount(nextAttempt);
        event.setProcessingStartedAt(
                OffsetDateTime.now()
        );
        event.setProcessedAt(null);
        event.setLastError(null);

        events.save(event);

        return new ClaimResult(
                "CLAIMED",
                "PROCESSING",
                nextAttempt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(Long databaseId) {

        RazorpayWebhookEvent event =
                requireEvent(databaseId);

        if ("PROCESSED".equals(
                event.getProcessingStatus())) {
            return;
        }

        event.setProcessingStatus("PROCESSED");
        event.setProcessingStartedAt(null);
        event.setLastError(null);
        event.setProcessedAt(OffsetDateTime.now());

        events.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIgnored(
            Long databaseId,
            String reason) {

        RazorpayWebhookEvent event =
                requireEvent(databaseId);

        if ("PROCESSED".equals(
                event.getProcessingStatus())) {
            return;
        }

        event.setProcessingStatus("IGNORED");
        event.setProcessingStartedAt(null);
        event.setLastError(
                safeError(reason)
        );
        event.setProcessedAt(OffsetDateTime.now());

        events.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            Long databaseId,
            String error) {

        RazorpayWebhookEvent event =
                requireEvent(databaseId);

        if ("PROCESSED".equals(
                event.getProcessingStatus())) {
            return;
        }

        event.setProcessingStatus("FAILED");
        event.setProcessingStartedAt(null);
        event.setLastError(
                safeError(error)
        );
        event.setProcessedAt(OffsetDateTime.now());

        events.save(event);
    }

    @Transactional(readOnly = true)
    public EventState getEventState(
            Long databaseId) {

        RazorpayWebhookEvent event =
                requireEvent(databaseId);

        return new EventState(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getProcessingStatus(),
                event.getRazorpayOrderId(),
                event.getRazorpayPaymentId(),
                event.getLastError()
        );
    }
    private RazorpayWebhookEvent requireEvent(
            Long databaseId) {

        if (databaseId == null
                || databaseId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Webhook event not found."
            );
        }

        return events.findById(databaseId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Webhook event not found."
                        )
                );
    }

    private String safeError(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > 2000) {
            return normalized.substring(0, 2000);
        }

        return normalized;
    }

    private String required(
            String value,
            String field,
            int maxLength) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required."
            );
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " is too long."
            );
        }

        return normalized;
    }

    private String optional(
            String value,
            int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Webhook identifier is too long."
            );
        }

        return normalized;
    }

    public record ClaimResult(
            String decision,
            String processingStatus,
            int attemptCount) {
    }
    public record EventState(
            Long databaseId,
            String eventId,
            String eventType,
            String processingStatus,
            String razorpayOrderId,
            String razorpayPaymentId,
            String lastError) {
    }

    public record RecordResult(
            Long databaseId,
            boolean duplicate) {
    }
}