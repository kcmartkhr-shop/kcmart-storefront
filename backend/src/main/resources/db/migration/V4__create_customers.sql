CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,

    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,

    email VARCHAR(320) NOT NULL,
    name VARCHAR(255),
    image_url TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_customers_provider_subject
        UNIQUE (provider, provider_subject),

    CONSTRAINT uk_customers_email
        UNIQUE (email)
);

CREATE INDEX idx_customers_active
    ON customers(active);