CREATE TABLE product_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    orden INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE products ADD COLUMN category_id UUID REFERENCES product_categories(id);

INSERT INTO product_categories (id, name, orden, active)
SELECT gen_random_uuid(), category, 0, TRUE
FROM products
WHERE category IS NOT NULL AND category <> ''
GROUP BY category;

UPDATE products
SET category_id = (
    SELECT pc.id FROM product_categories pc
    WHERE lower(pc.name) = lower(products.category)
);

ALTER TABLE products DROP COLUMN category;

CREATE INDEX idx_product_categories_orden ON product_categories(orden, name);
