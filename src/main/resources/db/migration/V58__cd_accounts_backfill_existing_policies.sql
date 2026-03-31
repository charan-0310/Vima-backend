-- V58: Originally backfilled CD accounts for policies missing cd_account_id.
-- That bulk data migration is intentionally not run here: CD accounts are created explicitly
-- via the API (POST /api/v1/cd-balance/policies/{policyId}/cd-account) or admin flows.
--
-- This versioned migration is kept as a no-op so Flyway ordering stays stable.
-- IMPORTANT: If an older checksum of this file already ran in an environment, do not replace
-- this file — use `flyway repair` after coordination, or add a new Vxx migration instead.
SELECT 1;
