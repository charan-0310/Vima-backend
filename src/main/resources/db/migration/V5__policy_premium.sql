ALTER TABLE cpc.policies
    RENAME COLUMN premium_amount TO total_premium_amount;    

ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS net_amount VARCHAR(100);

ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS gst VARCHAR(100);