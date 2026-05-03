CREATE TABLE customers (
    id UUID PRIMARY KEY,
    external_user_id VARCHAR(120),
    email VARCHAR(320) NOT NULL,
    phone_number VARCHAR(40),
    first_name VARCHAR(120) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'PASSIVE', 'BLOCKED')),
    CONSTRAINT ck_customers_email_not_blank CHECK (length(trim(email)) > 0)
);

CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    title VARCHAR(80) NOT NULL,
    address_type VARCHAR(30) NOT NULL,
    full_name VARCHAR(240) NOT NULL,
    phone_number VARCHAR(40) NOT NULL,
    city VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    address_line VARCHAR(1000) NOT NULL,
    postal_code VARCHAR(20),
    country VARCHAR(80) NOT NULL,
    is_default_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    is_default_billing BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_customer_addresses_type CHECK (address_type IN ('SHIPPING', 'BILLING', 'OTHER')),
    CONSTRAINT ck_customer_addresses_inactive_not_default
        CHECK (is_active OR (is_default_shipping = FALSE AND is_default_billing = FALSE)),
    CONSTRAINT ck_customer_addresses_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT ck_customer_addresses_full_name_not_blank CHECK (length(trim(full_name)) > 0),
    CONSTRAINT ck_customer_addresses_phone_not_blank CHECK (length(trim(phone_number)) > 0),
    CONSTRAINT ck_customer_addresses_city_not_blank CHECK (length(trim(city)) > 0),
    CONSTRAINT ck_customer_addresses_district_not_blank CHECK (length(trim(district)) > 0),
    CONSTRAINT ck_customer_addresses_line_not_blank CHECK (length(trim(address_line)) > 0),
    CONSTRAINT ck_customer_addresses_country_not_blank CHECK (length(trim(country)) > 0)
);

CREATE UNIQUE INDEX ux_customers_email ON customers (email);
CREATE UNIQUE INDEX ux_customers_external_user_id ON customers (external_user_id)
    WHERE external_user_id IS NOT NULL;
CREATE UNIQUE INDEX ux_customer_addresses_default_shipping ON customer_addresses (customer_id)
    WHERE is_default_shipping = TRUE AND is_active = TRUE;
CREATE UNIQUE INDEX ux_customer_addresses_default_billing ON customer_addresses (customer_id)
    WHERE is_default_billing = TRUE AND is_active = TRUE;

CREATE INDEX idx_customers_status ON customers (status);
CREATE INDEX idx_customers_email ON customers (email);
CREATE INDEX idx_customers_created_at ON customers (created_at);
CREATE INDEX idx_customer_addresses_customer_id ON customer_addresses (customer_id);
CREATE INDEX idx_customer_addresses_created_at ON customer_addresses (created_at);
