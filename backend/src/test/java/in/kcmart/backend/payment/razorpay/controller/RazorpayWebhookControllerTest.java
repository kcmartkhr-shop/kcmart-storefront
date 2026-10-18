package in.kcmart.backend.payment.razorpay.controller;

import in.kcmart.backend.payment.razorpay.security.RazorpayWebhookSignatureVerifier;
import in.kcmart.backend.payment.razorpay.service.RazorpayWebhookEventService;
import in.kcmart.backend.payment.razorpay.service.RazorpayWebhookProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookControllerTest {

    @Mock
    RazorpayWebhookSignatureVerifier verifier;

    @Mock
    RazorpayWebhookEventService events;

    @Mock
    RazorpayWebhookProcessingService processing;

    ObjectMapper mapper = new ObjectMapper();

    RazorpayWebhookController controller;

    final byte[] body = (
        "{\"event\":\"payment.captured\","
        + "\"payload\":{\"payment\":{\"entity\":{"
        + "\"id\":\"pay_test\","
        + "\"order_id\":\"order_test\"}}}}"
    ).getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        controller = new RazorpayWebhookController(
            verifier, events, processing, mapper
        );
    }

    @Test
    void missingSignatureReturns400() {
        ResponseEntity<?> response =
            controller.receiveWebhook(null, "evt_1", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(events, processing);
    }

    @Test
    void missingEventIdReturns400() {
        ResponseEntity<?> response =
            controller.receiveWebhook("signature", null, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(events, processing);
    }

    @Test
    void invalidSignatureReturns400() {
        when(verifier.verify(body, "bad")).thenReturn(false);

        ResponseEntity<?> response =
            controller.receiveWebhook("bad", "evt_1", body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(events, processing);
    }

    @Test
    void missingWebhookConfigurationReturns503() {
        when(verifier.verify(body, "signature"))
            .thenThrow(new IllegalStateException("not configured"));

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", body);

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            response.getStatusCode()
        );
        verifyNoInteractions(events, processing);
    }

    @Test
    void invalidJsonReturns400() {
        byte[] invalidBody = "{bad-json".getBytes(StandardCharsets.UTF_8);
        when(verifier.verify(invalidBody, "signature")).thenReturn(true);

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", invalidBody);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(events, processing);
    }

    @Test
    void missingEventTypeReturns400() {
        byte[] emptyEvent = "{\"payload\":{}}".getBytes(StandardCharsets.UTF_8);
        when(verifier.verify(emptyEvent, "signature")).thenReturn(true);

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", emptyEvent);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(events, processing);
    }

    @Test
    void terminalDuplicateReturnsAcceptedWithoutProcessing() {
        when(verifier.verify(body, "signature")).thenReturn(true);
        when(events.recordAuthenticatedEvent(
            "evt_1", "payment.captured", "order_test", "pay_test"
        )).thenReturn(new RazorpayWebhookEventService.RecordResult(7L, true));

        when(events.claimForProcessing(7L)).thenReturn(
            new RazorpayWebhookEventService.ClaimResult(
                "TERMINAL", "PROCESSED", 1
            )
        );

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(processing);
    }

    @Test
    void busyEventReturnsAcceptedWithoutProcessing() {
        when(verifier.verify(body, "signature")).thenReturn(true);
        when(events.recordAuthenticatedEvent(
            "evt_1", "payment.captured", "order_test", "pay_test"
        )).thenReturn(new RazorpayWebhookEventService.RecordResult(7L, true));

        when(events.claimForProcessing(7L)).thenReturn(
            new RazorpayWebhookEventService.ClaimResult(
                "BUSY", "PROCESSING", 1
            )
        );

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verifyNoInteractions(processing);
    }

    @Test
    void successfulProcessingReturnsAccepted() {
        when(verifier.verify(body, "signature")).thenReturn(true);
        when(events.recordAuthenticatedEvent(
            "evt_1", "payment.captured", "order_test", "pay_test"
        )).thenReturn(new RazorpayWebhookEventService.RecordResult(7L, false));

        when(events.claimForProcessing(7L)).thenReturn(
            new RazorpayWebhookEventService.ClaimResult(
                "CLAIMED", "PROCESSING", 1
            )
        );

        when(processing.process(
            7L, "payment.captured", "order_test", "pay_test"
        )).thenReturn(
            new RazorpayWebhookProcessingService.ProcessingResult(
                "PROCESSED", true, false
            )
        );

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(processing).process(
            7L, "payment.captured", "order_test", "pay_test"
        );
    }

    @Test
    void processingFailureReturns500() {
        when(verifier.verify(body, "signature")).thenReturn(true);
        when(events.recordAuthenticatedEvent(
            "evt_1", "payment.captured", "order_test", "pay_test"
        )).thenReturn(new RazorpayWebhookEventService.RecordResult(7L, false));

        when(events.claimForProcessing(7L)).thenReturn(
            new RazorpayWebhookEventService.ClaimResult(
                "CLAIMED", "PROCESSING", 1
            )
        );

        when(processing.process(
            7L, "payment.captured", "order_test", "pay_test"
        )).thenThrow(new IllegalStateException("test failure"));

        ResponseEntity<?> response =
            controller.receiveWebhook("signature", "evt_1", body);

        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            response.getStatusCode()
        );
    }

    @Test
    void eventRecordingFailureDoesNotInvokeProcessing() {
        when(verifier.verify(body, "signature")).thenReturn(true);
        when(events.recordAuthenticatedEvent(
            "evt_1", "payment.captured", "order_test", "pay_test"
        )).thenThrow(new ResponseStatusException(
            HttpStatus.CONFLICT, "test failure"
        ));

        assertThrows(
            ResponseStatusException.class,
            () -> controller.receiveWebhook("signature", "evt_1", body)
        );

        verifyNoInteractions(processing);
    }
}