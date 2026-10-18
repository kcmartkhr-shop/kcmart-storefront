-- ============================================================
-- KCMART ORDER FOUNDATION
-- ============================================================
--
-- Important:
-- Prices stored here are authoritative snapshots created by
-- the backend. Browser-submitted prices must never be trusted.
-- ============================================================


CREATE TABLE orders (

    id BIGSERIAL PRIMARY KEY,

    order_number VARCHAR(40) NOT NULL UNIQUE,

    customer_id BIGINT NOT NULL
        REFERENCES customers(id),

    fulfillment_type VARCHAR(30) NOT NULL,

    customer_name VARCHAR(255) NOT NULL,

    customer_email VARCHAR(320) NOT NULL,

    customer_phone VARCHAR(20) NOT NULL,

    address_line1 VARCHAR(255),

    address_line2 VARCHAR(255),

    landmark VARCHAR(255),

    city VARCHAR(120),

    state VARCHAR(120),

    postal_code VARCHAR(20),

    country VARCHAR(80),

    subtotal NUMERIC(12,2) NOT NULL
        CHECK (subtotal >= 0),

    delivery_charge NUMERIC(12,2) NOT NULL
        DEFAULT 0
        CHECK (delivery_charge >= 0),

    total_amount NUMERIC(12,2) NOT NULL
        CHECK (total_amount >= 0),

    payment_method VARCHAR(30) NOT NULL,

    payment_status VARCHAR(30) NOT NULL
        DEFAULT 'PENDING',

    order_status VARCHAR(30) NOT NULL
        DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    CONSTRAINT chk_orders_fulfillment_type
        CHECK (
            fulfillment_type IN (
                'DELIVERY',
                'STORE_PICKUP'
            )
        ),

    CONSTRAINT chk_orders_payment_method
        CHECK (
            payment_method IN (
                'COD',
                'ONLINE',
                'PAY_AT_STORE'
            )
        ),

    CONSTRAINT chk_orders_payment_status
        CHECK (
            payment_status IN (
                'PENDING',
                'PAID',
                'FAILED',
                'REFUNDED'
            )
        ),

    CONSTRAINT chk_orders_status
        CHECK (
            order_status IN (
                'PENDING',
                'CONFIRMED',
                'PROCESSING',
                'READY_FOR_PICKUP',
                'SHIPPED',
                'DELIVERED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_orders_total
        CHECK (
            total_amount =
            subtotal + delivery_charge
        ),

    CONSTRAINT chk_orders_delivery_address
        CHECK (
            fulfillment_type <> 'DELIVERY'
            OR (
                address_line1 IS NOT NULL
                AND city IS NOT NULL
                AND state IS NOT NULL
                AND postal_code IS NOT NULL
                AND country IS NOT NULL
            )
        ),

    CONSTRAINT chk_orders_pickup_payment
        CHECK (
            payment_method <> 'PAY_AT_STORE'
            OR fulfillment_type = 'STORE_PICKUP'
        )
);


CREATE TABLE order_items (

    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL
        REFERENCES orders(id)
        ON DELETE CASCADE,

    product_id BIGINT NOT NULL
        REFERENCES products(id),

    product_name VARCHAR(255) NOT NULL,

    sku VARCHAR(100) NOT NULL,

    unit_price NUMERIC(12,2) NOT NULL
        CHECK (unit_price >= 0),

    quantity INTEGER NOT NULL
        CHECK (
            quantity >= 1
            AND quantity <= 10
        ),

    line_total NUMERIC(12,2) NOT NULL
        CHECK (line_total >= 0),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT NOW(),

    CONSTRAINT chk_order_items_line_total
        CHECK (
            line_total =
            unit_price * quantity
        ),

    CONSTRAINT uk_order_items_order_product
        UNIQUE (
            order_id,
            product_id
        )
);


CREATE INDEX idx_orders_customer
    ON orders(customer_id);


CREATE INDEX idx_orders_status
    ON orders(order_status);


CREATE INDEX idx_orders_created_at
    ON orders(created_at DESC);


CREATE INDEX idx_order_items_order
    ON order_items(order_id);


CREATE INDEX idx_order_items_product
    ON order_items(product_id);