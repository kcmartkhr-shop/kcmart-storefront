package in.kcmart.backend.payment.razorpay.service;

import in.kcmart.backend.payment.razorpay.entity.RazorpayPaymentReconciliation;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAttempt;
import in.kcmart.backend.payment.razorpay.entity.RazorpayRefundAuditEvent;
import in.kcmart.backend.payment.razorpay.repository.RazorpayPaymentReconciliationRepository;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAttemptRepository;
import in.kcmart.backend.payment.razorpay.repository.RazorpayRefundAuditEventRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayRefundApprovalService {

    private final EntityManager entityManager;
    private final RazorpayPaymentReconciliationRepository reconciliations;
    private final RazorpayRefundAttemptRepository attempts;
    private final RazorpayRefundAuditEventRepository auditEvents;

    public RazorpayRefundApprovalService(
            EntityManager entityManager,
            RazorpayPaymentReconciliationRepository reconciliations,
            RazorpayRefundAttemptRepository attempts,
            RazorpayRefundAuditEventRepository auditEvents) {

        this.entityManager = entityManager;
        this.reconciliations = reconciliations;
        this.attempts = attempts;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public RazorpayRefundAttempt approveFullRefund(
            Long reconciliationId,
            String idempotencyKey,
            String approvedBy) {

        if (reconciliationId == null || reconciliationId <= 0) {
            throw new IllegalArgumentException(
                    "Valid reconciliation ID required."
            );
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100
                || !idempotencyKey.equals(idempotencyKey.trim())) {
            throw new IllegalArgumentException(
                    "Valid idempotency key required."
            );
        }

        if (approvedBy == null
                || approvedBy.isBlank()
                || approvedBy.length() > 320) {
            throw new IllegalArgumentException(
                    "Valid approving actor required."
            );
        }

        /*
         * Existing approvals can be retried without locking reconciliation.
         *
         * Claim/recovery/dispatch lock the refund attempt first, then
         * reconciliation. An approval retry must not acquire those locks
         * in reverse order.
         *
         * This is a non-locking read. It does not modify the refund.
         */
        var previouslyApproved =
                attempts.findByReconciliationId(reconciliationId);

        if (previouslyApproved.isPresent()) {
            RazorpayRefundAttempt previous = previouslyApproved.get();

            if (!idempotencyKey.equals(previous.getIdempotencyKey())
                    || !approvedBy.trim().equals(previous.getApprovedBy())) {
                throw new IllegalStateException(
                        "Conflicting refund approval already exists."
                );
            }

            return previous;
        }

        RazorpayPaymentReconciliation reconciliation =
                entityManager.find(
                        RazorpayPaymentReconciliation.class,
                        reconciliationId,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (reconciliation == null) {
            throw new IllegalStateException(
                    "Reconciliation not found."
            );
        }

        var existing = attempts.findByReconciliationId(reconciliationId);

        if (existing.isPresent()) {
            RazorpayRefundAttempt previous = existing.get();

            if (!idempotencyKey.equals(previous.getIdempotencyKey())
                    || !approvedBy.trim().equals(previous.getApprovedBy())) {
                throw new IllegalStateException(
                        "Conflicting refund approval already exists."
                );
            }

            return previous;
        }

        if (!"CAPTURED_REVIEW".equals(reconciliation.getStatus())) {
            throw new IllegalStateException(
                    "Reconciliation is not awaiting refund review."
            );
        }

        if (reconciliation.getAmountPaise() == null
                || reconciliation.getAmountPaise() <= 0
                || !"INR".equals(reconciliation.getCurrency())
                || reconciliation.getRazorpayPaymentId() == null
                || reconciliation.getRazorpayPaymentId().isBlank()) {
            throw new IllegalStateException(
                    "Invalid captured payment details."
            );
        }

        if (attempts.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new IllegalStateException(
                    "Idempotency key belongs to another refund."
            );
        }

        RazorpayRefundAttempt attempt = new RazorpayRefundAttempt();

        attempt.setReconciliationId(reconciliationId);
        attempt.setRazorpayPaymentId(
                reconciliation.getRazorpayPaymentId()
        );
        attempt.setIdempotencyKey(idempotencyKey);
        attempt.setAmountPaise(reconciliation.getAmountPaise());
        attempt.setCurrency("INR");
        attempt.setStatus("PENDING");
        attempt.setApprovedBy(approvedBy.trim());

        RazorpayRefundAttempt saved = attempts.saveAndFlush(attempt);

        reconciliation.setStatus("REFUND_PENDING");
        reconciliations.save(reconciliation);

        RazorpayRefundAuditEvent event =
                new RazorpayRefundAuditEvent();

        event.setRefundAttemptId(saved.getId());
        event.setEventType("REFUND_APPROVED");
        event.setPreviousStatus(null);
        event.setNewStatus("PENDING");
        event.setActorType("ADMIN");
        event.setActorIdentifier(approvedBy.trim());
        event.setEventNote(
                "Full refund approved. No gateway request issued."
        );

        auditEvents.saveAndFlush(event);

        return saved;
    }
}