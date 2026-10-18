package in.kcmart.backend.shipping.delhivery.controller;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryClient;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.service.DelhiveryTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AdminDelhiveryTrackingControllerTest {

    static class FakeClient implements DelhiveryClient {

        int trackingCalls;

        @Override
        public String createShipment(String requestBody) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String trackShipment(String waybill) {
            trackingCalls++;
            return "{\"mock\":true}";
        }
    }

    private AdminDelhiveryTrackingController controller(
            boolean enabled,
            FakeClient client) {

        DelhiveryProperties properties =
            new DelhiveryProperties(
                enabled,
                "http://127.0.0.1:1",
                "mock-token",
                "MOCK-PICKUP"
            );

        return new AdminDelhiveryTrackingController(
            new DelhiveryTrackingService(client),
            properties
        );
    }

    @Test
    void disabledIntegrationReturns503() {

        FakeClient client = new FakeClient();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller(false, client).track("TEST-AWB-123")
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );

        assertEquals(0, client.trackingCalls);
    }

    @Test
    void invalidWaybillReturns400() {

        FakeClient client = new FakeClient();

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller(true, client).track("../invalid")
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        assertEquals(0, client.trackingCalls);
    }

    @Test
    void validWaybillReturnsMockTrackingData() {

        FakeClient client = new FakeClient();

        var response =
            controller(true, client).track("TEST-AWB-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"mock\":true}", response.getBody());
        assertEquals(1, client.trackingCalls);
    }
}