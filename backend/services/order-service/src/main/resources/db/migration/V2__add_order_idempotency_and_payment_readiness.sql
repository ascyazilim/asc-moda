ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(160),
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(160),
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(160),
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_customer_idempotency
    ON orders (customer_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_external_reference
    ON orders (external_reference)
    WHERE external_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_idempotency_key ON orders (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_orders_payment_reference ON orders (payment_reference);
CREATE INDEX IF NOT EXISTS idx_orders_total_amount ON orders (total_amount);
CREATE INDEX IF NOT EXISTS idx_order_items_reservation_key ON order_items (reservation_key);
