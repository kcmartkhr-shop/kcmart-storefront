package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.shipping.delhivery.client.DelhiveryClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelhiveryTrackingServiceTest {

    private static class FakeDelhiveryClient
            implements DelhiveryClient {

        int trackingCalls = 0;
        String lastWaybill;

        @Override
        public String createShipment(String requestBody) {
            throw new UnsupportedOperationException(
                "Shipment creation must remain disabled."
            );
        }

        @Override
        public String trackShipment(String waybill) {
            trackingCalls++;
            lastWaybill = waybill;
            return "{\"status\":\"mock-tracking-success\"}";
        }
    }

    @Test
    void validWaybillReturnsTrackingResponse() {

        FakeDelhiveryClient fake = new FakeDelhiveryClient();
        DelhiveryTrackingService service =
            new DelhiveryTrackingService(fake);

        String response = service.track("TEST-AWB-123");

        assertEquals(
            "{\"status\":\"mock-tracking-success\"}",
            response
        );

        assertEquals(1, fake.trackingCalls);
        assertEquals("TEST-AWB-123", fake.lastWaybill);
    }

    @Test
    void invalidWaybillIsRejectedWithoutClientCall() {

        FakeDelhiveryClient fake = new FakeDelhiveryClient();
        DelhiveryTrackingService service =
            new DelhiveryTrackingService(fake);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.track("../invalid")
        );

        assertEquals(0, fake.trackingCalls);
    }

    @Test
    void nullWaybillIsRejectedWithoutClientCall() {

        FakeDelhiveryClient fake = new FakeDelhiveryClient();
        DelhiveryTrackingService service =
            new DelhiveryTrackingService(fake);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.track(null)
        );

        assertEquals(0, fake.trackingCalls);
    }

    @Test
    void trackingDoesNotInvokeShipmentCreation() {

        FakeDelhiveryClient fake = new FakeDelhiveryClient();
        DelhiveryTrackingService service =
            new DelhiveryTrackingService(fake);

        assertDoesNotThrow(
            () -> service.track("TEST-AWB-123")
        );

        assertEquals(1, fake.trackingCalls);
    }
}