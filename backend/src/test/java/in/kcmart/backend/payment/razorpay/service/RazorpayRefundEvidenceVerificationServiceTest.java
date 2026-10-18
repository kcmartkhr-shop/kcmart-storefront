package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.client.RazorpayClient;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayRefundResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAttemptRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RazorpayRefundEvidenceVerificationServiceTest {

    private RazorpayRefundAttemptRepository attempts;
    private RazorpayClient client;
    private RazorpayRefundOutcomeService outcomes;
    private RazorpayRefundEvidenceVerificationService verifier;

    @BeforeEach
    void setUp() {
        attempts = mock(RazorpayRefundAttemptRepository.class);
        client = mock(RazorpayClient.class);
        outcomes = mock(RazorpayRefundOutcomeService.class);

        verifier = new RazorpayRefundEvidenceVerificationService(
                attempts, client, outcomes
        );
    }

    private RazorpayRefundAttempt validAttempt() {
        RazorpayRefundAttempt attempt = new RazorpayRefundAttempt();
        attempt.setStatus("PROCESSING");
        attempt.setRazorpayRefundId("rfnd_test123");
        attempt.setRazorpayPaymentId("pay_test123");
        attempt.setAmountPaise(50000L);
        attempt.setCurrency("INR");

        try {
            var field = RazorpayRefundAttempt.class
                    .getDeclaredField("gatewayRequestSentAt");
            field.setAccessible(true);
            field.set(attempt, OffsetDateTime.now());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        return attempt;
    }

    private RazorpayRefundResponse response(
            String status,
            String paymentId,
            Long amount) {

        return new RazorpayRefundResponse(
                "rfnd_test123",
                "refund",
                paymentId,
                amount,
                "INR",
                status,
                1700000000L
        );
    }

    private void arrange(RazorpayRefundAttempt attempt,
                         RazorpayRefundResponse evidence) {
        when(attempts.findById(42L))
                .thenReturn(Optional.of(attempt));

        if (evidence != null) {
            when(client.fetchRefund("rfnd_test123"))
                    .thenReturn(evidence);
        }
    }

    @Test
    void processedRefundCanReachOutcomeRecorder() {
        RazorpayRefundAttempt attempt = validAttempt();
        arrange(attempt, response("processed", "pay_test123", 50000L));

        when(outcomes.recordVerifiedSuccess(
                eq(42L),
                eq("rfnd_test123"),
                eq("pay_test123"),
                eq(50000L),
                eq("INR"),
                eq("RAZORPAY_FETCH:rfnd_test123")
        )).thenReturn(attempt);

        assertSame(attempt, verifier.verifyAndRecordSuccess(42L));

        verify(client, times(1)).fetchRefund("rfnd_test123");
        verify(outcomes, times(1)).recordVerifiedSuccess(
                eq(42L),
                eq("rfnd_test123"),
                eq("pay_test123"),
                eq(50000L),
                eq("INR"),
                eq("RAZORPAY_FETCH:rfnd_test123")
        );
    }

    @Test
    void pendingRefundCannotComplete() {
        arrange(validAttempt(),
                response("pending", "pay_test123", 50000L));

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(outcomes);
    }

    @Test
    void failedRefundCannotComplete() {
        arrange(validAttempt(),
                response("failed", "pay_test123", 50000L));

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(outcomes);
    }

    @Test
    void wrongPaymentIdCannotComplete() {
        arrange(validAttempt(),
                response("processed", "pay_wrong", 50000L));

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(outcomes);
    }

    @Test
    void wrongAmountCannotComplete() {
        arrange(validAttempt(),
                response("processed", "pay_test123", 40000L));

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(outcomes);
    }

    @Test
    void missingRefundIdCannotCallGateway() {
        RazorpayRefundAttempt attempt = validAttempt();
        attempt.setRazorpayRefundId(null);

        arrange(attempt, null);

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(client, outcomes);
    }

    @Test
    void missingDispatchIntentCannotCallGateway() {
        RazorpayRefundAttempt attempt = validAttempt();

        try {
            var field = RazorpayRefundAttempt.class
                    .getDeclaredField("gatewayRequestSentAt");
            field.setAccessible(true);
            field.set(attempt, null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        arrange(attempt, null);

        assertThrows(IllegalStateException.class,
                () -> verifier.verifyAndRecordSuccess(42L));

        verifyNoInteractions(client, outcomes);
    }
}