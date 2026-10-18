package in.kcmart.backend.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceEmailSafetyGateTest {

    @Test
    void disabledMailMustRejectInvoiceEmail() {
        InvoiceEmailSafetyGate gate =
            new InvoiceEmailSafetyGate(false);

        assertFalse(gate.isEnabled());

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                gate::requireEnabled
            );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );
    }

    @Test
    void enabledMailMustAllowInvoiceEmail() {
        InvoiceEmailSafetyGate gate =
            new InvoiceEmailSafetyGate(true);

        assertTrue(gate.isEnabled());

        assertDoesNotThrow(gate::requireEnabled);
    }
}