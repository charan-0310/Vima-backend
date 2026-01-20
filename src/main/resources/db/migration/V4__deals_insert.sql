ALTER TABLE cpc.customers
    ADD COLUMN IF NOT EXISTS department VARCHAR(100);

ALTER TABLE cpc.customers
    ADD COLUMN IF NOT EXISTS reason_for_exit VARCHAR(100);