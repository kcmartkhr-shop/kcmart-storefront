CREATE TABLE order_shipments (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,

    carrier_name VARCHAR(100),
    tracking_number VARCHAR(150),
    tracking_url VARCHAR(1000),

    shipment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    shipped_at TIMESTAMPTZ,
    out_for_delivery_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,

    admin_note VARCHAR(1000),

    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_shipments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_order_shipments_order
        UNIQUE (order_id),

    CONSTRAINT chk_order_shipments_status
        CHECK (
            shipment_status IN (
                'PENDING',
                'PACKED',
                'SHIPPED',
                'OUT_FOR_DELIVERY',
                'DELIVERED'
            )
        )
);

CREATE INDEX idx_order_shipments_tracking_number
    ON order_shipments(tracking_number);

CREATE INDEX idx_order_shipments_status
    ON order_shipments(shipment_status);