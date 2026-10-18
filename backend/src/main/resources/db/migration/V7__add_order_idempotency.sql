-- ============================================================
-- KCMART ORDER IDEMPOTENCY FOUNDATION
-- ============================================================
-- Prevents one checkout attempt from creating multiple orders.
--
-- Order creation is NOT enabled by this migration.
-- Stock deduction is NOT performed by this migration.
-- Payment processing is NOT performed by this migration.
-- ============================================================

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE orders
    ADD CONSTRAINT uk_orders_idempotency_key
    UNIQUE (idempotency_key);