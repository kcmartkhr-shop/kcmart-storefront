package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryPackageServiceTest {

    private OrderRepository orders;
    private OrderShipmentRepository shipments;
    private DelhiveryShipmentPackageRepository packages;
    private DelhiveryShipmentBookingAttemptRepository attempts;
    private DelhiveryPackageService service;

    private final BigDecimal divisor = new BigDecimal("5000");

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        shipments = mock(OrderShipmentRepository.class);
        packages = mock(DelhiveryShipmentPackageRepository.class);
        attempts = mock(DelhiveryShipmentBookingAttemptRepository.class);

        service = new DelhiveryPackageService(
            orders,
            shipments,
            packages,
            attempts,
            new DelhiveryPackageWeightCalculator()
        );
    }

    private Order order(String fulfillment, String status) {
        Order order = new Order();
        order.setFulfillmentType(fulfillment);
        order.setOrderStatus(status);
        return order;
    }

    private DelhiveryShipmentPackage create(Long orderId) {
        return service.create(
            orderId,
            1200,
            new BigDecimal("30"),
            new BigDecimal("20"),
            new BigDecimal("15"),
            divisor
        );
    }

    @Test
    void rejectsInvalidOrderId() {
        assertThrows(ResponseStatusException.class, () -> create(0L));
        verifyNoInteractions(orders);
    }

    @Test
    void rejectsMissingOrder() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStorePickupOrder() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("PICKUP", "PACKED")));

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOrderNotPacked() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PLACED")));

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void rejectsExistingShipment() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));
        var existingShipment = new in.kcmart.backend.order.entity.OrderShipment();
        existingShipment.setOrderId(5L);
        existingShipment.setShipmentStatus("PACKED");
        existingShipment.setCarrierName("MANUAL_CARRIER");
        existingShipment.setTrackingNumber("MANUAL-TRACKING-001");

        when(shipments.findByOrderIdForUpdate(5L))
            .thenReturn(java.util.Optional.of(existingShipment));

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void rejectsExistingBookingAttempt() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));
        when(attempts.existsByOrderId(5L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicatePackage() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));
        when(packages.existsByOrderId(5L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> create(5L));

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void savesCalculatedPackage() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));

        when(packages.saveAndFlush(any(DelhiveryShipmentPackage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DelhiveryShipmentPackage result = create(5L);

        assertEquals(5L, result.getOrderId());
        assertEquals(1200, result.getActualWeightGrams());
        assertEquals(1800, result.getVolumetricWeightGrams());
        assertEquals(1800, result.getChargeableWeightGrams());

        verify(packages).saveAndFlush(any(DelhiveryShipmentPackage.class));
    }

    @Test
    void rejectsInvalidDimensionsWithoutSaving() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));

        assertThrows(ResponseStatusException.class, () ->
            service.create(
                5L,
                1200,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.ONE,
                divisor
            )
        );

        verify(packages, never()).saveAndFlush(any());
    }

    @Test
    void readsExistingPackage() {
        DelhiveryShipmentPackage saved =
            new DelhiveryShipmentPackage(
                5L,
                1200,
                new BigDecimal("30"),
                new BigDecimal("20"),
                new BigDecimal("15"),
                1800,
                1800
            );

        when(packages.findByOrderId(5L))
            .thenReturn(Optional.of(saved));

        assertSame(saved, service.getByOrderId(5L));
    }

    @Test
    void rejectsMissingPackageOnRead() {
        when(packages.findByOrderId(5L))
            .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
            service.getByOrderId(5L)
        );
    }

    @Test
    void allowsEmptyPackedShipmentShellForPackagePreparation() {
        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order("DELIVERY", "PACKED")));

        var shell = new in.kcmart.backend.order.entity.OrderShipment();
        shell.setOrderId(5L);
        shell.setShipmentStatus("PACKED");

        when(shipments.existsByOrderId(5L)).thenReturn(true);

        when(shipments.findByOrderIdForUpdate(5L))
            .thenReturn(Optional.of(shell));

        when(packages.saveAndFlush(any(DelhiveryShipmentPackage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DelhiveryShipmentPackage result = create(5L);

        assertEquals(5L, result.getOrderId());
        verify(packages).saveAndFlush(any(DelhiveryShipmentPackage.class));
    }
}