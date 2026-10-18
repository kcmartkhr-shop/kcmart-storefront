CREATE TABLE order_notifications (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,

    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,

    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(255) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    attempt_count INTEGER NOT NULL DEFAULT 0,

    last_error VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,

    CONSTRAINT fk_order_notifications_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_notification_type
        CHECK (
            notification_type IN (
                'ORDER_COMPLETED',
                'ORDER_SHIPPED'
            )
        ),

    CONSTRAINT chk_order_notification_channel
        CHECK (
            channel IN ('EMAIL')
        ),

    CONSTRAINT chk_order_notification_status
        CHECK (
            status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_order_notification_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_order_notifications_order_id
    ON order_notifications(order_id);

CREATE INDEX idx_order_notifications_status
    ON order_notifications(status);

CREATE INDEX idx_order_notifications_created_at
    ON order_notifications(created_at);

CREATE UNIQUE INDEX uq_order_completed_email
    ON order_notifications(order_id, notification_type, channel)
    WHERE notification_type = 'ORDER_COMPLETED'
      AND channel = 'EMAIL';