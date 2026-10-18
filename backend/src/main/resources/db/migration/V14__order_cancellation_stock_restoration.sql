CREATE TABLE order_stock_restorations (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,

    fulfillment_type VARCHAR(30) NOT NULL,

    restored_by VARCHAR(255) NOT NULL,

    restored_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_stock_restorations_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT uk_order_stock_restorations_order
        UNIQUE (order_id),

    CONSTRAINT chk_order_stock_restorations_fulfillment
        CHECK (
            fulfillment_type IN (
                'STORE_PICKUP',
                'DELIVERY'
            )
        )
);

CREATE INDEX idx_order_stock_restorations_restored_at
    ON order_stock_restorations(restored_at);