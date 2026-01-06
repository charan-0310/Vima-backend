-- Create the custom type for valid actions

CREATE TYPE permission_action AS ENUM ('READ', 'WRITE', 'APPROVE');

CREATE TABLE IF NOT EXISTS admin.feature_flags (
    flag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(150) UNIQUE NOT NULL, -- e.g. 'group-insurance.company'
    description TEXT,
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin.feature_flag_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id UUID REFERENCES admin.feature_flags(flag_id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL, -- e.g. 'ROLE_HR_ADMIN'
    is_active BOOLEAN DEFAULT FALSE,
    actions permission_action[]  DEFAULT ARRAY[]::permission_action[],
    UNIQUE(role_name, flag_id) -- Prevents duplicate entries for the same role/flag
);

CREATE TABLE IF NOT EXISTS admin.feature_flag_companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id UUID REFERENCES admin.feature_flags(flag_id) ON DELETE CASCADE,
    organization_id UUID NOT NULL, -- Links to your Company Table
    is_active BOOLEAN DEFAULT FALSE,
    actions permission_action[] DEFAULT ARRAY[]::permission_action[],
    UNIQUE(organization_id, flag_id) -- Prevents duplicate entries for the same company/flag
);