package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAuditEvent;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAuditEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayRefundRecoveryService {

    private final EntityManager entityManager;
    private final RazorpayRefundAuditEventRepository auditEvents;

    public RazorpayRefundRecoveryService(
            EntityManager entityManager,
            RazorpayRefundAuditEventRepository auditEvents) {

        this.entityManager = entityManager;
        this.auditEvents = auditEvents;
    }

    /*
     * Quarantine an uncertain refund outcome.
     *
     * This method never calls the payment gateway.
     * It never retries a refund.
     *
     * Call only after the responsible operator has determined
     * that the execution outcome is uncertain.
     */
    @Transactional
    public RazorpayRefundAttempt markOutcomeUnknown(
            Long refundAttemptId,
            String operatorIdentifier,
            String reason) {

        if (refundAttemptId == null || refundAttemptId <= 0) {
            throw new IllegalArgumentException(
                    "Valid refund attempt ID required."
            );
        }

        if (operatorIdentifier == null
                || operatorIdentifier.isBlank()
                || operatorIdentifier.trim().length() > 320) {
            throw new IllegalArgumentException(
                    "Valid operator identifier required."
            );
        }

        if (reason == null
                || reason.isBlank()
                || reason.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "A valid uncertainty reason is required."
            );
        }

        RazorpayRefundAttempt attempt = entityManager.find(
                RazorpayRefundAttempt.class,
                refundAttemptId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (attempt == null) {
            throw new IllegalStateException(
                    "Refund attempt not found."
            );
        }

        /*
         * Repeated quarantine is idempotent:
         * no additional audit event is created.
         */
        if ("UNKNOWN".equals(attempt.getStatus())) {
            return attempt;
        }

        if (!"PROCESSING".equals(attempt.getStatus())) {
            throw new IllegalStateException(
                    "Only PROCESSING refunds can be quarantined."
            );
        }

        RazorpayPaymentReconciliation reconciliation =
                entityManager.find(
                        RazorpayPaymentReconciliation.class,
                        attempt.getReconciliationId(),
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (reconciliation == null) {
            throw new IllegalStateException(
                    "Refund reconciliation not found."
            );
        }

        if (!"REFUND_IN_PROGRESS".equals(reconciliation.getStatus())) {
            throw new IllegalStateException(
                    "Reconciliation is not in refund processing."
            );
        }

        attempt.setStatus("UNKNOWN");
        reconciliation.setStatus("MANUAL_REVIEW");

        entityManager.flush();

        RazorpayRefundAuditEvent event =
                new RazorpayRefundAuditEvent();

        event.setRefundAttemptId(attempt.getId());
        event.setEventType("REFUND_OUTCOME_UNKNOWN");
        event.setPreviousStatus("PROCESSING");
        event.setNewStatus("UNKNOWN");
        event.setActorType("ADMIN");
        event.setActorIdentifier(operatorIdentifier.trim());
        event.setEventNote(reason.trim());

        auditEvents.saveAndFlush(event);

        return attempt;
    }
}