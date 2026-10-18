package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAuditEvent;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAuditEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RazorpayRefundOutcomeService {

    private final EntityManager entityManager;
    private final RazorpayRefundAuditEventRepository auditEvents;

    public RazorpayRefundOutcomeService(
            EntityManager entityManager,
            RazorpayRefundAuditEventRepository auditEvents) {

        this.entityManager = entityManager;
        this.auditEvents = auditEvents;
    }

    /*
     * INTERNAL RECORDING ONLY.
     *
     * This service does not contact Razorpay.
     * It does not authenticate or fetch gateway evidence.
     *
     * A trusted caller must independently verify that Razorpay
     * confirms this exact refund as successfully processed.
     *
     * Never expose this method directly to customer input.
     */
    @Transactional
    public RazorpayRefundAttempt recordVerifiedSuccess(
            Long refundAttemptId,
            String razorpayRefundId,
            String razorpayPaymentId,
            Long amountPaise,
            String currency,
            String evidenceReference) {

        if (refundAttemptId == null || refundAttemptId <= 0) {
            throw new IllegalArgumentException("Valid refund attempt ID required.");
        }

        String refundId = requireText(razorpayRefundId, 100, "Refund ID");
        String paymentId = requireText(razorpayPaymentId, 100, "Payment ID");
        String verifiedCurrency = requireText(currency, 10, "Currency");
        String evidence = requireText(evidenceReference, 320, "Evidence reference");

        if (amountPaise == null || amountPaise <= 0) {
            throw new IllegalArgumentException("Positive refund amount required.");
        }

        if (!"INR".equals(verifiedCurrency)) {
            throw new IllegalArgumentException("Only INR refunds are supported.");
        }

        RazorpayRefundAttempt attempt = entityManager.find(
                RazorpayRefundAttempt.class,
                refundAttemptId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (attempt == null) {
            throw new IllegalStateException("Refund attempt not found.");
        }

        RazorpayPaymentReconciliation reconciliation = entityManager.find(
                RazorpayPaymentReconciliation.class,
                attempt.getReconciliationId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (reconciliation == null) {
            throw new IllegalStateException("Refund reconciliation not found.");
        }

        if (!paymentId.equals(attempt.getRazorpayPaymentId())
                || !paymentId.equals(reconciliation.getRazorpayPaymentId())
                || !amountPaise.equals(attempt.getAmountPaise())
                || !amountPaise.equals(reconciliation.getAmountPaise())
                || !verifiedCurrency.equals(attempt.getCurrency())
                || !verifiedCurrency.equals(reconciliation.getCurrency())) {

            throw new IllegalStateException("Verified refund details mismatch.");
        }

        if ("SUCCEEDED".equals(attempt.getStatus())) {

            if (!"REFUNDED".equals(reconciliation.getStatus())
                    || !refundId.equals(attempt.getRazorpayRefundId())
                    || !refundId.equals(reconciliation.getRazorpayRefundId())
                    || attempt.getCompletedAt() == null
                    || reconciliation.getRefundedAt() == null) {

                throw new IllegalStateException(
                        "Existing refund completion conflicts with supplied evidence."
                );
            }

            return attempt;
        }

        if (!"PROCESSING".equals(attempt.getStatus())
                || !"REFUND_IN_PROGRESS".equals(reconciliation.getStatus())) {

            throw new IllegalStateException(
                    "Refund is not eligible for successful completion."
            );
        }

        if (attempt.getGatewayRequestSentAt() == null) {
            throw new IllegalStateException(
                    "Durable gateway dispatch intent is missing."
            );
        }

        if (attempt.getRazorpayRefundId() != null
                && !refundId.equals(attempt.getRazorpayRefundId())) {

            throw new IllegalStateException("Conflicting gateway refund ID.");
        }

        if (reconciliation.getRazorpayRefundId() != null
                && !refundId.equals(reconciliation.getRazorpayRefundId())) {

            throw new IllegalStateException(
                    "Conflicting reconciliation refund ID."
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        attempt.setRazorpayRefundId(refundId);
        attempt.setStatus("SUCCEEDED");
        attempt.setCompletedAt(now);
        attempt.setGatewayResponseReceivedAt(now);

        reconciliation.setRazorpayRefundId(refundId);
        reconciliation.setStatus("REFUNDED");
        reconciliation.setRefundedAt(now);

        entityManager.flush();

        RazorpayRefundAuditEvent event = new RazorpayRefundAuditEvent();

        event.setRefundAttemptId(attempt.getId());
        event.setEventType("REFUND_VERIFIED_SUCCEEDED");
        event.setPreviousStatus("PROCESSING");
        event.setNewStatus("SUCCEEDED");
        event.setActorType("GATEWAY");
        event.setActorIdentifier(evidence);
        event.setEventNote(
                "Successful refund recorded from independently verified evidence."
        );

        auditEvents.saveAndFlush(event);

        return attempt;
    }

    private String requireText(
            String value,
            int maxLength,
            String fieldName) {

        if (value == null
                || value.isBlank()
                || !value.equals(value.trim())
                || value.length() > maxLength) {

            throw new IllegalArgumentException(
                    fieldName + " is invalid."
            );
        }

        return value;
    }
}