package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingConfirmationServiceTest {

    private final DelhiveryShipmentBookingAttemptRepository attempts =
        mock(DelhiveryShipmentBookingAttemptRepository.class);

    private final OrderShipmentRepository shipments =
        mock(OrderShipmentRepository.class);

    private final DelhiveryBookingConfirmationService service =
        new DelhiveryBookingConfirmationService(attempts, shipments);

    private DelhiveryShipmentBookingAttempt processingAttempt() {
        var attempt = new DelhiveryShipmentBookingAttempt(18L);

        try {
            var field = DelhiveryShipmentBookingAttempt.class
                .getDeclaredField("attemptStatus");
            field.setAccessible(true);
            field.set(attempt, "PROCESSING");
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }

        return attempt;
    }

    private OrderShipment packedShipment() {
        OrderShipment shipment = new OrderShipment();
        shipment.setOrderId(18L);
        shipment.setShipmentStatus("PACKED");
        return shipment;
    }

    @Test
    void successfulConfirmationSavesAwbAndAttempt() {
        var attempt = processingAttempt();
        var shipment = packedShipment();

        when(attempts.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(attempt));
        when(shipments.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(shipment));

        service.confirm(18L, "AWB123456");

        assertEquals("SUCCEEDED", attempt.getAttemptStatus());
        assertEquals("AWB123456", attempt.getWaybill());
        assertEquals("Delhivery", shipment.getCarrierName());
        assertEquals("AWB123456", shipment.getTrackingNumber());
        assertEquals("PACKED", shipment.getShipmentStatus());

        verify(shipments).saveAndFlush(shipment);
        verify(attempts).saveAndFlush(attempt);
    }

    @Test
    void invalidWaybillCannotModifyDatabase() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.confirm(18L, "bad!")
        );

        verifyNoInteractions(attempts, shipments);
    }

    @Test
    void duplicateConfirmationIsRejected() {
        var attempt = processingAttempt();
        attempt.markSucceeded("AWB123456");

        when(attempts.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(attempt));

        assertThrows(
            IllegalStateException.class,
            () -> service.confirm(18L, "AWB999999")
        );

        verifyNoInteractions(shipments);
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void missingShipmentCannotConfirm() {
        when(attempts.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(processingAttempt()));
        when(shipments.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> service.confirm(18L, "AWB123456")
        );

        verify(attempts, never()).saveAndFlush(any());
        verify(shipments, never()).saveAndFlush(any());
    }

    @Test
    void existingCarrierCannotBeOverwritten() {
        var shipment = packedShipment();
        shipment.setCarrierName("OtherCourier");

        when(attempts.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(processingAttempt()));
        when(shipments.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(shipment));

        assertThrows(
            IllegalStateException.class,
            () -> service.confirm(18L, "AWB123456")
        );

        assertEquals("OtherCourier", shipment.getCarrierName());
        verify(shipments, never()).saveAndFlush(any());
        verify(attempts, never()).saveAndFlush(any());
    }
}