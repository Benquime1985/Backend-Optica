ALTER TABLE sales ADD COLUMN rfc VARCHAR(30);
ALTER TABLE sales ADD COLUMN numero_factura VARCHAR(30);

CREATE TABLE sale_payments (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id),
    metodo VARCHAR(20) NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sale_payments_sale ON sale_payments(sale_id);

UPDATE sales SET status = 'COMPLETADA' WHERE status = 'COMPLETED';
