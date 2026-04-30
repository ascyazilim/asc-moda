CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_inventory_items_sku UNIQUE (sku),
    CONSTRAINT uk_inventory_items_product_variant_id UNIQUE (product_variant_id),
    CONSTRAINT ck_inventory_items_quantities CHECK (
        quantity_on_hand >= 0
        AND reserved_quantity >= 0
        AND reserved_quantity <= quantity_on_hand
    )
);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL,
    note VARCHAR(1000),
    reference_type VARCHAR(30) NOT NULL,
    reference_id VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_stock_movements_inventory_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id),
    CONSTRAINT ck_stock_movements_type CHECK (movement_type IN ('INCREASE', 'DECREASE', 'RESERVE', 'RELEASE', 'ADJUSTMENT')),
    CONSTRAINT ck_stock_movements_reference_type CHECK (reference_type IN ('ADMIN', 'ORDER', 'SYSTEM')),
    CONSTRAINT ck_stock_movements_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_items_product_variant_id ON inventory_items (product_variant_id);
CREATE INDEX idx_inventory_items_active ON inventory_items (is_active);
CREATE INDEX idx_stock_movements_inventory_item_id ON stock_movements (inventory_item_id);
CREATE INDEX idx_stock_movements_sku ON stock_movements (sku);
CREATE INDEX idx_stock_movements_created_at ON stock_movements (created_at);
