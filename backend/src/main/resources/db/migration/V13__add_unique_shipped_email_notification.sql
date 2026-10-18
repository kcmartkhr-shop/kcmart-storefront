-- KCMART V13
-- Prevent duplicate shipment email notifications for the same order.

CREATE UNIQUE INDEX ux_order_notifications_shipped_email
    ON order_notifications (order_id)
    WHERE notification_type = 'ORDER_SHIPPED'
      AND channel = 'EMAIL';