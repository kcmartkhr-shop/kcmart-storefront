package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAuditEvent;

import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentReconciliationRepository;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAuditEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayRefundClaimService {

    private final EntityManager entityManager;
    private final RazorpayPaymentReconciliationRepository reconciliations;
    private final RazorpayRefundAuditEventRepository auditEvents;

    public RazorpayRefundClaimService(
            EntityManager entityManager,
            RazorpayPaymentReconciliationRepository reconciliations,
            RazorpayRefundAuditEventRepository auditEvents) {

        this.entityManager = entityManager;
        this.reconciliations = reconciliations;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public RazorpayRefundAttempt claimPendingRefund(
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
         * Lock the refund attempt first.
         * This serializes competing worker claims.
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

        if (!"PENDING".equals(attempt.getStatus())) {
            throw new IllegalStateException(
                    "Refund attempt is not pending."
            );
        }

        /*
         * Lock the reconciliation before updating its status.
         */
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

        if (!"REFUND_PENDING".equals(reconciliation.getStatus())) {
            throw new IllegalStateException(
                    "Reconciliation is not pending refund execution."
            );
        }

        if (!attempt.getRazorpayPaymentId().equals(
                reconciliation.getRazorpayPaymentId())
                || !attempt.getAmountPaise().equals(
                        reconciliation.getAmountPaise())
                || !"INR".equals(attempt.getCurrency())
                || !"INR".equals(reconciliation.getCurrency())) {

            throw new IllegalStateException(
                    "Refund payment details do not match reconciliation."
            );
        }

        attempt.setClaimedAt(java.time.OffsetDateTime.now());
        attempt.setStatus("PROCESSING");
        entityManager.flush();

        reconciliation.setStatus("REFUND_IN_PROGRESS");
        reconciliations.saveAndFlush(reconciliation);

        RazorpayRefundAuditEvent event =
                new RazorpayRefundAuditEvent();

        event.setRefundAttemptId(attempt.getId());
        event.setEventType("REFUND_CLAIMED");
        event.setPreviousStatus("PENDING");
        event.setNewStatus("PROCESSING");
        event.setActorType("SYSTEM");
        event.setActorIdentifier(workerIdentifier.trim());
        event.setEventNote(
                "Refund claimed for processing. No gateway request issued."
        );

        auditEvents.saveAndFlush(event);

        return attempt;
    }
}