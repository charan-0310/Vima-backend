-- Store original relationship label (e.g. Son, Daughter, Child) when we normalize to CHILD1–CHILD4.
-- Used for display and reporting; lookups remain on (employee_number, relationship).
ALTER TABLE cpc.customers ADD COLUMN IF NOT EXISTS actual_relationship VARCHAR(50) NULL;
COMMENT ON COLUMN cpc.customers.actual_relationship IS 'User-provided relationship label (e.g. Son, Daughter) when relationship is CHILD1–CHILD4; null for non-child or when not provided.';
