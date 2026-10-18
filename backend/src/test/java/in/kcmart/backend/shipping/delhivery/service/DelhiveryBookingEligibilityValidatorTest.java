package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingEligibilityValidatorTest {

    private final DelhiveryBookingEligibilityValidator validator =
            new DelhiveryBookingEligibilityValidator();

    private Order validOrder() {
        Order order = mock(Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getOrderStatus()).thenReturn("PACKED");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getPaymentStatus()).thenReturn("PAID");
        when(order.getOrderNumber()).thenReturn("KCM-TEST-001");
        when(order.getCustomerName()).thenReturn("Test Customer");
        when(order.getCustomerPhone()).thenReturn("9876543210");
        when(order.getAddressLine1()).thenReturn("Test Street");
        when(order.getCity()).thenReturn("Bhubaneswar");
        when(order.getState()).thenReturn("Odisha");
        when(order.getPostalCode()).thenReturn("751001");

        return order;
    }

    private DelhiveryShipmentPackage validPackage() {
        return new DelhiveryShipmentPackage(
                1L,
                2000,
                new BigDecimal("15"),
                new BigDecimal("3"),
                new BigDecimal("55"),
                495,
                2000
        );
    }

    @Test
    void acceptsValidPrepaidPackedDelivery() {
        assertDoesNotThrow(
                () -> validator.validate(validOrder(), validPackage())
        );
    }

    @Test
    void rejectsUnpaidDelivery() {
        Order order = validOrder();
        when(order.getPaymentStatus()).thenReturn("PENDING");

        assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(order, validPackage())
        );
    }

    @Test
    void rejectsCodPayment() {
        Order order = validOrder();
        when(order.getPaymentMethod()).thenReturn("COD");

        assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(order, validPackage())
        );
    }

    @Test
    void rejectsInvalidPin() {
        Order order = validOrder();
        when(order.getPostalCode()).thenReturn("123");

        assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(order, validPackage())
        );
    }

    @Test
    void rejectsMissingPackage() {
        assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(validOrder(), null)
        );
    }

    @Test
    void rejectsUnpackedOrder() {
        Order order = validOrder();
        when(order.getOrderStatus()).thenReturn("PROCESSING");

        assertThrows(
                ResponseStatusException.class,
                () -> validator.validate(order, validPackage())
        );
    }
}