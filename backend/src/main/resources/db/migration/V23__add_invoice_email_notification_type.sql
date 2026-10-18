-- KCMART V23
-- Allow separate, auditable invoice email notifications.
-- Preserve existing completion and shipment uniqueness indexes.

ALTER TABLE order_notifications
    DROP CONSTRAINT chk_order_notification_type;

ALTER TABLE order_notifications
    ADD CONSTRAINT chk_order_notification_type
    CHECK (
        notification_type IN (
            'ORDER_COMPLETED',
            'ORDER_SHIPPED',
            'INVOICE_EMAIL'
        )
    );

CREATE INDEX idx_order_notifications_invoice_email
    ON order_notifications (order_id, created_at DESC, id DESC)
    WHERE notification_type = 'INVOICE_EMAIL'
      AND channel = 'EMAIL';