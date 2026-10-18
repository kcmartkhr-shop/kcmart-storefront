CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,

    product_id BIGINT NOT NULL
        REFERENCES products(id)
        ON DELETE CASCADE,

    image_url VARCHAR(1000) NOT NULL,

    is_cover BOOLEAN NOT NULL DEFAULT FALSE,

    display_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_image_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_product_images_product_id
    ON product_images(product_id);

CREATE UNIQUE INDEX uq_product_images_cover
    ON product_images(product_id)
    WHERE is_cover = TRUE;

CREATE UNIQUE INDEX uq_product_images_order
    ON product_images(product_id, display_order);
