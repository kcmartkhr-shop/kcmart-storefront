package in.kcmart.backend.order.reservation.service;

import in.kcmart.backend.catalog.entity.ProductInventory;
import in.kcmart.backend.catalog.repository.ProductInventoryRepository;
import in.kcmart.backend.order.reservation.OrderStockReservation;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderStockReservationServiceTest {

    private ProductInventoryRepository inventoryRepository;
    private OrderStockReservationRepository reservationRepository;
    private OrderStockReservationService service;

    @BeforeEach
    void setup() {
        inventoryRepository = mock(ProductInventoryRepository.class);
        reservationRepository = mock(OrderStockReservationRepository.class);

        service = new OrderStockReservationService(
            inventoryRepository,
            reservationRepository
        );
    }

    @Test
    void insufficientOnlineStockRejectsReservation() {
        ProductInventory inventory = mock(ProductInventory.class);

        when(inventory.getOnlineStock()).thenReturn(2);

        when(inventoryRepository.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(inventory));

        when(reservationRepository.sumActiveQuantity(
            eq(10L), any(OffsetDateTime.class)
        )).thenReturn(1);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.reserveDeliveryStock(100L, 10L, 2)
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(reservationRepository, never())
            .save(any(OrderStockReservation.class));
    }

    @Test
    void duplicateOrderProductReservationIsRejected() {
        ProductInventory inventory = mock(ProductInventory.class);

        when(inventory.getOnlineStock()).thenReturn(10);

        when(inventoryRepository.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(inventory));

        when(reservationRepository.sumActiveQuantity(
            eq(10L), any(OffsetDateTime.class)
        )).thenReturn(0);

        when(reservationRepository.findByOrderIdAndProductId(100L, 10L))
            .thenReturn(Optional.of(new OrderStockReservation()));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.reserveDeliveryStock(100L, 10L, 1)
        );

        assertEquals(409, exception.getStatusCode().value());

        verify(reservationRepository, never())
            .save(any(OrderStockReservation.class));
    }

    @Test
    void validReservationSavesCorrectQuantityAndExpiry() {
        ProductInventory inventory = mock(ProductInventory.class);

        when(inventory.getOnlineStock()).thenReturn(10);

        when(inventoryRepository.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(inventory));

        when(reservationRepository.sumActiveQuantity(
            eq(10L), any(OffsetDateTime.class)
        )).thenReturn(2);

        when(reservationRepository.findByOrderIdAndProductId(100L, 10L))
            .thenReturn(Optional.empty());

        when(reservationRepository.save(any(OrderStockReservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime before = OffsetDateTime.now();

        OrderStockReservation reservation =
            service.reserveDeliveryStock(100L, 10L, 3);

        OffsetDateTime after = OffsetDateTime.now();

        assertEquals(100L, reservation.getOrderId());
        assertEquals(10L, reservation.getProductId());
        assertEquals(3, reservation.getQuantity());
        assertEquals("ACTIVE", reservation.getStatus());

        assertFalse(reservation.getExpiresAt().isBefore(
            before.plusMinutes(5)
        ));

        assertFalse(reservation.getExpiresAt().isAfter(
            after.plusMinutes(5)
        ));

        verify(reservationRepository)
            .save(any(OrderStockReservation.class));
    }

    @Test
    void successfulConsumptionDeductsStockOnce() {
        OrderStockReservation reservation = activeReservation();

        ProductInventory inventory = mock(ProductInventory.class);
        when(inventory.getOnlineStock()).thenReturn(10);

        when(reservationRepository.findByOrderIdForUpdate(100L))
            .thenReturn(java.util.List.of(reservation));

        when(inventoryRepository.findByProductIdForUpdate(10L))
            .thenReturn(Optional.of(inventory));

        service.consumeForPaidOrder(100L);

        verify(inventory).setOnlineStock(7);
        assertEquals("CONSUMED", reservation.getStatus());
        assertNotNull(reservation.getConsumedAt());

        verify(inventoryRepository, times(1)).save(inventory);
        verify(reservationRepository, times(1)).save(reservation);
    }

    @Test
    void alreadyConsumedReservationDoesNotDeductAgain() {
        OrderStockReservation reservation = activeReservation();
        reservation.setStatus("CONSUMED");

        when(reservationRepository.findByOrderIdForUpdate(100L))
            .thenReturn(java.util.List.of(reservation));

        service.consumeForPaidOrder(100L);

        verifyNoInteractions(inventoryRepository);
        verify(reservationRepository, never())
            .save(any(OrderStockReservation.class));
    }

    @Test
    void expiredReservationRejectsConsumption() {
        OrderStockReservation reservation = activeReservation();
        reservation.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(reservationRepository.findByOrderIdForUpdate(100L))
            .thenReturn(java.util.List.of(reservation));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.consumeForPaidOrder(100L)
        );

        assertEquals(409, exception.getStatusCode().value());

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void missingReservationRejectsConsumption() {
        when(reservationRepository.findByOrderIdForUpdate(100L))
            .thenReturn(java.util.List.of());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.consumeForPaidOrder(100L)
        );

        assertEquals(409, exception.getStatusCode().value());

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void expiryCleanupReleasesReservationsWithoutStockDeduction() {
        OrderStockReservation reservation = activeReservation();
        reservation.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(reservationRepository.findExpiredActiveForUpdate(
            any(OffsetDateTime.class)
        )).thenReturn(java.util.List.of(reservation));

        int released = service.releaseExpiredReservations();

        assertEquals(1, released);
        assertEquals("RELEASED", reservation.getStatus());
        assertNotNull(reservation.getReleasedAt());

        verify(reservationRepository).save(reservation);
        verifyNoInteractions(inventoryRepository);
    }

    private OrderStockReservation activeReservation() {
        OrderStockReservation reservation = new OrderStockReservation();

        reservation.setOrderId(100L);
        reservation.setProductId(10L);
        reservation.setQuantity(3);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(OffsetDateTime.now().plusMinutes(5));

        return reservation;
    }
}
