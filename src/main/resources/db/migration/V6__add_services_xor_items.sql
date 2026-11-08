CREATE TABLE services (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(80),
    price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE sale_items ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE sale_items ADD COLUMN service_id UUID REFERENCES services(id);

ALTER TABLE sale_items ADD CONSTRAINT chk_sale_items_xor CHECK (
    (product_id IS NOT NULL AND service_id IS NULL)
    OR (product_id IS NULL AND service_id IS NOT NULL)
);
