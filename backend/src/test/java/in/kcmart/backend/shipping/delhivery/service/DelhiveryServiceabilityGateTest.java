package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryServiceabilityClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryServiceabilityGateTest {

    private final DelhiveryServiceabilityClient client =
        mock(DelhiveryServiceabilityClient.class);

    private final DelhiveryPincodeServiceabilityValidator validator =
        new DelhiveryPincodeServiceabilityValidator(new ObjectMapper());

    private final DelhiveryServiceabilityGate gate =
        new DelhiveryServiceabilityGate(client, validator);

    @Test
    void disabledIntegrationBlocksServiceability() {
        when(client.fetchPincode("194103"))
            .thenThrow(new IllegalStateException(
                "Delhivery integration is disabled."
            ));

        assertThrows(
            IllegalStateException.class,
            () -> gate.requireServiceable("194103")
        );

        verify(client, times(1)).fetchPincode("194103");
    }

    @Test
    void invalidPincodeIsRejected() {
        when(client.fetchPincode("123"))
            .thenThrow(new IllegalArgumentException(
                "Invalid destination pincode."
            ));

        assertThrows(
            IllegalArgumentException.class,
            () -> gate.requireServiceable("123")
        );
    }

    @Test
    void nonServiceablePincodeIsRejected() {
        when(client.fetchPincode("194103")).thenReturn("{\"delivery_codes\":[]}");

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> gate.requireServiceable("194103")
        );

        assertTrue(error.getMessage().contains("NSZ"));
    }

    @Test
    void unverifiedJsonCannotApproveShipment() {
        when(client.fetchPincode("194103")).thenReturn(
            "{\"delivery_codes\":[{\"postal_code\":{\"pin\":194103,\"remark\":\"\"}}]}"
        );

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> gate.requireServiceable("194103")
        );

        assertTrue(error.getMessage().contains("Prepaid delivery is not supported"));
    }
}