package in.kcmart.backend.shipping.delhivery.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisabledDelhiveryClientTest {

    private final DelhiveryClient client =
        new DisabledDelhiveryClient();

    @Test
    void shipmentCreationIsBlocked() {

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> client.createShipment("{}")
        );

        assertTrue(
            exception.getMessage().contains("disabled")
        );
    }

    @Test
    void shipmentTrackingIsBlocked() {

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> client.trackShipment("TEST-AWB-123")
        );

        assertTrue(
            exception.getMessage().contains("disabled")
        );
    }
}