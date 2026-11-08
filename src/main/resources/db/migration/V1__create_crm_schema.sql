CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE patients (
    id UUID PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(120) NOT NULL,
    birth_date DATE,
    phone VARCHAR(30),
    email VARCHAR(160),
    address VARCHAR(250),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE consultations (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    professional_id UUID NOT NULL REFERENCES users(id),
    consultation_date TIMESTAMP NOT NULL,
    reason VARCHAR(500) NOT NULL,
    visual_acuity VARCHAR(1000),
    graduation VARCHAR(1000),
    observations VARCHAR(2000),
    recommendations VARCHAR(2000),
    next_review_at TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(80),
    price NUMERIC(12, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE sales (
    id UUID PRIMARY KEY,
    folio VARCHAR(30) NOT NULL UNIQUE,
    patient_id UUID REFERENCES patients(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    subtotal NUMERIC(12, 2) NOT NULL,
    discount NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL REFERENCES sales(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_patients_active ON patients(active);
CREATE INDEX idx_consultations_patient ON consultations(patient_id, consultation_date DESC);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_sales_created_at ON sales(created_at);
