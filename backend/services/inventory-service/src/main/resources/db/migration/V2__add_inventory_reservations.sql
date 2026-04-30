ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS last_stock_change_at TIMESTAMPTZ;

UPDATE inventory_items
SET last_stock_change_at = COALESCE(updated_at, created_at, NOW())
WHERE last_stock_change_at IS NULL;

ALTER TABLE stock_movements
    ADD COLUMN IF NOT EXISTS before_quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS after_quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS before_reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS after_reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(240),
    ADD COLUMN IF NOT EXISTS operator_id VARCHAR(120);

ALTER TABLE stock_movements
    DROP CONSTRAINT IF EXISTS ck_stock_movements_type;

ALTER TABLE stock_movements
    ADD CONSTRAINT ck_stock_movements_type
        CHECK (movement_type IN ('INCREASE', 'DECREASE', 'RESERVE', 'RELEASE', 'CONSUME', 'ADJUSTMENT'));

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id VARCHAR(120) NOT NULL,
    reservation_key VARCHAR(160) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_stock_reservations_inventory_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id),
    CONSTRAINT uk_stock_reservations_key UNIQUE (reservation_key),
    CONSTRAINT ck_stock_reservations_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_stock_reservations_reference_type CHECK (reference_type IN ('ADMIN', 'ORDER', 'SYSTEM')),
    CONSTRAINT ck_stock_reservations_quantity CHECK (quantity >= 0)
);

CREATE UNIQUE INDEX ux_stock_reservations_reference_active
    ON stock_reservations (reference_type, reference_id, product_variant_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_inventory_items_low_stock ON inventory_items (is_active, low_stock_threshold);
CREATE INDEX idx_stock_movements_movement_type ON stock_movements (movement_type);
CREATE INDEX idx_stock_movements_reference_id ON stock_movements (reference_id);
CREATE INDEX idx_stock_movements_reference_type ON stock_movements (reference_type);
CREATE INDEX idx_stock_reservations_inventory_item_id ON stock_reservations (inventory_item_id);
CREATE INDEX idx_stock_reservations_product_variant_id ON stock_reservations (product_variant_id);
CREATE INDEX idx_stock_reservations_sku ON stock_reservations (sku);
CREATE INDEX idx_stock_reservations_status ON stock_reservations (status);
CREATE INDEX idx_stock_reservations_reference ON stock_reservations (reference_type, reference_id);
CREATE INDEX idx_stock_reservations_expires_at ON stock_reservations (expires_at);
