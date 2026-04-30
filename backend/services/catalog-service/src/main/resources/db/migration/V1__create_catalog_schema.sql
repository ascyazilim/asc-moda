CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(220) NOT NULL,
    slug VARCHAR(240) NOT NULL,
    description VARCHAR(5000),
    short_description VARCHAR(500),
    base_price NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_products_base_price CHECK (base_price >= 0)
);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    sku VARCHAR(120) NOT NULL,
    color VARCHAR(80),
    size VARCHAR(40),
    stock_keeping_note VARCHAR(500),
    price_override NUMERIC(12, 2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_product_variants_price_override CHECK (price_override IS NULL OR price_override >= 0)
);

CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    variant_id UUID,
    image_url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(300),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_images_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id)
);

CREATE INDEX idx_categories_active ON categories (is_active);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_product_variants_active ON product_variants (is_active);
CREATE INDEX idx_product_images_product_id ON product_images (product_id);
CREATE INDEX idx_product_images_variant_id ON product_images (variant_id);
CREATE INDEX idx_product_images_product_sort_order ON product_images (product_id, sort_order);
CREATE UNIQUE INDEX ux_product_images_main_product ON product_images (product_id)
    WHERE is_main = TRUE AND variant_id IS NULL AND is_active = TRUE;
CREATE UNIQUE INDEX ux_product_images_main_variant ON product_images (variant_id)
    WHERE is_main = TRUE AND variant_id IS NOT NULL AND is_active = TRUE;
