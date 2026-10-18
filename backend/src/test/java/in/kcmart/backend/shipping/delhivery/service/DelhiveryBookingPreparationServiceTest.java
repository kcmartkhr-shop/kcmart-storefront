package in.kcmart.backend.shipping.delhivery.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelhiveryBookingPreparationServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final DelhiveryBookingPreflightService preflight =
        mock(DelhiveryBookingPreflightService.class);
    private final DelhiveryBookingClaimService claims =
        mock(DelhiveryBookingClaimService.class);

    private final DelhiveryBookingPreparationService service =
        new DelhiveryBookingPreparationService(
            orders, preflight, claims
        );

    @Test
    void invalidOrderIdCannotClaim() {
        assertThrows(
            IllegalArgumentException.class,
            () -> service.prepare(0L)
        );

        verifyNoInteractions(orders, preflight, claims);
    }

    @Test
    void missingOrderCannotClaim() {
        when(orders.findById(123L))
            .thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> service.prepare(123L)
        );

        verifyNoInteractions(preflight, claims);
    }

    @Test
    void failedServiceabilityCannotClaim() {
        Order order = mock(Order.class);
        when(order.getPostalCode()).thenReturn("194103");
        when(orders.findById(123L))
            .thenReturn(Optional.of(order));

        doThrow(new IllegalStateException("NSZ"))
            .when(preflight)
            .verifyServiceability("194103");

        assertThrows(
            IllegalStateException.class,
            () -> service.prepare(123L)
        );

        verify(claims, never()).claim(anyLong());
    }

    @Test
    void verifiedPreflightAllowsSingleClaim() {
        Order order = mock(Order.class);
        when(order.getPostalCode()).thenReturn("194103");
        when(orders.findById(123L))
            .thenReturn(Optional.of(order));

        when(claims.claim(123L)).thenReturn(77L);

        assertEquals(77L, service.prepare(123L));

        var orderOfCalls = inOrder(preflight, claims);
        orderOfCalls.verify(preflight)
            .verifyServiceability("194103");
        orderOfCalls.verify(claims).claim(123L);

        verify(claims, times(1)).claim(123L);
    }
}