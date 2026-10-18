-- ============================================================
-- KCMART ORDER LIFECYCLE FOUNDATION
-- ============================================================
-- Adds lifecycle statuses and immutable status history.
--
-- IMPORTANT:
-- - Does NOT deduct or restore inventory.
-- - Does NOT process payments.
-- - Does NOT send email.
-- - Does NOT create shipment or invoice records.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Extend allowed order lifecycle statuses
-- ------------------------------------------------------------

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
    CHECK (
        order_status IN (
            'PENDING',
            'CONFIRMED',
            'PROCESSING',
            'PACKED',
            'READY_FOR_PICKUP',
            'SHIPPED',
            'OUT_FOR_DELIVERY',
            'DELIVERED',
            'COMPLETED',
            'CANCELLED'
        )
    );

-- ------------------------------------------------------------
-- 2. Immutable order status history
-- ------------------------------------------------------------

CREATE TABLE order_status_history (

    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL
        REFERENCES orders(id)
        ON DELETE CASCADE,

    previous_status VARCHAR(30),

    new_status VARCHAR(30) NOT NULL,

    actor_type VARCHAR(30) NOT NULL,

    actor_identifier VARCHAR(320),

    note VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    CONSTRAINT chk_order_status_history_previous
        CHECK (
            previous_status IS NULL
            OR previous_status IN (
                'PENDING',
                'CONFIRMED',
                'PROCESSING',
                'PACKED',
                'READY_FOR_PICKUP',
                'SHIPPED',
                'OUT_FOR_DELIVERY',
                'DELIVERED',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_order_status_history_new
        CHECK (
            new_status IN (
                'PENDING',
                'CONFIRMED',
                'PROCESSING',
                'PACKED',
                'READY_FOR_PICKUP',
                'SHIPPED',
                'OUT_FOR_DELIVERY',
                'DELIVERED',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_order_status_history_actor
        CHECK (
            actor_type IN (
                'SYSTEM',
                'ADMIN',
                'CUSTOMER'
            )
        )
);

CREATE INDEX idx_order_status_history_order
    ON order_status_history(order_id, created_at);

CREATE INDEX idx_order_status_history_created_at
    ON order_status_history(created_at DESC);

-- ------------------------------------------------------------
-- 3. Seed lifecycle history for existing orders
-- ------------------------------------------------------------

INSERT INTO order_status_history (
    order_id,
    previous_status,
    new_status,
    actor_type,
    actor_identifier,
    note,
    created_at
)
SELECT
    id,
    NULL,
    order_status,
    'SYSTEM',
    'migration-v8',
    'Initial lifecycle history created from existing order state.',
    created_at
FROM orders;