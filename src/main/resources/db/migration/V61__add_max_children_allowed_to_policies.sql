ALTER TABLE cpc.policies
ADD COLUMN IF NOT EXISTS max_children_allowed INTEGER;
