package in.kcmart.backend.order.service;

import in.kcmart.backend.order.dto.admin.AdminOrderShipmentUpsertRequest;
import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderShipmentRepository;
import in.kcmart.backend.shipping.delhivery.repository.DelhiveryShipmentBookingAttemptRepository;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderShipmentServiceTest {

    @Test
    void rejectsManualShipmentUpdateWhenDelhiveryBookingAttemptExists() {

        OrderRepository orders = mock(OrderRepository.class);
        OrderShipmentRepository shipments = mock(OrderShipmentRepository.class);

        DelhiveryShipmentBookingAttemptRepository bookingAttempts =
            mock(DelhiveryShipmentBookingAttemptRepository.class);

        OrderShipmentService service = new OrderShipmentService(
            orders,
            shipments,
            bookingAttempts
        );

        Long orderId = 123L;

        Order order = new Order();
        order.setFulfillmentType("DELIVERY");
        order.setOrderStatus("PACKED");

        when(orders.findByIdForUpdate(orderId))
            .thenReturn(Optional.of(order));

        when(bookingAttempts.existsByOrderId(orderId))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.upsert(
                orderId,
                new AdminOrderShipmentUpsertRequest(
                    "TEST_CARRIER",
                    "TEST-TRACKING-123",
                    null,
                    null
                ),
                "admin"
            )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(bookingAttempts).existsByOrderId(orderId);
        verifyNoInteractions(shipments);
    }
}