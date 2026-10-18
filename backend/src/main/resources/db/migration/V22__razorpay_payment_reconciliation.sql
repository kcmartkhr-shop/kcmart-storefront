CREATE TABLE razorpay_payment_reconciliations (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,
    payment_attempt_id BIGINT NOT NULL,

    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100) NOT NULL,

    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',

    reason VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CAPTURED_REVIEW',

    razorpay_refund_id VARCHAR(100),

    last_error VARCHAR(2000),

    verified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refund_requested_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rpr_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rpr_payment_attempt
        FOREIGN KEY (payment_attempt_id)
        REFERENCES razorpay_payment_attempts(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_rpr_payment_id
        UNIQUE (razorpay_payment_id),

    CONSTRAINT uk_rpr_refund_id
        UNIQUE (razorpay_refund_id),

    CONSTRAINT chk_rpr_amount
        CHECK (amount_paise > 0),

    CONSTRAINT chk_rpr_currency
        CHECK (currency = 'INR'),

    CONSTRAINT chk_rpr_status
        CHECK (
            status IN (
                'CAPTURED_REVIEW',
                'REFUND_PENDING',
                'REFUND_IN_PROGRESS',
                'REFUNDED',
                'REFUND_FAILED',
                'MANUAL_REVIEW'
            )
        )
);

CREATE INDEX idx_rpr_order
    ON razorpay_payment_reconciliations(order_id);

CREATE INDEX idx_rpr_status
    ON razorpay_payment_reconciliations(status);

CREATE INDEX idx_rpr_created
    ON razorpay_payment_reconciliations(created_at);