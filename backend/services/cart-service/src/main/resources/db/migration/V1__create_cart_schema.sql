CREATE TABLE carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'ABANDONED'))
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    product_name_snapshot VARCHAR(220) NOT NULL,
    variant_name_snapshot VARCHAR(160),
    color_snapshot VARCHAR(80),
    size_snapshot VARCHAR(40),
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    is_selected BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT uk_cart_items_cart_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_cart_items_unit_price CHECK (unit_price_snapshot >= 0)
);

CREATE UNIQUE INDEX ux_carts_customer_active ON carts (customer_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_carts_customer_id ON carts (customer_id);
CREATE INDEX idx_carts_status ON carts (status);
CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_variant_id ON cart_items (product_variant_id);
CREATE INDEX idx_cart_items_sku ON cart_items (sku);
