-- V28: Refund safety foundation.
-- Database structures only. No external refund requests.
-- Initial policy: one full-refund workflow per reconciliation.

CREATE TABLE razorpay_refund_attempts (
    id BIGSERIAL PRIMARY KEY,

    reconciliation_id BIGINT NOT NULL
        REFERENCES razorpay_payment_reconciliations(id)
        ON DELETE RESTRICT,

    razorpay_payment_id VARCHAR(100) NOT NULL,
    razorpay_refund_id VARCHAR(100),

    idempotency_key VARCHAR(100) NOT NULL,

    amount_paise BIGINT NOT NULL
        CHECK (amount_paise > 0),

    currency VARCHAR(10) NOT NULL DEFAULT 'INR'
        CHECK (currency = 'INR'),

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'SUCCEEDED',
            'FAILED',
            'UNKNOWN',
            'MANUAL_REVIEW'
        )),

    approved_by VARCHAR(320) NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    claimed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    gateway_request_sent_at TIMESTAMPTZ,
    gateway_response_received_at TIMESTAMPTZ,

    last_error VARCHAR(2000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refund_attempt_reconciliation
        UNIQUE (reconciliation_id),

    CONSTRAINT uk_refund_attempt_idempotency
        UNIQUE (idempotency_key),

    CONSTRAINT uk_refund_attempt_gateway_refund
        UNIQUE (razorpay_refund_id),

    CONSTRAINT chk_refund_approval_actor
        CHECK (LENGTH(TRIM(approved_by)) > 0),

    CONSTRAINT chk_refund_idempotency_nonempty
        CHECK (LENGTH(TRIM(idempotency_key)) > 0)
);

CREATE INDEX idx_refund_attempt_status
    ON razorpay_refund_attempts(status);

CREATE INDEX idx_refund_attempt_created
    ON razorpay_refund_attempts(created_at);

CREATE TABLE razorpay_refund_audit_events (
    id BIGSERIAL PRIMARY KEY,

    refund_attempt_id BIGINT NOT NULL
        REFERENCES razorpay_refund_attempts(id)
        ON DELETE RESTRICT,

    event_type VARCHAR(60) NOT NULL,

    previous_status VARCHAR(30),
    new_status VARCHAR(30),

    actor_type VARCHAR(30) NOT NULL
        CHECK (actor_type IN ('ADMIN', 'SYSTEM', 'GATEWAY')),

    actor_identifier VARCHAR(320),

    event_note VARCHAR(2000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_refund_audit_event_type
        CHECK (LENGTH(TRIM(event_type)) > 0)
);

CREATE INDEX idx_refund_audit_attempt_created
    ON razorpay_refund_audit_events(
        refund_attempt_id,
        created_at,
        id
    );

-- Append-only audit events.
-- The application role must not update or delete audit records.
-- A trigger prevents modification even when the role has table DML rights.

CREATE FUNCTION kcmart_reject_refund_audit_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Refund audit events are append-only';
END;
$$;

CREATE TRIGGER trg_refund_audit_append_only
BEFORE UPDATE OR DELETE
ON razorpay_refund_audit_events
FOR EACH ROW
EXECUTE FUNCTION kcmart_reject_refund_audit_mutation();