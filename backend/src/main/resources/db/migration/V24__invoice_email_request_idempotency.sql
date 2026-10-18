-- KCMART V24
-- Track an idempotency key for explicitly requested invoice emails.
-- Existing notification rows remain unchanged.

ALTER TABLE order_notifications
    ADD COLUMN request_key VARCHAR(100);

CREATE UNIQUE INDEX uq_order_notifications_invoice_request_key
    ON order_notifications (request_key)
    WHERE notification_type = 'INVOICE_EMAIL'
      AND channel = 'EMAIL'
      AND request_key IS NOT NULL;