package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.client.RazorpayClient;
import in.kcmart.backend.payment.razorpay.client.RazorpayClient.RazorpayRefundResponse;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAttemptRepository;

import org.springframework.stereotype.Service;

@Service
public class RazorpayRefundEvidenceVerificationService {

    private final RazorpayRefundAttemptRepository attempts;
    private final RazorpayClient razorpayClient;
    private final RazorpayRefundOutcomeService outcomeService;

    public RazorpayRefundEvidenceVerificationService(
            RazorpayRefundAttemptRepository attempts,
            RazorpayClient razorpayClient,
            RazorpayRefundOutcomeService outcomeService) {

        this.attempts = attempts;
        this.razorpayClient = razorpayClient;
        this.outcomeService = outcomeService;
    }

    /*
     * INTERNAL USE ONLY.
     *
     * No refund creation or refund retry occurs here.
     * No external controller should expose this operation.
     *
     * Network verification happens before the outcome transaction.
     * The outcome service rechecks persisted state under row locks.
     */
    public RazorpayRefundAttempt verifyAndRecordSuccess(Long attemptId) {

        if (attemptId == null || attemptId <= 0) {
            throw new IllegalArgumentException(
                    "Valid refund attempt ID required."
            );
        }

        RazorpayRefundAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new IllegalStateException(
                        "Refund attempt not found."
                ));

        if (!"PROCESSING".equals(attempt.getStatus())
                && !"SUCCEEDED".equals(attempt.getStatus())) {

            throw new IllegalStateException(
                    "Refund attempt is not eligible for verification."
            );
        }

        if (attempt.getGatewayRequestSentAt() == null) {
            throw new IllegalStateException(
                    "Durable gateway dispatch intent is missing."
            );
        }

        String refundId = attempt.getRazorpayRefundId();

        if (refundId == null || refundId.isBlank()) {
            throw new IllegalStateException(
                    "Stored Razorpay refund ID is required."
            );
        }

        /*
         * Authenticated GET only.
         * Never create a refund or infer success from a timeout.
         */
        RazorpayRefundResponse evidence =
                razorpayClient.fetchRefund(refundId);

        if (!"processed".equals(evidence.status())) {
            throw new IllegalStateException(
                    "Gateway refund is not processed."
            );
        }

        if (!refundId.equals(evidence.id())
                || !attempt.getRazorpayPaymentId().equals(evidence.payment_id())
                || !attempt.getAmountPaise().equals(evidence.amount())
                || !attempt.getCurrency().equals(evidence.currency())
                || !"INR".equals(evidence.currency())) {

            throw new IllegalStateException(
                    "Gateway refund evidence does not match stored attempt."
            );
        }

        /*
         * The outcome service obtains database write locks and
         * revalidates attempt and reconciliation state.
         */
        return outcomeService.recordVerifiedSuccess(
                attemptId,
                evidence.id(),
                evidence.payment_id(),
                evidence.amount(),
                evidence.currency(),
                "RAZORPAY_FETCH:" + evidence.id()
        );
    }
}