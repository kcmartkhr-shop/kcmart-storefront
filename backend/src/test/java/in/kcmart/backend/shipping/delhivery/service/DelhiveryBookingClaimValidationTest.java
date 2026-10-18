package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderShipment;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryProperties;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentBookingAttempt;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingClaimValidationTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderShipmentRepository shipments = mock(OrderShipmentRepository.class);
    private final DelhiveryShipmentBookingAttemptRepository attempts =
            mock(DelhiveryShipmentBookingAttemptRepository.class);
    private final DelhiveryShipmentPackageRepository packages =
            mock(DelhiveryShipmentPackageRepository.class);

    private final DelhiveryBookingEligibilityValidator validator =
            new DelhiveryBookingEligibilityValidator();

    private DelhiveryBookingClaimService service() {
        var properties = new DelhiveryProperties(
                true,
                true,
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

    private Order validOrder() {
        Order order = mock(Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getPaymentStatus()).thenReturn("PAID");
        when(order.getOrderNumber()).thenReturn("KCM-TEST-05310");
        when(order.getCustomerName()).thenReturn("Test Customer");
        when(order.getCustomerPhone()).thenReturn("9876543210");
        when(order.getAddressLine1()).thenReturn("Test Street");
        when(order.getCity()).thenReturn("Bhubaneswar");
        when(order.getState()).thenReturn("Odisha");
        when(order.getPostalCode()).thenReturn("751001");

        when(orders.findByIdForUpdate(123L)).thenReturn(Optional.of(order));

        OrderShipment shell = new OrderShipment();
        shell.setOrderId(123L);
        shell.setShipmentStatus("PACKED");

        when(shipments.findByOrderIdForUpdate(123L))
                .thenReturn(Optional.of(shell));

        return order;
    }

    private DelhiveryShipmentPackage validPackage() {
        return new DelhiveryShipmentPackage(
                123L,
                2000,
                new BigDecimal("15"),
                new BigDecimal("3"),
                new BigDecimal("55"),
                495,
                2000
        );
    }

    @Test
    void rejectsMissingSavedPackageWithoutBookingAttempt() {
        validOrder();

        when(packages.findByOrderId(123L)).thenReturn(Optional.empty());

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service().claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnpaidOrderWithoutBookingAttempt() {
        Order order = validOrder();
        when(order.getPaymentStatus()).thenReturn("PENDING");

        when(packages.findByOrderId(123L))
                .thenReturn(Optional.of(validPackage()));

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service().claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInvalidPinWithoutBookingAttempt() {
        Order order = validOrder();
        when(order.getPostalCode()).thenReturn("123");

        when(packages.findByOrderId(123L))
                .thenReturn(Optional.of(validPackage()));

        assertThrows(
                ResponseStatusException.class,
                () -> service().claim(123L)
        );

        verify(attempts, never()).saveAndFlush(any());
    }

    @Test
    void validPackedPrepaidOrderCreatesSingleBookingAttempt() {
        validOrder();

        when(packages.findByOrderId(123L))
                .thenReturn(Optional.of(validPackage()));

        when(attempts.saveAndFlush(any(DelhiveryShipmentBookingAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service().claim(123L));

        verify(attempts, times(1))
                .saveAndFlush(any(DelhiveryShipmentBookingAttempt.class));
    }

    @Test
    void existingBookingAttemptBlocksNewClaim() {
        validOrder();

        when(attempts.existsByOrderId(123L)).thenReturn(true);

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service().claim(123L)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(attempts, never()).saveAndFlush(any());
        verifyNoInteractions(packages);
    }
}