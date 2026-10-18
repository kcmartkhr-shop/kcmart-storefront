package in.kcmart.backend.order.reservation.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.entity.OrderStatusHistory;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.OrderStockReservation;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryReservationExpiryProcessorTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private OrderStockReservationRepository reservations;

    @Mock
    private OrderStatusHistoryRepository statusHistory;

    @InjectMocks
    private DeliveryReservationExpiryProcessor processor;

    private Order pendingDeliveryOrder() {
        Order order = mock(Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getPaymentStatus()).thenReturn("PENDING");
        when(order.getOrderStatus()).thenReturn("PENDING");

        return order;
    }

    private OrderStockReservation reservation(
            String status,
            OffsetDateTime expiresAt) {

        OrderStockReservation reservation =
                new OrderStockReservation();

        reservation.setOrderId(100L);
        reservation.setProductId(200L);
        reservation.setQuantity(2);
        reservation.setStatus(status);
        reservation.setExpiresAt(expiresAt);

        return reservation;
    }

    @Test
    void historicalOrdersMustNotBeModified() {
        processor.expireOneOrder(5L);

        verifyNoInteractions(orders, reservations, statusHistory);
    }

    @Test
    void expiredOrderMustBeCancelledAndReservationsReleased() {
        Order order = pendingDeliveryOrder();

        when(orders.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        OrderStockReservation expired = reservation(
                "ACTIVE",
                OffsetDateTime.now().minusMinutes(10)
        );

        OrderStockReservation active = reservation(
                "ACTIVE",
                OffsetDateTime.now().plusMinutes(10)
        );

        when(reservations.findByOrderIdForUpdate(100L))
                .thenReturn(List.of(expired, active));

        processor.expireOneOrder(100L);

        assertEquals("RELEASED", expired.getStatus());
        assertEquals("RELEASED", active.getStatus());

        assertNotNull(expired.getReleasedAt());
        assertNotNull(active.getReleasedAt());

        verify(order).setOrderStatus("CANCELLED");
        verify(order).setPaymentStatus("FAILED");

        verify(orders).save(order);
        verify(reservations).save(expired);
        verify(reservations).save(active);
        verify(statusHistory).save(any(OrderStatusHistory.class));
    }

    @Test
    void unexpiredReservationMustNotCancelOrder() {
        Order order = pendingDeliveryOrder();

        when(orders.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        OrderStockReservation active = reservation(
                "ACTIVE",
                OffsetDateTime.now().plusMinutes(10)
        );

        when(reservations.findByOrderIdForUpdate(100L))
                .thenReturn(List.of(active));

        processor.expireOneOrder(100L);

        assertEquals("ACTIVE", active.getStatus());

        verify(order, never()).setOrderStatus(any());
        verify(order, never()).setPaymentStatus(any());

        verify(orders, never()).save(any());
        verify(reservations, never()).save(any());
        verifyNoInteractions(statusHistory);
    }

    @Test
    void paidOrderMustNotBeCancelled() {
        Order order = mock(Order.class);

        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getPaymentStatus()).thenReturn("PAID");

        when(orders.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));

        processor.expireOneOrder(100L);

        verifyNoInteractions(reservations, statusHistory);
        verify(orders, never()).save(any());
    }
}