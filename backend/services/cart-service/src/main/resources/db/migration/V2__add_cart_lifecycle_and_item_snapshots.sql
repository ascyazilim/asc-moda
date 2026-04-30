ALTER TABLE carts
    ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS checked_out_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS abandoned_at TIMESTAMPTZ;

ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS product_slug_snapshot VARCHAR(240),
    ADD COLUMN IF NOT EXISTS main_image_url_snapshot VARCHAR(1000);

UPDATE cart_items
SET product_slug_snapshot = LOWER(TRIM(BOTH '-' FROM REGEXP_REPLACE(product_name_snapshot, '[^a-zA-Z0-9]+', '-', 'g')))
WHERE product_slug_snapshot IS NULL;

UPDATE cart_items
SET product_slug_snapshot = product_variant_id::TEXT
WHERE product_slug_snapshot IS NULL OR product_slug_snapshot = '';

ALTER TABLE cart_items
    ALTER COLUMN product_slug_snapshot SET NOT NULL;
