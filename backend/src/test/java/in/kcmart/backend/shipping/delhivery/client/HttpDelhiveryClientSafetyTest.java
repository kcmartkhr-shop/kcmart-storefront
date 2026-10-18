package in.kcmart.backend.shipping.delhivery.client;

import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpDelhiveryClientSafetyTest {

    @Test
    void shipmentCreationRemainsBlocked() {

        DelhiveryProperties properties = new DelhiveryProperties(
            true,
            "http://127.0.0.1:1",
            "mock-test-token",
            "MOCK-PICKUP"
        );

        DelhiveryClient client = new HttpDelhiveryClient(
            RestClient.builder(),
            properties
        );

        assertThrows(
            IllegalStateException.class,
            () -> client.createShipment("{}")
        );
    }
}