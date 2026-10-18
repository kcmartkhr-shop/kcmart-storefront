-- ============================================================
-- KCMART TEMPORARY ONLINE STOCK RESERVATIONS
-- ============================================================
--
-- Delivery + ONLINE orders temporarily reserve stock while
-- the customer completes Razorpay payment.
--
-- IMPORTANT:
-- product_inventory.online_stock is NOT reduced when a
-- temporary reservation is created.
--
-- Effective available online stock:
--
-- online_stock - ACTIVE reservations
--
-- ACTIVE reservations expire after the application-managed
-- five-minute reservation window.
-- ============================================================

CREATE TABLE order_stock_reservations (

    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL
        REFERENCES orders(id)
        ON DELETE CASCADE,

    product_id BIGINT NOT NULL
        REFERENCES products(id)
        ON DELETE CASCADE,

    quantity INTEGER NOT NULL
        CHECK (
            quantity >= 1
            AND quantity <= 10
        ),

    status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    expires_at TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    consumed_at TIMESTAMPTZ,

    released_at TIMESTAMPTZ,

    CONSTRAINT chk_order_stock_reservations_status
        CHECK (
            status IN (
                'ACTIVE',
                'CONSUMED',
                'RELEASED'
            )
        ),

    CONSTRAINT chk_order_stock_reservations_timestamps
        CHECK (
            (status = 'ACTIVE'
                AND consumed_at IS NULL
                AND released_at IS NULL)
            OR
            (status = 'CONSUMED'
                AND consumed_at IS NOT NULL
                AND released_at IS NULL)
            OR
            (status = 'RELEASED'
                AND consumed_at IS NULL
                AND released_at IS NOT NULL)
        )
);


CREATE INDEX idx_order_stock_reservations_order
    ON order_stock_reservations(order_id);


CREATE INDEX idx_order_stock_reservations_product_status
    ON order_stock_reservations(product_id, status);


CREATE INDEX idx_order_stock_reservations_expires_at
    ON order_stock_reservations(expires_at);


CREATE UNIQUE INDEX uk_order_stock_reservations_order_product
    ON order_stock_reservations(order_id, product_id);
