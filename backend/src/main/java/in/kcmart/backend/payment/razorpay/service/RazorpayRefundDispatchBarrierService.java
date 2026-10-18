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
public class RazorpayRefundDispatchBarrierService {

    private final EntityManager entityManager;
    private final RazorpayRefundAuditEventRepository auditEvents;

    public RazorpayRefundDispatchBarrierService(
            EntityManager entityManager,
            RazorpayRefundAuditEventRepository auditEvents) {

        this.entityManager = entityManager;
        this.auditEvents = auditEvents;
    }

    /*
     * Persist a one-time dispatch intent.
     *
     * IMPORTANT:
     * - No Razorpay request is made here.
     * - No refund is retried here.
     * - A committed intent means the gateway MAY have been contacted.
     * - An unknown result must go to manual verification.
     *
     * Any future gateway adapter must run only after this method
     * successfully commits, never inside this transaction.
     */
    @Transactional
    public void recordDispatchIntent(
            Long refundAttemptId,
            String workerIdentifier) {

        if (refundAttemptId == null || refundAttemptId <= 0) {
            throw new IllegalArgumentException(
                    "Valid refund attempt ID required."
            );
        }

        if (workerIdentifier == null
                || workerIdentifier.isBlank()
                || workerIdentifier.trim().length() > 320) {
            throw new IllegalArgumentException(
                    "Valid worker identifier required."
            );
        }

        /*
         * Consistent with claim/recovery:
         * lock refund attempt first, reconciliation second.
         */
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

        if (!"PROCESSING".equals(attempt.getStatus())) {
            throw new IllegalStateException(
                    "Refund is not in PROCESSING state."
            );
        }

        RazorpayPaymentReconciliation reconciliation =
                entityManager.find(
                        RazorpayPaymentReconciliation.class,
                        attempt.getReconciliationId(),
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (reconciliation == null
                || !"REFUND_IN_PROGRESS".equals(reconciliation.getStatus())) {
            throw new IllegalStateException(
                    "Reconciliation is not in refund processing."
            );
        }

        if (!attempt.getRazorpayPaymentId().equals(
                    reconciliation.getRazorpayPaymentId())
                || !attempt.getAmountPaise().equals(
                    reconciliation.getAmountPaise())
                || !"INR".equals(attempt.getCurrency())
                || !"INR".equals(reconciliation.getCurrency())) {
            throw new IllegalStateException(
                    "Refund payment details mismatch."
            );
        }

        /*
         * Atomic compare-and-set.
         * Only a NULL dispatch timestamp may be changed.
         * The timestamp is assigned by PostgreSQL.
         */
        int updated = entityManager.createNativeQuery(
            """
            UPDATE razorpay_refund_attempts
               SET gateway_request_sent_at = CURRENT_TIMESTAMP,
                   updated_at = CURRENT_TIMESTAMP
             WHERE id = :attemptId
               AND status = 'PROCESSING'
               AND gateway_request_sent_at IS NULL
            """
        )
        .setParameter("attemptId", refundAttemptId)
        .executeUpdate();

        if (updated != 1) {
            throw new IllegalStateException(
                    "Dispatch intent already recorded or refund state changed."
            );
        }

        RazorpayRefundAuditEvent event =
                new RazorpayRefundAuditEvent();

        event.setRefundAttemptId(refundAttemptId);
        event.setEventType("REFUND_DISPATCH_INTENT_RECORDED");
        event.setPreviousStatus("PROCESSING");
        event.setNewStatus("PROCESSING");
        event.setActorType("SYSTEM");
        event.setActorIdentifier(workerIdentifier.trim());
        event.setEventNote(
                "Durable one-time gateway dispatch intent recorded. "
                + "No gateway request was issued by this service."
        );

        auditEvents.saveAndFlush(event);
    }
}