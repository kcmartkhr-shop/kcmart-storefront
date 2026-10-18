CREATE TABLE order_payment_history (
    id BIGSERIAL PRIMARY KEY,

    order_id BIGINT NOT NULL,

    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,

    payment_method VARCHAR(30) NOT NULL,

    actor_type VARCHAR(30) NOT NULL,
    actor_identifier VARCHAR(320),

    note VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_payment_history_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_payment_history_status
        CHECK (new_status IN ('PENDING', 'PAID')),

    CONSTRAINT chk_order_payment_history_previous_status
        CHECK (
            previous_status IS NULL
            OR previous_status IN ('PENDING', 'PAID')
        ),

    CONSTRAINT chk_order_payment_history_method
        CHECK (payment_method IN ('ONLINE', 'PAY_AT_STORE')),

    CONSTRAINT chk_order_payment_history_actor
        CHECK (actor_type IN ('SYSTEM', 'ADMIN'))
);

CREATE INDEX idx_order_payment_history_order_id
    ON order_payment_history(order_id);

CREATE INDEX idx_order_payment_history_created_at
    ON order_payment_history(created_at);