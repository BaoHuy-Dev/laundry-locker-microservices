CREATE SCHEMA IF NOT EXISTS laundry_schema;

CREATE TABLE laundry_schema.laundry_catalog_items (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) DEFAULT 'LAUNDRY',
    service_type VARCHAR(50) DEFAULT 'WASH',
    unit_price NUMERIC(12,2) DEFAULT 0,
    max_price NUMERIC(12,2),
    unit VARCHAR(50) DEFAULT 'kg',
    description VARCHAR(2000),
    image VARCHAR(1000),
    is_addon BOOLEAN DEFAULT FALSE,
    is_monthly_package BOOLEAN DEFAULT FALSE,
    estimated_hours INTEGER,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_laundry_items_store_id ON laundry_schema.laundry_catalog_items(store_id);
