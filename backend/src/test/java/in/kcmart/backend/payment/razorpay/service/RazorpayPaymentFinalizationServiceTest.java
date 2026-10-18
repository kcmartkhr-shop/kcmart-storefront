package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.service.OrderPaymentService;
import in.kcmart.backend.order.reservation.service.OrderStockReservationService;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentFinalizationServiceTest {

    private static final Long ORDER_ID = 100L;
    private static final String GATEWAY_ORDER = "order_test_100";
    private static final String GATEWAY_PAYMENT = "pay_test_100";
    private static final long AMOUNT = 49900L;

    @Mock
    private OrderRepository orders;

    @Mock
    private RazorpayPaymentAttemptRepository attempts;

    @Mock
    private OrderPaymentService paymentService;

    @Mock
    private OrderStockReservationService reservations;

    @InjectMocks
    private RazorpayPaymentFinalizationService service;

    private Order mockOrder(String orderStatus, String paymentStatus) {
        Order order = mock(Order.class);
        lenient().when(order.getId()).thenReturn(ORDER_ID);
        lenient().when(order.getPaymentMethod()).thenReturn("ONLINE");
        lenient().when(order.getFulfillmentType()).thenReturn("DELIVERY");
        lenient().when(order.getOrderStatus()).thenReturn(orderStatus);
        lenient().when(order.getPaymentStatus()).thenReturn(paymentStatus);

        when(orders.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        return order;
    }

    private RazorpayPaymentAttempt mockAttempt(String status) {
        RazorpayPaymentAttempt attempt =
                mock(RazorpayPaymentAttempt.class);

        lenient().when(attempt.getStatus()).thenReturn(status);
        lenient().when(attempt.getAmountPaise()).thenReturn(AMOUNT);
        lenient().when(attempt.getCurrency()).thenReturn("INR");
        lenient().when(attempt.getRazorpayOrderId())
                .thenReturn(GATEWAY_ORDER);

        when(attempts.findForFinalization(
                ORDER_ID, GATEWAY_ORDER
        )).thenReturn(Optional.of(attempt));

        return attempt;
    }

    private RazorpayPaymentFinalizationService.FinalizationResult finalizePayment() {
        return service.finalizeCapturedPayment(
                ORDER_ID,
                GATEWAY_ORDER,
                GATEWAY_PAYMENT,
                AMOUNT,
                "INR",
                true
        );
    }

    @Test
    void successfulPaymentConsumesStockAndMarksPaid() {
        mockOrder("PENDING", "PENDING");
        RazorpayPaymentAttempt attempt = mockAttempt("CREATED");

        var result = finalizePayment();

        assertFalse(result.alreadyFinalized());

        var sequence = inOrder(reservations, attempts, paymentService);

        sequence.verify(reservations).consumeForPaidOrder(ORDER_ID);
        sequence.verify(attempts).save(attempt);
        sequence.verify(paymentService).markOnlinePaymentReceived(
                ORDER_ID, GATEWAY_PAYMENT
        );

        verify(attempt).setStatus("CAPTURED");
        verify(attempt).setRazorpayPaymentId(GATEWAY_PAYMENT);
    }

    @Test
    void duplicateCapturedPaymentIsIdempotent() {
        Order order = mockOrder("PENDING", "PAID");
        RazorpayPaymentAttempt attempt = mockAttempt("CAPTURED");

        when(attempt.getRazorpayPaymentId())
                .thenReturn(GATEWAY_PAYMENT);
        when(attempt.isSignatureVerified()).thenReturn(true);

        var result = finalizePayment();

        assertTrue(result.alreadyFinalized());
        assertEquals(ORDER_ID, result.orderId());

        verifyNoInteractions(reservations, paymentService);
        verify(attempts, never()).save(any());
    }

    @Test
    void browserCallbackUpgradesSignatureWithoutDuplicatePayment() {
        mockOrder("PENDING", "PAID");
        RazorpayPaymentAttempt attempt = mockAttempt("CAPTURED");

        when(attempt.getRazorpayPaymentId())
                .thenReturn(GATEWAY_PAYMENT);
        when(attempt.isSignatureVerified()).thenReturn(false);

        var result = finalizePayment();

        assertTrue(result.alreadyFinalized());

        verify(attempt).setSignatureVerified(true);
        verify(attempts).save(attempt);

        verifyNoInteractions(reservations, paymentService);
        verify(attempt, never()).setStatus("CAPTURED");
        verify(attempt, never()).setPaidAt(any());
    }
    @Test
    void conflictingPaymentIdIsRejected() {
        mockOrder("PENDING", "PAID");
        RazorpayPaymentAttempt attempt = mockAttempt("CAPTURED");

        when(attempt.getRazorpayPaymentId())
                .thenReturn("pay_other");

        assertThrows(
                ResponseStatusException.class,
                this::finalizePayment
        );

        verifyNoInteractions(reservations, paymentService);
        verify(attempts, never()).save(any());
    }

    @Test
    void cancelledOrderCannotBeFinalized() {
        mockOrder("CANCELLED", "FAILED");
        mockAttempt("CREATED");

        assertThrows(
                ResponseStatusException.class,
                this::finalizePayment
        );

        verifyNoInteractions(reservations, paymentService);
        verify(attempts, never()).save(any());
    }

    @Test
    void reservationFailureMustPreventPaymentUpdates() {
        mockOrder("PENDING", "PENDING");
        RazorpayPaymentAttempt attempt = mockAttempt("CREATED");

        doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Stock reservation has expired"
        )).when(reservations).consumeForPaidOrder(ORDER_ID);

        assertThrows(
                ResponseStatusException.class,
                this::finalizePayment
        );

        verify(attempt, never()).setStatus("CAPTURED");
        verify(attempts, never()).save(any());
        verifyNoInteractions(paymentService);
    }
}