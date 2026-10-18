CREATE TABLE razorpay_payment_attempts (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,

    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100),

    amount_paise BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',

    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',

    signature_verified BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ,

    CONSTRAINT fk_razorpay_payment_attempt_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_razorpay_payment_attempt_order_id
        UNIQUE (razorpay_order_id),

    CONSTRAINT uk_razorpay_payment_attempt_payment_id
        UNIQUE (razorpay_payment_id),

    CONSTRAINT chk_razorpay_payment_amount
        CHECK (amount_paise > 0),

    CONSTRAINT chk_razorpay_payment_currency
        CHECK (currency = 'INR'),

    CONSTRAINT chk_razorpay_payment_status
        CHECK (
            status IN (
                'CREATED',
                'AUTHORIZED',
                'CAPTURED',
                'FAILED',
                'REFUNDED'
            )
        )
);

CREATE INDEX idx_razorpay_payment_attempt_order
    ON razorpay_payment_attempts(order_id);

CREATE INDEX idx_razorpay_payment_attempt_status
    ON razorpay_payment_attempts(status);


CREATE TABLE razorpay_webhook_events (
    id BIGSERIAL PRIMARY KEY,

    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,

    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',

    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),

    last_error VARCHAR(2000),

    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,

    CONSTRAINT uk_razorpay_webhook_event
        UNIQUE (event_id),

    CONSTRAINT chk_razorpay_webhook_processing_status
        CHECK (
            processing_status IN (
                'RECEIVED',
                'PROCESSED',
                'IGNORED',
                'FAILED'
            )
        )
);

CREATE INDEX idx_razorpay_webhook_received
    ON razorpay_webhook_events(received_at);

CREATE INDEX idx_razorpay_webhook_payment
    ON razorpay_webhook_events(razorpay_payment_id);