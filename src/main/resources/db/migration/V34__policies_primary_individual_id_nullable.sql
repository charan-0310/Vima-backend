-- Allow organization policies without a primary individual (company can create policy before customers/employees exist).
-- FK to cpc.customers remains; when primary_individual_id is null, no reference is required.
ALTER TABLE cpc.policies ALTER COLUMN primary_individual_id DROP NOT NULL;
