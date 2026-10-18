-- KCMART Stage 5C-10
--
-- DELIVERY     -> ONLINE only
-- STORE_PICKUP -> ONLINE or PAY_AT_STORE
-- COD          -> disabled
--
-- Existing V5 migration is intentionally untouched.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM orders
        WHERE payment_method = 'COD'
    ) THEN
        RAISE EXCEPTION
            'Cannot disable COD because existing COD orders exist';
    END IF;
END $$;

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_payment_method;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_method
    CHECK (
        payment_method IN (
            'ONLINE',
            'PAY_AT_STORE'
        )
    );

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_delivery_online_payment;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_delivery_online_payment
    CHECK (
        fulfillment_type <> 'DELIVERY'
        OR payment_method = 'ONLINE'
    );