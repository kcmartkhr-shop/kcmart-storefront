CREATE TABLE order_invoices (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL UNIQUE,

    invoice_number VARCHAR(100) NOT NULL UNIQUE,

    storage_key VARCHAR(500) NOT NULL,

    original_filename VARCHAR(255) NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    file_size BIGINT NOT NULL,

    uploaded_by VARCHAR(320) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_invoices_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_invoice_file_size
        CHECK (file_size > 0),

    CONSTRAINT chk_order_invoice_content_type
        CHECK (content_type = 'application/pdf')
);

CREATE INDEX idx_order_invoices_order_id
    ON order_invoices(order_id);

CREATE INDEX idx_order_invoices_created_at
    ON order_invoices(created_at);