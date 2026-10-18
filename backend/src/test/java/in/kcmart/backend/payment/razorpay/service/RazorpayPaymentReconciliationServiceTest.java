package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.order.entity.Order;
import in.kcmart.backend.order.repository.OrderRepository;
import in.kcmart.backend.order.repository.OrderStatusHistoryRepository;
import in.kcmart.backend.order.reservation.repository.OrderStockReservationRepository;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayPaymentResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentAttemptRepository;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentReconciliationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentReconciliationServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private RazorpayPaymentAttemptRepository attempts;

    @Mock
    private RazorpayPaymentReconciliationRepository reconciliations;

    @Mock
    private OrderStockReservationRepository reservations;

    @Mock
    private OrderStatusHistoryRepository statusHistory;

    @InjectMocks
    private RazorpayPaymentReconciliationService service;

    private static final Long ORDER_ID = 100L;
    private static final Long ATTEMPT_ID = 50L;
    private static final Long AMOUNT = 49900L;

    private static final String RAZORPAY_ORDER = "order_test_100";
    private static final String RAZORPAY_PAYMENT = "pay_test_100";

    private RazorpayPaymentResponse capturedPayment() {
        return new RazorpayPaymentResponse(
                RAZORPAY_PAYMENT,
                "payment",
                AMOUNT,
                "INR",
                "captured",
                RAZORPAY_ORDER,
                "upi",
                true,
                null,
                null,
                1000L
        );
    }

    private void mockCancelledOrderAndAttempt() {

        Order order = mock(Order.class);

        lenient().when(order.getFulfillmentType()).thenReturn("DELIVERY");
        lenient().when(order.getPaymentMethod()).thenReturn("ONLINE");
        lenient().when(order.getOrderStatus()).thenReturn("CANCELLED");
        lenient().when(order.getPaymentStatus()).thenReturn("FAILED");

        lenient().when(orders.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        RazorpayPaymentAttempt attempt =
                mock(RazorpayPaymentAttempt.class);

        lenient().when(attempt.getId()).thenReturn(ATTEMPT_ID);
        lenient().when(attempt.getRazorpayOrderId())
                .thenReturn(RAZORPAY_ORDER);
        lenient().when(attempt.getAmountPaise()).thenReturn(AMOUNT);
        lenient().when(attempt.getCurrency()).thenReturn("INR");

        lenient().when(attempts.findForFinalization(
                ORDER_ID,
                RAZORPAY_ORDER
        )).thenReturn(Optional.of(attempt));
    }

    @Test
    void frozenOrdersMustNeverBeReconciled() {

        assertThrows(
                IllegalStateException.class,
                () -> service.recordCapturedForCancelledOrder(
                        5L,
                        RAZORPAY_ORDER,
                        capturedPayment()
                )
        );

        verifyNoInteractions(
                orders,
                attempts,
                reconciliations,
                reservations,
                statusHistory
        );
    }

    @Test
    void cancelledOrderCapturedPaymentCreatesReviewRecord() {

        mockCancelledOrderAndAttempt();

        when(reconciliations.findByRazorpayPaymentId(
                RAZORPAY_PAYMENT
        )).thenReturn(Optional.empty());

        when(reconciliations.saveAndFlush(any(
                RazorpayPaymentReconciliation.class
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        RazorpayPaymentReconciliation result =
                service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        capturedPayment()
                );

        assertEquals(ORDER_ID, result.getOrderId());
        assertEquals(ATTEMPT_ID, result.getPaymentAttemptId());
        assertEquals(RAZORPAY_PAYMENT, result.getRazorpayPaymentId());
        assertEquals(AMOUNT, result.getAmountPaise());
        assertEquals("CAPTURED_REVIEW", result.getStatus());

        verify(reconciliations, times(1))
                .saveAndFlush(any(
                        RazorpayPaymentReconciliation.class
                ));

        verifyNoInteractions(reservations, statusHistory);
    }

    @Test
    void duplicateCallbackReturnsExistingRecord() {

        mockCancelledOrderAndAttempt();

        RazorpayPaymentReconciliation existing =
                mock(RazorpayPaymentReconciliation.class);

        when(existing.getOrderId()).thenReturn(ORDER_ID);
        when(existing.getPaymentAttemptId()).thenReturn(ATTEMPT_ID);
        when(existing.getAmountPaise()).thenReturn(AMOUNT);
        when(existing.getRazorpayOrderId())
                .thenReturn(RAZORPAY_ORDER);

        when(reconciliations.findByRazorpayPaymentId(
                RAZORPAY_PAYMENT
        )).thenReturn(Optional.of(existing));

        RazorpayPaymentReconciliation result =
                service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        capturedPayment()
                );

        assertSame(existing, result);

        verify(reconciliations, never())
                .saveAndFlush(any(
                        RazorpayPaymentReconciliation.class
                ));

        verifyNoInteractions(reservations, statusHistory);
    }

    @Test
    void uncapturedPaymentMustBeRejected() {
        RazorpayPaymentResponse payment =
                new RazorpayPaymentResponse(
                        RAZORPAY_PAYMENT,
                        "payment",
                        AMOUNT,
                        "INR",
                        "authorized",
                        RAZORPAY_ORDER,
                        "upi",
                        false,
                        null,
                        null,
                        1000L
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        payment
                )
        );

        verifyNoInteractions(
                orders,
                attempts,
                reconciliations,
                reservations,
                statusHistory
        );
    }

    @Test
    void incorrectPaymentAmountMustBeRejected() {
        mockCancelledOrderAndAttempt();

        RazorpayPaymentResponse payment =
                new RazorpayPaymentResponse(
                        RAZORPAY_PAYMENT,
                        "payment",
                        AMOUNT + 100L,
                        "INR",
                        "captured",
                        RAZORPAY_ORDER,
                        "upi",
                        true,
                        null,
                        null,
                        1000L
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        payment
                )
        );

        verify(reconciliations, never())
                .saveAndFlush(any(
                        RazorpayPaymentReconciliation.class
                ));
    }

    @Test
    void conflictingReconciliationRecordMustBeRejected() {
        mockCancelledOrderAndAttempt();

        RazorpayPaymentReconciliation existing =
                mock(RazorpayPaymentReconciliation.class);

        when(existing.getOrderId()).thenReturn(999L);

        when(reconciliations.findByRazorpayPaymentId(
                RAZORPAY_PAYMENT
        )).thenReturn(Optional.of(existing));

        assertThrows(
                IllegalStateException.class,
                () -> service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        capturedPayment()
                )
        );

        verify(reconciliations, never())
                .saveAndFlush(any(
                        RazorpayPaymentReconciliation.class
                ));
    }

    @Test
    void expiredReservationMustBeReleasedBeforeReconciliation() {
        Order order = mock(Order.class);

        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getOrderStatus())
                .thenReturn("PENDING", "CANCELLED");
        when(order.getPaymentStatus())
                .thenReturn("PENDING", "FAILED");

        when(orders.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        RazorpayPaymentAttempt attempt =
                mock(RazorpayPaymentAttempt.class);

        when(attempt.getId()).thenReturn(ATTEMPT_ID);
        when(attempt.getRazorpayOrderId()).thenReturn(RAZORPAY_ORDER);
        when(attempt.getAmountPaise()).thenReturn(AMOUNT);
        when(attempt.getCurrency()).thenReturn("INR");

        when(attempts.findForFinalization(ORDER_ID, RAZORPAY_ORDER))
                .thenReturn(Optional.of(attempt));

        in.kcmart.backend.order.reservation.OrderStockReservation reservation =
                new in.kcmart.backend.order.reservation.OrderStockReservation();

        reservation.setOrderId(ORDER_ID);
        reservation.setProductId(200L);
        reservation.setQuantity(2);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(
                java.time.OffsetDateTime.now().minusMinutes(10)
        );

        when(reservations.findByOrderIdForUpdate(ORDER_ID))
                .thenReturn(java.util.List.of(reservation));

        when(reconciliations.findByRazorpayPaymentId(RAZORPAY_PAYMENT))
                .thenReturn(Optional.empty());

        when(reconciliations.saveAndFlush(
                any(RazorpayPaymentReconciliation.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        RazorpayPaymentReconciliation result =
                service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        capturedPayment()
                );

        assertEquals("RELEASED", reservation.getStatus());
        assertNotNull(reservation.getReleasedAt());
        assertEquals("CAPTURED_REVIEW", result.getStatus());

        verify(order).setOrderStatus("CANCELLED");
        verify(order).setPaymentStatus("FAILED");
        verify(order, never()).setPaymentStatus("PAID");
        verify(reservations).save(reservation);
        verify(orders).save(order);
        verify(statusHistory).save(any());
    }

    @Test
    void unexpiredReservationMustNotBeCancelled() {
        Order order = mock(Order.class);

        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getFulfillmentType()).thenReturn("DELIVERY");
        when(order.getPaymentMethod()).thenReturn("ONLINE");
        when(order.getOrderStatus()).thenReturn("PENDING");
        when(order.getPaymentStatus()).thenReturn("PENDING");

        when(orders.findByIdForUpdate(ORDER_ID))
                .thenReturn(Optional.of(order));

        RazorpayPaymentAttempt attempt =
                mock(RazorpayPaymentAttempt.class);

        when(attempt.getRazorpayOrderId()).thenReturn(RAZORPAY_ORDER);
        when(attempt.getAmountPaise()).thenReturn(AMOUNT);
        when(attempt.getCurrency()).thenReturn("INR");

        when(attempts.findForFinalization(ORDER_ID, RAZORPAY_ORDER))
                .thenReturn(Optional.of(attempt));

        in.kcmart.backend.order.reservation.OrderStockReservation reservation =
                new in.kcmart.backend.order.reservation.OrderStockReservation();

        reservation.setOrderId(ORDER_ID);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(
                java.time.OffsetDateTime.now().plusMinutes(30)
        );

        when(reservations.findByOrderIdForUpdate(ORDER_ID))
                .thenReturn(java.util.List.of(reservation));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.recordCapturedForCancelledOrder(
                        ORDER_ID,
                        RAZORPAY_ORDER,
                        capturedPayment()
                )
        );

        assertEquals("Reservation has not expired.", error.getMessage());
        assertEquals("ACTIVE", reservation.getStatus());

        verify(order, never()).setOrderStatus(any());
        verify(order, never()).setPaymentStatus(any());
        verify(reservations, never()).save(any());
        verify(reconciliations, never()).saveAndFlush(any());
        verifyNoInteractions(statusHistory);
    }
}
