CREATE SCHEMA IF NOT EXISTS staff_schema;

CREATE TABLE staff_schema.staff_assignments (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    locker_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_staff_assignments_staff_id ON staff_schema.staff_assignments(staff_id);
CREATE INDEX idx_staff_assignments_order_id ON staff_schema.staff_assignments(order_id);
