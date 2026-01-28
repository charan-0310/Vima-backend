-- Add health_id column to customers table
-- Flyway Migration: V9__add_health_id_column.sql

ALTER TABLE cpc.customers
    ADD COLUMN IF NOT EXISTS health_id VARCHAR(100);

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_customers_health_id ON cpc.customers(health_id);

-- Add comment
COMMENT ON COLUMN cpc.customers.health_id IS 'Health ID assigned to the customer';
