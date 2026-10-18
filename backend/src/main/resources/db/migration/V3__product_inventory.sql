ALTER TABLE products
ADD COLUMN IF NOT EXISTS image_url TEXT;

CREATE TABLE IF NOT EXISTS product_inventory (
    product_id BIGINT PRIMARY KEY REFERENCES products(id)
        ON DELETE CASCADE,
    online_stock INTEGER NOT NULL DEFAULT 0
        CHECK (online_stock >= 0),
    store_stock INTEGER NOT NULL DEFAULT 0
        CHECK (store_stock >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);