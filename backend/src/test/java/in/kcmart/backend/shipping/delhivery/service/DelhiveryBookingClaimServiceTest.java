package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingClaimServiceTest {

    private final OrderRepository orders =
        mock(OrderRepository.class);

    private final OrderShipmentRepository shipments =
        mock(OrderShipmentRepository.class);

    private final DelhiveryShipmentBookingAttemptRepository attempts =
        mock(DelhiveryShipmentBookingAttemptRepository.class);

    private final DelhiveryShipmentPackageRepository packages =
        mock(DelhiveryShipmentPackageRepository.class);

    private final DelhiveryBookingEligibilityValidator validator =
        mock(DelhiveryBookingEligibilityValidator.class);

    private DelhiveryBookingClaimService service(
            boolean enabled,
            boolean shipmentCreationEnabled) {

        DelhiveryProperties properties = new DelhiveryProperties(
            enabled,
            shipmentCreationEnabled,
            "https://track.delhivery.com",
            "",
            ""
        );

        return new DelhiveryBookingClaimService(
            orders,
            shipments,
            attempts,
            properties,
            packages,
            validator
        );
    }

    @Test
    void rejectsWhenIntegrationDisabled() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service(false, false).claim(123L)
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );

        verifyNoInteractions(orders, shipments, attempts);
    }

    @Test
    void rejectsWhenShipmentCreationDisabled() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, false).claim(123L)
        );

        assertEquals(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getStatusCode()
        );

        verifyNoInteractions(orders, shipments, attempts);
    }

    @Test
    void rejectsInvalidOrderIdBeforeDatabaseAccess() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(0L)
        );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            exception.getStatusCode()
        );

        verifyNoInteractions(orders, shipments, attempts);
    }

    @Test
    void rejectsMissingOrder() {
        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(123L)
        );

        assertEquals(
            HttpStatus.NOT_FOUND,
            exception.getStatusCode()
        );

        verifyNoInteractions(shipments, attempts);
    }

    @Test
    void rejectsNonDeliveryOrder() {
        var order = mock(in.kcmart.backend.order.entity.Order.class);

        when(order.getFulfillmentType()).thenReturn("STORE_PICKUP");
        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(order));

        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(shipments, attempts);
    }

    @Test
    void rejectsOrderNotPacked() {
        var order = mock(in.kcmart.backend.order.entity.Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PENDING");

        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(order));

        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(shipments, attempts);
    }

    @Test
    void rejectsExistingShipment() {
        var order = mock(in.kcmart.backend.order.entity.Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");

        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(order));

        var existingShipment = new in.kcmart.backend.order.entity.OrderShipment();
        existingShipment.setOrderId(123L);
        existingShipment.setShipmentStatus("PACKED");
        existingShipment.setCarrierName("MANUAL_CARRIER");
        existingShipment.setTrackingNumber("MANUAL-TRACKING-001");

        when(shipments.findByOrderIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(existingShipment));

        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(attempts);
    }

    @Test
    void rejectsExistingBookingAttempt() {
        var order = mock(in.kcmart.backend.order.entity.Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");

        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(order));

        when(shipments.existsByOrderId(123L)).thenReturn(false);
        when(attempts.existsByOrderId(123L)).thenReturn(true);

        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service(true, true).claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void allowsEmptyPackedShipmentShellForBookingClaim() {
        var order = mock(in.kcmart.backend.order.entity.Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");

        when(orders.findByIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(order));

        var shell = new in.kcmart.backend.order.entity.OrderShipment();
        shell.setOrderId(123L);
        shell.setShipmentStatus("PACKED");

        when(shipments.existsByOrderId(123L)).thenReturn(true);

        when(shipments.findByOrderIdForUpdate(123L))
            .thenReturn(java.util.Optional.of(shell));

        var attempt =
            new in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt(123L);

        when(attempts.saveAndFlush(any(
                in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt.class)))
            .thenReturn(attempt);

        service(true, true).claim(123L);

        verify(attempts).saveAndFlush(any());
    }
}