package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.config.DelhiveryPackageConfiguration;
import in.kcmart.backend.shipping.delhivery.entity.DelhiveryShipmentPackage;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentPackageRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryPackageConfigurationInjectionTest {

    @Test
    void usesConfiguredDivisorInsteadOfHardcodedDefault() {

        OrderRepository orders = mock(OrderRepository.class);
        OrderShipmentRepository shipments =
            mock(OrderShipmentRepository.class);
        DelhiveryShipmentPackageRepository packages =
            mock(DelhiveryShipmentPackageRepository.class);
        DelhiveryShipmentBookingAttemptRepository attempts =
            mock(DelhiveryShipmentBookingAttemptRepository.class);

        Order order = new Order();
        order.setFulfillmentType("DELIVERY");
        order.setOrderStatus("PACKED");

        when(orders.findByIdForUpdate(5L))
            .thenReturn(Optional.of(order));

        when(packages.saveAndFlush(any(DelhiveryShipmentPackage.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        DelhiveryPackageService service = new DelhiveryPackageService(
            orders,
            shipments,
            packages,
            attempts,
            new DelhiveryPackageWeightCalculator(),
            new DelhiveryPackageConfiguration(new BigDecimal("4000"))
        );

        DelhiveryShipmentPackage result = service.create(
            5L,
            1200,
            new BigDecimal("30"),
            new BigDecimal("20"),
            new BigDecimal("15")
        );

        assertEquals(2250, result.getVolumetricWeightGrams());
        assertEquals(2250, result.getChargeableWeightGrams());

        verify(packages).saveAndFlush(any(DelhiveryShipmentPackage.class));
    }

    @Test
    void rejectsInvalidConfiguredDivisor() {
        assertThrows(IllegalArgumentException.class, () ->
            new DelhiveryPackageConfiguration(BigDecimal.ZERO)
        );
    }
}