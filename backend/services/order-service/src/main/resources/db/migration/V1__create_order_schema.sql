CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    source_cart_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    shipping_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL,
    note VARCHAR(1000),
    source VARCHAR(30) NOT NULL,
    shipping_full_name VARCHAR(160) NOT NULL,
    shipping_phone_number VARCHAR(40) NOT NULL,
    shipping_city VARCHAR(80) NOT NULL,
    shipping_district VARCHAR(120) NOT NULL,
    shipping_address_line VARCHAR(500) NOT NULL,
    shipping_postal_code VARCHAR(20),
    shipping_country VARCHAR(80) NOT NULL,
    customer_full_name_snapshot VARCHAR(160) NOT NULL,
    customer_phone_number_snapshot VARCHAR(40) NOT NULL,
    placed_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT uk_orders_source_cart_id UNIQUE (source_cart_id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'FAILED')),
    CONSTRAINT ck_orders_source CHECK (source IN ('WEB', 'MOBILE', 'ADMIN', 'SYSTEM')),
    CONSTRAINT ck_orders_amounts CHECK (
        subtotal_amount >= 0
        AND discount_amount >= 0
        AND shipping_amount >= 0
        AND total_amount >= 0
        AND total_amount = subtotal_amount - discount_amount + shipping_amount
    )
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    cart_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    product_name_snapshot VARCHAR(220) NOT NULL,
    product_slug_snapshot VARCHAR(240) NOT NULL,
    main_image_url_snapshot VARCHAR(1000),
    color_snapshot VARCHAR(80),
    size_snapshot VARCHAR(40),
    unit_price_snapshot NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    reservation_id UUID,
    reservation_key VARCHAR(160) NOT NULL,
    reservation_status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uk_order_items_order_variant UNIQUE (order_id, product_variant_id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_order_items_prices CHECK (
        unit_price_snapshot >= 0
        AND line_total >= 0
        AND line_total = unit_price_snapshot * quantity
    ),
    CONSTRAINT ck_order_items_reservation_status CHECK (reservation_status IN ('ACTIVE', 'RELEASED', 'CONSUMED'))
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_variant_id ON order_items (product_variant_id);
CREATE INDEX idx_order_items_sku ON order_items (sku);
CREATE INDEX idx_order_items_reservation_id ON order_items (reservation_id);
