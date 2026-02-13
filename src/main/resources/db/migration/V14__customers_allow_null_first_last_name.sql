-- Allow first_name and last_name to be null for self-enrollment (only full_name may be set)
ALTER TABLE cpc.customers
  ALTER COLUMN first_name DROP NOT NULL;

ALTER TABLE cpc.customers
  ALTER COLUMN last_name DROP NOT NULL;
