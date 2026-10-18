CREATE TABLE delhivery_shipment_packages (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    actual_weight_grams INTEGER NOT NULL,
    length_cm NUMERIC(10,2) NOT NULL,
    width_cm NUMERIC(10,2) NOT NULL,
    height_cm NUMERIC(10,2) NOT NULL,
    volumetric_weight_grams INTEGER NOT NULL,
    chargeable_weight_grams INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_delhivery_package_order UNIQUE (order_id),

    CONSTRAINT chk_delhivery_actual_weight
        CHECK (actual_weight_grams > 0),

    CONSTRAINT chk_delhivery_dimensions
        CHECK (
            length_cm > 0 AND
            width_cm > 0 AND
            height_cm > 0
        ),

    CONSTRAINT chk_delhivery_volumetric_weight
        CHECK (volumetric_weight_grams > 0),

    CONSTRAINT chk_delhivery_chargeable_weight
        CHECK (
            chargeable_weight_grams >= actual_weight_grams
            AND chargeable_weight_grams >= volumetric_weight_grams
        )
);