package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryLabelEligibilityValidatorTest {

    private final DelhiveryLabelEligibilityValidator validator =
        new DelhiveryLabelEligibilityValidator();

    private OrderShipment shipment() {
        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(10L);
        shipment.setCarrierName("Delhivery");
        shipment.setTrackingNumber("AWB123456");
        return shipment;
    }

    private DelhiveryShipmentBookingAttempt attempt() {
        DelhiveryShipmentBookingAttempt attempt =
            mock(DelhiveryShipmentBookingAttempt.class);

        when(attempt.getOrderId()).thenReturn(10L);
        when(attempt.getAttemptStatus()).thenReturn("SUCCEEDED");
        when(attempt.getWaybill()).thenReturn("AWB123456");

        return attempt;
    }

    @Test
    void confirmedShipmentIsEligible() {
        assertNull(validator.validate(10L, shipment(), attempt()));
    }

    @Test
    void missingShipmentIsRejected() {
        assertNotNull(validator.validate(10L, null, attempt()));
    }

    @Test
    void wrongOrderIsRejected() {
        assertNotNull(validator.validate(11L, shipment(), attempt()));
    }

    @Test
    void wrongCarrierIsRejected() {
        OrderShipment shipment = shipment();
        shipment.setCarrierName("Other");

        assertNotNull(validator.validate(10L, shipment, attempt()));
    }

    @Test
    void invalidAwbIsRejected() {
        OrderShipment shipment = shipment();
        shipment.setTrackingNumber("../invalid");

        assertNotNull(validator.validate(10L, shipment, attempt()));
    }

    @Test
    void missingAttemptIsRejected() {
        assertNotNull(validator.validate(10L, shipment(), null));
    }

    @Test
    void unconfirmedAttemptIsRejected() {
        DelhiveryShipmentBookingAttempt attempt = attempt();
        when(attempt.getAttemptStatus()).thenReturn("UNKNOWN");

        assertNotNull(validator.validate(10L, shipment(), attempt));
    }

    @Test
    void mismatchedAwbIsRejected() {
        DelhiveryShipmentBookingAttempt attempt = attempt();
        when(attempt.getWaybill()).thenReturn("DIFFERENT123");

        assertNotNull(validator.validate(10L, shipment(), attempt));
    }
}