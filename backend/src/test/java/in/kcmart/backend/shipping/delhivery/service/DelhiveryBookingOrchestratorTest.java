package in.kcmart.backend.shipping.delhivery.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingOrchestratorTest {

    @Test
    void bookingIsHardDisabledAndDoesNotPrepareAttempt() {

        DelhiveryBookingPreparationService preparation =
            mock(DelhiveryBookingPreparationService.class);

        DelhiveryBookingOrchestrator orchestrator =
            new DelhiveryBookingOrchestrator(preparation);

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.book(123L)
        );

        assertTrue(error.getMessage().contains("not activated"));

        verifyNoInteractions(preparation);
    }
}