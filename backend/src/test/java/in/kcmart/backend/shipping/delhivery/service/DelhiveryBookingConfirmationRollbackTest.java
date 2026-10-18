package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingConfirmationRollbackTest {

    @Test
    void failureInSecondSavePropagatesAndPreventsSuccess() {

        var attempts =
            mock(DelhiveryShipmentBookingAttemptRepository.class);

        var shipments =
            mock(OrderShipmentRepository.class);

        var attempt =
            new DelhiveryShipmentBookingAttempt(18L);

        try {
            var field = DelhiveryShipmentBookingAttempt.class
                .getDeclaredField("attemptStatus");
            field.setAccessible(true);
            field.set(attempt, "PROCESSING");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "Unable to prepare PROCESSING test fixture.", e
            );
        }

        var shipment = new OrderShipment();
        shipment.setOrderId(18L);
        shipment.setShipmentStatus("PACKED");

        when(attempts.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(attempt));

        when(shipments.findByOrderIdForUpdate(18L))
            .thenReturn(Optional.of(shipment));

        when(attempts.saveAndFlush(any()))
            .thenThrow(new IllegalStateException("Simulated persistence failure"));

        var service =
            new DelhiveryBookingConfirmationService(attempts, shipments);

        assertThrows(
            IllegalStateException.class,
            () -> service.confirm(18L, "AWB123456")
        );

        verify(shipments).saveAndFlush(shipment);
        verify(attempts).saveAndFlush(attempt);
    }
}