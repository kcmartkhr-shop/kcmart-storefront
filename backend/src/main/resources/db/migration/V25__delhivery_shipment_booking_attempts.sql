CREATE TABLE delhivery_shipment_booking_attempts (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL
        REFERENCES orders(id),

    attempt_status VARCHAR(20) NOT NULL
        DEFAULT 'PENDING',

    waybill VARCHAR(150),

    failure_reason VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_delhivery_booking_status
        CHECK (
            attempt_status IN (
                'PENDING',
                'SUCCEEDED',
                'UNKNOWN',
                'FAILED'
            )
        ),

    CONSTRAINT uq_delhivery_booking_order
        UNIQUE (order_id)
);

CREATE UNIQUE INDEX uq_delhivery_booking_waybill
    ON delhivery_shipment_booking_attempts (waybill)
    WHERE waybill IS NOT NULL;