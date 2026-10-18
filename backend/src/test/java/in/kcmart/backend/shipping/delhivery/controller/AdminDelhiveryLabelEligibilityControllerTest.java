package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.service.DelhiveryLabelEligibilityService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDelhiveryLabelEligibilityControllerTest {

    private final DelhiveryLabelEligibilityService service =
        mock(DelhiveryLabelEligibilityService.class);

    private final AdminDelhiveryLabelEligibilityController controller =
        new AdminDelhiveryLabelEligibilityController(service);

    @Test
    void eligibleOrderReturnsSuccess() {
        var result =
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                true, null
            );

        when(service.check(10L)).thenReturn(result);

        var response = controller.check(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
        assertTrue(response.getHeaders()
            .getCacheControl().contains("no-store"));

        verify(service).check(10L);
    }

    @Test
    void blockedOrderReturnsBlocker() {
        var result =
            new DelhiveryLabelEligibilityService.LabelEligibilityResult(
                false, "Delhivery booking is not confirmed."
            );

        when(service.check(10L)).thenReturn(result);

        var response = controller.check(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().eligible());
        assertNotNull(response.getBody().blocker());

        verify(service).check(10L);
    }

    @Test
    void invalidOrderIsRejectedBeforeServiceCall() {
        var exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.check(0L)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        verifyNoInteractions(service);
    }

    @Test
    void nullOrderIsRejectedBeforeServiceCall() {
        assertThrows(
            ResponseStatusException.class,
            () -> controller.check(null)
        );

        verifyNoInteractions(service);
    }
}