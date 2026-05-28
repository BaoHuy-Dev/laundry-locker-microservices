CREATE SCHEMA IF NOT EXISTS partner_schema;

CREATE TABLE partner_schema.partners (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    contact_email VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE partner_schema.staff_access_codes (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    action VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP
);
