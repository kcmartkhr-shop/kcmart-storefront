package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingResultServiceTest {

    private final DelhiveryShipmentBookingAttemptRepository repository =
        mock(DelhiveryShipmentBookingAttemptRepository.class);

    private final DelhiveryBookingResultService service =
        new DelhiveryBookingResultService(repository);

    private DelhiveryShipmentBookingAttempt processingAttempt() {
        var attempt = new DelhiveryShipmentBookingAttempt(123L);

        // Simulate a claim already committed by the execution claim service.
        try {
            Field status = DelhiveryShipmentBookingAttempt.class
                .getDeclaredField("attemptStatus");
            status.setAccessible(true);
            status.set(attempt, "PROCESSING");
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }

        when(repository.findByOrderIdForUpdate(123L))
            .thenReturn(Optional.of(attempt));

        return attempt;
    }

    @Test
    void directSuccessIsDisabled() {
        assertThrows(
            IllegalStateException.class,
            () -> service.recordSuccess(123L, "AWB123456")
        );

        verifyNoInteractions(repository);
    }

    @Test
    void timeoutMarksProcessingAttemptUnknown() {
        var attempt = processingAttempt();

        service.recordUnknown(123L, "Response timeout");

        assertEquals("UNKNOWN", attempt.getAttemptStatus());
        verify(repository).saveAndFlush(attempt);
    }

    @Test
    void confirmedFailureMarksProcessingAttemptFailed() {
        var attempt = processingAttempt();

        service.recordConfirmedFailure(123L, "Confirmed rejection");

        assertEquals("FAILED", attempt.getAttemptStatus());
        verify(repository).saveAndFlush(attempt);
    }

    @Test
    void unknownCannotBecomeFailed() {
        var attempt = processingAttempt();
        service.recordUnknown(123L, "Timeout");

        assertThrows(
            IllegalStateException.class,
            () -> service.recordConfirmedFailure(123L, "Late failure")
        );

        assertEquals("UNKNOWN", attempt.getAttemptStatus());
        verify(repository, times(1)).saveAndFlush(attempt);
    }

    @Test
    void failedCannotBecomeUnknown() {
        var attempt = processingAttempt();
        service.recordConfirmedFailure(123L, "Confirmed rejection");

        assertThrows(
            IllegalStateException.class,
            () -> service.recordUnknown(123L, "Late timeout")
        );

        assertEquals("FAILED", attempt.getAttemptStatus());
        verify(repository, times(1)).saveAndFlush(attempt);
    }

    @Test
    void missingAttemptRejected() {
        when(repository.findByOrderIdForUpdate(123L))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> service.recordUnknown(123L, "Timeout")
        );

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidOrderIdRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.recordUnknown(0L, "Timeout")
        );

        verifyNoInteractions(repository);
    }
}