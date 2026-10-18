package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingPreflightServiceTest {

    private final DelhiveryServiceabilityGate gate =
        mock(DelhiveryServiceabilityGate.class);

    private DelhiveryBookingPreflightService service(
            boolean enabled,
            boolean shipmentCreationEnabled) {

        DelhiveryProperties properties =
            new DelhiveryProperties(
                enabled,
                shipmentCreationEnabled,
                "http://127.0.0.1:1",
                "mock-token",
                "MOCK-PICKUP"
            );

        return new DelhiveryBookingPreflightService(
            properties, gate
        );
    }

    @Test
    void disabledIntegrationBlocksPreflight() {
        assertThrows(
            IllegalStateException.class,
            () -> service(false, false)
                .verifyServiceability("194103")
        );

        verifyNoInteractions(gate);
    }

    @Test
    void disabledShipmentCreationBlocksPreflight() {
        assertThrows(
            IllegalStateException.class,
            () -> service(true, false)
                .verifyServiceability("194103")
        );

        verifyNoInteractions(gate);
    }

    @Test
    void invalidPincodeBlocksPreflight() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service(true, true)
                .verifyServiceability("123")
        );

        verifyNoInteractions(gate);
    }

    @Test
    void validPincodeCallsServiceabilityGate() {
        service(true, true).verifyServiceability("194103");

        verify(gate, times(1))
            .requireServiceable("194103");
    }

    @Test
    void serviceabilityFailureStopsPreflight() {
        doThrow(new IllegalStateException("NSZ"))
            .when(gate).requireServiceable("194103");

        assertThrows(
            IllegalStateException.class,
            () -> service(true, true)
                .verifyServiceability("194103")
        );
    }
}