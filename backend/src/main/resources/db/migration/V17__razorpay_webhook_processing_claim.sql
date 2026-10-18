ALTER TABLE razorpay_webhook_events
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN processing_started_at TIMESTAMPTZ;

ALTER TABLE razorpay_webhook_events
    DROP CONSTRAINT IF EXISTS razorpay_webhook_events_processing_status_check;

ALTER TABLE razorpay_webhook_events
    ADD CONSTRAINT razorpay_webhook_events_processing_status_check
    CHECK (
        processing_status IN (
            'RECEIVED',
            'PROCESSING',
            'PROCESSED',
            'IGNORED',
            'FAILED'
        )
    );

ALTER TABLE razorpay_webhook_events
    ADD CONSTRAINT razorpay_webhook_events_attempt_count_check
    CHECK (attempt_count >= 0);

CREATE INDEX idx_razorpay_webhook_processing_recovery
    ON razorpay_webhook_events (
        processing_status,
        processing_started_at
    );