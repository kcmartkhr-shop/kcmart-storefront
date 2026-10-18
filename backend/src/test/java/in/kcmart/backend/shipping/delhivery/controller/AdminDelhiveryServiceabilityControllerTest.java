package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryServiceabilityGate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDelhiveryServiceabilityControllerTest {


private final DelhiveryServiceabilityGate gate =
        mock(DelhiveryServiceabilityGate.class);

    private AdminDelhiveryServiceabilityController controller(
            boolean enabled) {

        DelhiveryProperties properties = new DelhiveryProperties(
            enabled,
            "http://127.0.0.1:1",
            "mock-token",
            "MOCK-PICKUP"
        );

        return new AdminDelhiveryServiceabilityController(
            properties,
            gate
        );
    }

    @Test
    void validPincodeReturnsPrepaidServiceability() {

        Map<String, Object> result =
            controller(true).check("751001");

        assertEquals("751001", result.get("pincode"));
        assertEquals(true, result.get("serviceable"));
        assertEquals(true, result.get("prepaid"));
        assertFalse(result.containsKey("cod"));
        assertEquals(false, result.get("shipmentBooked"));

        verify(gate).requireServiceable("751001");
    }

    @Test
    void invalidPincodeReturns400WithoutCallingGate() {
        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controller(true).check("123")
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(gate);
    }

    @Test
    void disabledIntegrationReturns503WithoutCallingGate() {
        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controller(false).check("751001")
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            error.getStatusCode()
        );

        verifyNoInteractions(gate);
    }

    @Test
    void nonServiceablePincodeNeverReturnsSuccess() {
        doThrow(new IllegalStateException("NSZ"))
            .when(gate).requireServiceable("751001");

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controller(true).check("751001")
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            error.getStatusCode()
        );
    }

    @Test
    void embargoNeverReturnsSuccess() {
        doThrow(new IllegalStateException("Embargo"))
            .when(gate).requireServiceable("751001");

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> controller(true).check("751001")
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            error.getStatusCode()
        );
    }
}