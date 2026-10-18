package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryLabelEligibilityServiceTest {

    private final OrderShipmentRepository shipments =
        mock(OrderShipmentRepository.class);

    private final DelhiveryShipmentBookingAttemptRepository attempts =
        mock(DelhiveryShipmentBookingAttemptRepository.class);

    private final DelhiveryLabelEligibilityService service =
        new DelhiveryLabelEligibilityService(
            shipments,
            attempts,
            new DelhiveryLabelEligibilityValidator()
        );

    @Test
    void invalidOrderDoesNotQueryDatabase() {
        var result = service.check(-1L);

        assertFalse(result.eligible());
        verifyNoInteractions(shipments, attempts);
    }

    @Test
    void missingShipmentIsNotEligible() {
        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.empty());

        when(attempts.findByOrderId(10L))
            .thenReturn(Optional.empty());

        var result = service.check(10L);

        assertFalse(result.eligible());
        assertNotNull(result.blocker());
    }

    @Test
    void confirmedMatchingShipmentIsEligible() {
        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(10L);
        shipment.setCarrierName("Delhivery");
        shipment.setTrackingNumber("AWB123456");

        DelhiveryShipmentBookingAttempt attempt =
            mock(DelhiveryShipmentBookingAttempt.class);

        when(attempt.getOrderId()).thenReturn(10L);
        when(attempt.getAttemptStatus()).thenReturn("SUCCEEDED");
        when(attempt.getWaybill()).thenReturn("AWB123456");

        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.of(shipment));

        when(attempts.findByOrderId(10L))
            .thenReturn(Optional.of(attempt));

        var result = service.check(10L);

        assertTrue(result.eligible());
        assertNull(result.blocker());

        verify(shipments).findByOrderId(10L);
        verify(attempts).findByOrderId(10L);
        verifyNoMoreInteractions(shipments, attempts);
    }

    @Test
    void mismatchedWaybillIsNotEligible() {
        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(10L);
        shipment.setCarrierName("Delhivery");
        shipment.setTrackingNumber("AWB123456");

        DelhiveryShipmentBookingAttempt attempt =
            mock(DelhiveryShipmentBookingAttempt.class);

        when(attempt.getOrderId()).thenReturn(10L);
        when(attempt.getAttemptStatus()).thenReturn("SUCCEEDED");
        when(attempt.getWaybill()).thenReturn("AWB999999");

        when(shipments.findByOrderId(10L))
            .thenReturn(Optional.of(shipment));

        when(attempts.findByOrderId(10L))
            .thenReturn(Optional.of(attempt));

        var result = service.check(10L);

        assertFalse(result.eligible());
        assertNotNull(result.blocker());
    }
}