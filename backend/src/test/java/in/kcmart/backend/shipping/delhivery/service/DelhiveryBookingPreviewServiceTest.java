package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelhiveryBookingPreviewServiceTest {

    @Mock OrderRepository orders;
    @Mock OrderShipmentRepository shipments;
    @Mock DelhiveryShipmentPackageRepository packages;
    @Mock DelhiveryShipmentBookingAttemptRepository attempts;
    @Mock DelhiveryBookingEligibilityValidator validator;

    DelhiveryBookingPreviewService service;

    @BeforeEach
    void setup() {
        service = new DelhiveryBookingPreviewService(
            orders, shipments, packages, attempts, validator
        );
    }

    @Test
    void eligibleOrderIsLocallyReady() {
        Order order = mock(Order.class);
        DelhiveryShipmentPackage shipmentPackage =
            mock(DelhiveryShipmentPackage.class);

        when(orders.findById(1L)).thenReturn(Optional.of(order));
        when(packages.findByOrderId(1L))
            .thenReturn(Optional.of(shipmentPackage));

        var result = service.preview(1L);

        assertTrue(result.locallyReady());
        assertTrue(result.prepaidOnly());
        assertFalse(result.serviceabilityVerified());
        assertFalse(result.shipmentBooked());
        assertTrue(result.blockers().isEmpty());

        verify(validator).validate(order, shipmentPackage);
    }

    @Test
    void invalidOrderEligibilityIsBlocked() {
        Order order = mock(Order.class);

        when(orders.findById(2L)).thenReturn(Optional.of(order));
        doThrow(new ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Order must be PAID."
        )).when(validator).validate(eq(order), any());

        var result = service.preview(2L);

        assertFalse(result.locallyReady());
        assertFalse(result.blockers().isEmpty());
    }

    @Test
    void missingPackageIsBlockedByValidator() {
        Order order = mock(Order.class);

        when(orders.findById(3L)).thenReturn(Optional.of(order));

        doThrow(new ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Package missing."
        )).when(validator).validate(eq(order), isNull());

        var result = service.preview(3L);

        assertFalse(result.locallyReady());
    }

    @Test
    void existingTrackingNumberBlocksBooking() {
        Order order = mock(Order.class);
        OrderShipment shipment = mock(OrderShipment.class);

        when(orders.findById(4L)).thenReturn(Optional.of(order));
        when(shipments.findByOrderId(4L))
            .thenReturn(Optional.of(shipment));
        when(shipment.getTrackingNumber()).thenReturn("AWB123456");

        var result = service.preview(4L);

        assertFalse(result.locallyReady());
        assertTrue(result.shipmentBooked());
    }

    @Test
    void existingBookingAttemptBlocksBooking() {
        Order order = mock(Order.class);

        when(orders.findById(5L)).thenReturn(Optional.of(order));
        when(attempts.existsByOrderId(5L)).thenReturn(true);

        var result = service.preview(5L);

        assertFalse(result.locallyReady());
        assertTrue(result.blockers().stream()
            .anyMatch(s -> s.contains("booking attempt")));
    }

    @Test
    void invalidOrderIdIsRejected() {
        assertThrows(
            ResponseStatusException.class,
            () -> service.preview(0L)
        );

        verifyNoInteractions(
            orders, shipments, packages, attempts, validator
        );
    }
}