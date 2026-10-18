package in.kcmart.backend.payment.razorpay.controller;

import in.kcmart.backend.payment.razorpay.security.RazorpayWebhookSignatureVerifier;
import in.kcmart.backend.payment.razorpay.service.RazorpayWebhookEventService;
import in.kcmart.backend.payment.razorpay.service.RazorpayWebhookProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/razorpay")
public class RazorpayWebhookController {

    private final RazorpayWebhookSignatureVerifier signatureVerifier;
    private final RazorpayWebhookEventService eventService;
    private final RazorpayWebhookProcessingService processingService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookController(
            RazorpayWebhookSignatureVerifier signatureVerifier,
            RazorpayWebhookEventService eventService,
            RazorpayWebhookProcessingService processingService,
            ObjectMapper objectMapper) {

        this.signatureVerifier = signatureVerifier;
        this.eventService = eventService;
        this.processingService = processingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false)
            String signature,

            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false)
            String eventIdHeader,

            @RequestBody byte[] rawBody) {

        if (signature == null || signature.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Missing Razorpay webhook signature."
                    ));
        }

        if (eventIdHeader == null
                || eventIdHeader.isBlank()) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Missing Razorpay webhook event ID."
                    ));
        }

        final boolean valid;

        try {
            valid =
                    signatureVerifier.verify(
                            rawBody,
                            signature
                    );

        } catch (IllegalStateException ex) {

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Webhook verification is not configured."
                    ));
        }

        if (!valid) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Invalid Razorpay webhook signature."
                    ));
        }

        final JsonNode root;

        try {
            root = objectMapper.readTree(rawBody);

        } catch (Exception ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Invalid webhook JSON."
                    ));
        }

        String eventId =
                eventIdHeader.trim();

        String eventType =
                root.path("event")
                        .asText("")
                        .trim();

        if (eventType.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "accepted", false,
                            "error",
                            "Webhook event type is missing."
                    ));
        }

        JsonNode payment =
                root.path("payload")
                        .path("payment")
                        .path("entity");

        String razorpayPaymentId =
                payment.path("id")
                        .asText("")
                        .trim();

        String razorpayOrderId =
                payment.path("order_id")
                        .asText("")
                        .trim();

        var recorded =
                eventService.recordAuthenticatedEvent(
                        eventId,
                        eventType,
                        emptyToNull(razorpayOrderId),
                        emptyToNull(razorpayPaymentId)
                );

        /*
         * Every authenticated webhook delivery must claim its
         * database event before payment processing begins.
         *
         * RECEIVED / FAILED -> PROCESSING -> CLAIMED
         * PROCESSING        -> BUSY
         * PROCESSED/IGNORED -> TERMINAL
         */
        var claim =
                eventService.claimForProcessing(
                        recorded.databaseId()
                );

        if ("TERMINAL".equals(claim.decision())
                || "BUSY".equals(claim.decision())) {

            return ResponseEntity.ok(
                    Map.of(
                            "accepted", true,
                            "duplicate", recorded.duplicate(),
                            "eventId", eventId,
                            "eventType", eventType,
                            "processingStatus",
                            claim.processingStatus(),
                            "claimDecision",
                            claim.decision(),
                            "attemptCount",
                            claim.attemptCount()
                    )
            );
        }

        try {

            var result =
                    processingService.process(
                            recorded.databaseId(),
                            eventType,
                            emptyToNull(razorpayOrderId),
                            emptyToNull(razorpayPaymentId)
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "accepted", true,
                            "duplicate", recorded.duplicate(),
                            "eventId", eventId,
                            "eventType", eventType,
                            "processingStatus",
                            result.status(),
                            "claimDecision",
                            claim.decision(),
                            "attemptCount",
                            claim.attemptCount(),
                            "paymentFinalized",
                            result.paymentFinalized(),
                            "alreadyFinalized",
                            result.alreadyFinalized()
                    )
            );

        } catch (Exception ex) {

            /*
             * ProcessingService records FAILED independently.
             * Return non-2xx so the provider can retry.
             */
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "accepted", true,
                            "duplicate", recorded.duplicate(),
                            "eventId", eventId,
                            "eventType", eventType,
                            "processingStatus", "FAILED",
                            "claimDecision",
                            claim.decision(),
                            "attemptCount",
                            claim.attemptCount()
                    ));
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value;
    }
}
