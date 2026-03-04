-- ============================================================================
-- V0__baseline.sql
-- Creates all schemas, enum types, and tables that existed BEFORE Flyway (V1).
-- V1–V30 build on top of this: adding columns, creating enrollment/claims
-- tables, inserting feature flag data, etc.
-- ============================================================================

-- ============================================================================
-- 1. SCHEMAS
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS cpc;
CREATE SCHEMA IF NOT EXISTS admin;
CREATE SCHEMA IF NOT EXISTS document;

-- ============================================================================
-- 2. ENUM TYPES — cpc schema
-- PostgreSQL CREATE TYPE has no IF NOT EXISTS, so we use DO blocks
-- ============================================================================
DO $$ BEGIN CREATE TYPE cpc.account_status_enum AS ENUM (
    'ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING', 'PENDING_DELETE',
    'PENDING_APPROVAL', 'PENDING_EXIT', 'LEAVING', 'APPROVED',
    'REJECTED', 'COMPLETED'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.account_type_enum AS ENUM (
    'RETAIL_PRIMARY', 'RETAIL_DEPENDENT',
    'CORPORATE_EMPLOYEE', 'CORPORATE_DEPENDENT'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.endorsement_type_enum AS ENUM (
    'ADDITION', 'DELETION', 'BULK_UPLOAD'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
-- V8 adds: INITIAL_UPLOAD

DO $$ BEGIN CREATE TYPE cpc.industry_enum AS ENUM (
    'IT_SERVICES', 'BANKING_FINANCE', 'MANUFACTURING', 'HEALTHCARE',
    'EDUCATION', 'RETAIL', 'TELECOMMUNICATIONS', 'PHARMACEUTICALS',
    'AUTOMOTIVE', 'REAL_ESTATE', 'OTHER',
    'ARTIFICIAL_INTELLIGENCE_/_TECHNOLOGY',
    'ARTIFICIAL_INTELLIGENCE_AND_TECHNOLOGY'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
-- V6 adds: BANKING

DO $$ BEGIN CREATE TYPE cpc.product_type_enum AS ENUM (
    'HEALTH', 'VEHICLE', 'MOTOR', 'LIFE', 'GENERAL',
    'TERM', 'GHI', 'GPA', 'GTI'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
-- V11.1 adds: GMC, GTL (GPA already exists, skipped with IF NOT EXISTS)

DO $$ BEGIN CREATE TYPE cpc.coverage_type_enum AS ENUM (
    'INDIVIDUAL', 'FAMILY_FLOATER', 'GROUP'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
-- V11.1 adds: E, ES, ESC, ESCP

DO $$ BEGIN CREATE TYPE cpc.policy_status_enum AS ENUM (
    'ACTIVE', 'LAPSED', 'CANCELLED', 'EXPIRED', 'PENDING'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.payment_frequency_enum AS ENUM (
    'MONTHLY', 'QUARTERLY', 'BI_ANNUALLY', 'YEARLY'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.confirmation_method_enum AS ENUM (
    'EMAIL', 'PORTAL', 'API', 'PHONE_CALL', 'IN_PERSON', 'OTHER'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.premium_change_enum AS ENUM (
    'INCREASE', 'DECREASE', 'NO_CHANGE'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.relationship_enum AS ENUM (
    'SPOUSE', 'CHILD', 'PARENT', 'SIBLING', 'OTHER',
    'CHILD1', 'CHILD2', 'CHILD3', 'CHILD4',
    'FATHER', 'MOTHER', 'FATHER_IN_LAW', 'MOTHER_IN_LAW'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================================
-- 3. ENUM TYPES — admin schema
-- ============================================================================
DO $$ BEGIN CREATE TYPE admin.user_role_enum AS ENUM (
    'SUPER_ADMIN', 'ADMIN', 'SALES_AGENT', 'SALES_POSP',
    'CLAIMS_PROCESSOR', 'SUPPORT_AGENT', 'SALES_MANAGER',
    'SALES_ADMIN', 'VIMA_ADMIN', 'HR_ADMIN'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE admin.rule_type_enum AS ENUM (
    'SLAB_BASED', 'FIXED_PER_POLICY', 'PREMIUM_BASED'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================================
-- 4. ENUM TYPES — document schema
-- ============================================================================
DO $$ BEGIN CREATE TYPE document.document_entity_type_enum AS ENUM (
    'LEAD', 'INDIVIDUAL', 'POLICY', 'CLAIM', 'ORGANIZATION', 'CUSTOMER'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE document.document_type_enum AS ENUM (
    'PAN_CARD', 'AADHAAR_CARD', 'PASSPORT', 'DRIVING_LICENSE', 'VOTER_ID',
    'BANK_STATEMENT', 'INCOME_PROOF', 'ADDRESS_PROOF', 'PHOTO',
    'POLICY_CERTIFICATE', 'POLICY_SCHEDULE', 'ENDORSEMENT',
    'RENEWAL_NOTICE', 'MEDICAL_BILL', 'DISCHARGE_SUMMARY',
    'PRESCRIPTION', 'LAB_REPORT', 'DIAGNOSTIC_REPORT',
    'CLAIM_FORM', 'OTHER', 'POLICY'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;
-- V14.1 adds: SELF_ENROLLMENT
-- V22  adds: CLAIM_LETTER_APPROVAL, CLAIM_LETTER_REJECTION, CLAIM_LETTER_QUERY, CLAIM_LETTER_PAID, QUERY_RESPONSE_DOC
-- V27  adds: SETTLEMENT_DOCUMENT

DO $$ BEGIN CREATE TYPE document.document_category_enum AS ENUM (
    'KYC_DOCUMENTS', 'POLICY_DOCUMENTS', 'CLAIM_DOCUMENTS',
    'FINANCIAL_DOCUMENTS', 'MEDICAL_RECORDS', 'ENDORSEMENT_DOCUMENTS', 'OTHER'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ============================================================================
-- 5. SEQUENCES — admin schema
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS admin.agent_id_seq START 3;
CREATE SEQUENCE IF NOT EXISTS admin.agent_targets_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.customer_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.incentive_packages_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.incentive_rule_slabs_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.incentive_rules_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.quote_id_seq;
CREATE SEQUENCE IF NOT EXISTS admin.vendor_api_endpoints_id_seq MAXVALUE 2147483647;
CREATE SEQUENCE IF NOT EXISTS admin.vendor_api_headers_id_seq MAXVALUE 2147483647;
CREATE SEQUENCE IF NOT EXISTS admin.vendor_tokens_id_seq MAXVALUE 2147483647;
CREATE SEQUENCE IF NOT EXISTS admin.vendors_id_seq MAXVALUE 2147483647;

-- ============================================================================
-- 6. TABLES — cpc schema (no cross-schema FKs yet)
-- ============================================================================

CREATE TABLE IF NOT EXISTS cpc.organizations (
    organization_id uuid NOT NULL,
    organization_name varchar(255) NOT NULL,
    gstin varchar(15),
    pan_number varchar(10),
    primary_contact_email varchar(255),
    primary_contact_phone varchar(20),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    registered_address varchar(250),
    industry cpc.industry_enum,
    primary_contact_name varchar(20),
    CONSTRAINT organizations_pkey PRIMARY KEY (organization_id)
);

-- customers: V4 adds department/reason_for_exit, V9 adds health_id,
-- V13.1 adds enrollment_*, V14 drops NOT NULL on first/last_name,
-- V14.2 drops NOT NULL on phone, V30 adds actual_relationship
CREATE TABLE IF NOT EXISTS cpc.customers (
    individual_id uuid NOT NULL,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    email varchar(255),
    phone varchar(20) NOT NULL,
    date_of_birth date,
    gender varchar(20),
    pan_number varchar(10),
    aadhaar_number varchar(12),
    address text,
    city varchar(100),
    state varchar(100),
    pincode varchar(6),
    account_type cpc.account_type_enum DEFAULT 'RETAIL_PRIMARY' NOT NULL,
    status cpc.account_status_enum DEFAULT 'ACTIVE' NOT NULL,
    organization_id uuid REFERENCES cpc.organizations(organization_id),
    employee_number varchar(50),
    primary_individual_id uuid REFERENCES cpc.customers(individual_id),
    relationship varchar(50),
    is_primary_member bool DEFAULT true,
    username varchar(100),
    password_hash varchar(255),
    preferred_language varchar(10) DEFAULT 'en',
    lead_id uuid,
    cust_id varchar(50),
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    date_of_joining date,
    designation varchar(50),
    marital_status varchar(255),
    full_name varchar(255),
    sum_insured varchar(255),
    date_of_exit date,
    endorsement_id uuid,
    CONSTRAINT customers_pkey PRIMARY KEY (individual_id),
    CONSTRAINT customers_username_key UNIQUE (username)
);

-- policies: V5 renames premium_amount→total_premium_amount & adds net_amount/gst,
-- V10 adds tpa_*, V11.2 adds policy_category/sum_insured_multiplier/applies_to_employees
-- & drops NOT NULL on coverage_type
CREATE TABLE IF NOT EXISTS cpc.policies (
    policy_id bigserial NOT NULL,
    policy_number varchar(100) NOT NULL,
    primary_individual_id uuid NOT NULL REFERENCES cpc.customers(individual_id),
    insurance_provider_id uuid NOT NULL,
    insurance_product_id uuid,
    organization_id uuid REFERENCES cpc.organizations(organization_id),
    product_type cpc.product_type_enum NOT NULL,
    coverage_type cpc.coverage_type_enum NOT NULL,
    status cpc.policy_status_enum DEFAULT 'ACTIVE' NOT NULL,
    sum_insured numeric(15, 2),
    premium_amount numeric(15, 2) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    renewal_date date,
    lead_id uuid,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    covered_individuals uuid[],
    payment_frequency cpc.payment_frequency_enum DEFAULT 'YEARLY' NOT NULL,
    document_id uuid,
    CONSTRAINT policies_pkey PRIMARY KEY (policy_id),
    CONSTRAINT policies_policy_number_key UNIQUE (policy_number)
);

CREATE TABLE IF NOT EXISTS cpc.endorsements (
    endorsement_id uuid DEFAULT gen_random_uuid() NOT NULL,
    organization_id uuid NOT NULL,
    document_id uuid,
    endorsement_type cpc.endorsement_type_enum NOT NULL,
    status cpc.account_status_enum DEFAULT 'PENDING_APPROVAL' NOT NULL,
    total_employees int4 DEFAULT 0 NOT NULL,
    total_dependents int4 DEFAULT 0 NOT NULL,
    approved_at timestamptz,
    confirmation_method cpc.confirmation_method_enum,
    insurer_ref_number varchar(100),
    premium_change_type cpc.premium_change_enum,
    premium_amount numeric(15, 2),
    uploaded_by uuid,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    approved_by varchar(50),
    CONSTRAINT endorsements_pkey PRIMARY KEY (endorsement_id)
);
-- V13.1 adds: source, enrollment_window_id, submission_count
-- V14.2 adds: source_metadata, enrollment_window_id (idempotent)

CREATE TABLE IF NOT EXISTS cpc.motor_policy_details (
    motor_policy_id uuid NOT NULL,
    policy_id int8 NOT NULL REFERENCES cpc.policies(policy_id) ON DELETE CASCADE,
    vehicle_registration_number varchar(20) NOT NULL,
    vehicle_make varchar(50),
    vehicle_model varchar(50),
    vehicle_type varchar(20),
    manufacturing_year int4,
    registration_date date,
    idv_value numeric(12, 2),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT motor_policy_details_pkey PRIMARY KEY (motor_policy_id)
);

-- nominees: V13.1 drops NOT NULL on policy_id, adds submission_id/customer_id/constraint
CREATE TABLE IF NOT EXISTS cpc.nominees (
    nominee_id uuid NOT NULL,
    policy_id int8 NOT NULL REFERENCES cpc.policies(policy_id) ON DELETE CASCADE,
    first_name varchar(100) NOT NULL,
    last_name varchar(100),
    date_of_birth date NOT NULL,
    gender varchar(255) NOT NULL,
    relationship varchar(255) NOT NULL,
    nominee_percentage numeric(5, 2) DEFAULT 100.00,
    is_active bool DEFAULT true,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    full_name varchar(255),
    CONSTRAINT nominees_pkey PRIMARY KEY (nominee_id)
);

-- ============================================================================
-- 7. TABLES — document schema
-- ============================================================================

-- V22 adds: synced_to_insurer, insurer_doc_ref, synced_at
CREATE TABLE IF NOT EXISTS document.documents (
    document_id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type document.document_entity_type_enum NOT NULL,
    entity_id varchar(50) NOT NULL,
    document_type document.document_type_enum NOT NULL,
    document_category document.document_category_enum NOT NULL,
    s3_bucket varchar(255) NOT NULL,
    s3_key text NOT NULL,
    original_filename varchar(255),
    file_size int8,
    mime_type varchar(100),
    uploaded_by uuid,
    uploaded_by_role admin.user_role_enum,
    uploaded_at timestamp DEFAULT now(),
    notes text,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    CONSTRAINT documents_pkey PRIMARY KEY (document_id)
);

-- ============================================================================
-- 8. TABLES — admin schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS admin.admin_users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    username varchar(100) NOT NULL,
    email varchar(255) NOT NULL,
    full_name varchar(255) NOT NULL,
    role varchar(50) DEFAULT 'SALES_AGENT' NOT NULL,
    is_active bool DEFAULT true,
    last_login timestamp,
    password_hash varchar(255),
    oauth_provider varchar(50),
    oauth_provider_id varchar(100),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    zoho_crm_id varchar(100),
    nonce varchar(255),
    nonce_timestamp timestamp,
    nonce_expires_at timestamp,
    agent_id varchar(255),
    reporting_to uuid REFERENCES admin.admin_users(id) ON DELETE SET NULL,
    organization_id uuid REFERENCES cpc.organizations(organization_id),
    CONSTRAINT admin_users_pkey PRIMARY KEY (id),
    CONSTRAINT admin_users_email_key UNIQUE (email),
    CONSTRAINT admin_users_username_key UNIQUE (username),
    CONSTRAINT agent_id UNIQUE (agent_id)
);
CREATE INDEX IF NOT EXISTS idx_admin_users_organization_id ON admin.admin_users(organization_id);
CREATE INDEX IF NOT EXISTS idx_admin_users_reporting_to_active ON admin.admin_users(reporting_to, is_active);
CREATE INDEX IF NOT EXISTS idx_admin_users_username_active ON admin.admin_users(username, is_active);
CREATE INDEX IF NOT EXISTS idx_reporting_to ON admin.admin_users(reporting_to);

CREATE TABLE IF NOT EXISTS admin.insurance_providers (
    provider_id uuid DEFAULT gen_random_uuid() NOT NULL,
    provider_name varchar(255) NOT NULL,
    provider_code varchar(50) NOT NULL,
    is_active bool DEFAULT true,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    product_type cpc.product_type_enum DEFAULT 'HEALTH' NOT NULL,
    CONSTRAINT insurance_providers_pkey PRIMARY KEY (provider_id),
    CONSTRAINT insurance_providers_provider_code_key UNIQUE (provider_code)
);

CREATE TABLE IF NOT EXISTS admin.incentive_packages (
    id bigserial NOT NULL,
    name varchar(255) NOT NULL,
    description varchar(255),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incentive_packages_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin.incentive_rules (
    id bigserial NOT NULL,
    package_id int8 NOT NULL REFERENCES admin.incentive_packages(id) ON DELETE CASCADE,
    fixed_payout numeric(38, 2),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    rule_type admin.rule_type_enum,
    CONSTRAINT incentive_rules_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin.incentive_rule_slabs (
    id bigserial NOT NULL,
    rule_id int8 NOT NULL REFERENCES admin.incentive_rules(id) ON DELETE CASCADE,
    min_value numeric(38, 2),
    max_value numeric(38, 2),
    payout_amount numeric(38, 2),
    CONSTRAINT incentive_rule_slabs_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_slabs_rule ON admin.incentive_rule_slabs(rule_id);

CREATE TABLE IF NOT EXISTS admin.agent_targets (
    id bigserial NOT NULL,
    agent_id uuid NOT NULL REFERENCES admin.admin_users(id) ON DELETE CASCADE,
    month int4 NOT NULL CHECK (month >= 1 AND month <= 12),
    year int4 NOT NULL CHECK (year >= 2000),
    target_count int4,
    package_id int8 NOT NULL REFERENCES admin.incentive_packages(id) ON DELETE CASCADE,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT agent_targets_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_agent_targets_lookup ON admin.agent_targets(agent_id, month, year);

CREATE TABLE IF NOT EXISTS admin.vendors (
    id serial4 NOT NULL,
    name varchar(100) NOT NULL,
    base_url varchar(255) NOT NULL,
    type varchar(50) NOT NULL,
    active bool DEFAULT true,
    config jsonb NOT NULL,
    created_at timestamp DEFAULT now(),
    CONSTRAINT vendors_pkey PRIMARY KEY (id),
    CONSTRAINT vendors_name_key UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS admin.vendor_tokens (
    id serial4 NOT NULL,
    vendor_id int4 NOT NULL REFERENCES admin.vendors(id) ON DELETE CASCADE,
    access_token varchar(255) NOT NULL,
    refresh_token varchar(255),
    expires_at timestamp NOT NULL,
    created_at timestamp DEFAULT now(),
    updated_at timestamp DEFAULT now(),
    CONSTRAINT vendor_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin.vendor_api_endpoints (
    id serial4 NOT NULL,
    vendor_id int4 NOT NULL REFERENCES admin.vendors(id) ON DELETE CASCADE,
    api_key varchar(100) NOT NULL,
    path varchar(255) NOT NULL,
    method varchar(10) DEFAULT 'POST',
    active bool DEFAULT true,
    description varchar(255),
    created_at timestamp DEFAULT now(),
    CONSTRAINT vendor_api_endpoints_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_vendor_api_key ON admin.vendor_api_endpoints(vendor_id, api_key);

CREATE TABLE IF NOT EXISTS admin.vendor_api_headers (
    id serial4 NOT NULL,
    endpoint_id int4 NOT NULL REFERENCES admin.vendor_api_endpoints(id) ON DELETE CASCADE,
    header_key varchar(100) NOT NULL,
    header_value varchar(255) NOT NULL,
    is_secret bool DEFAULT false,
    created_at timestamp DEFAULT now(),
    CONSTRAINT vendor_api_headers_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin.zoho_token (
    id int8 DEFAULT 1 NOT NULL,
    access_token varchar(255) NOT NULL,
    refresh_token varchar(255) NOT NULL,
    expiry_time timestamptz NOT NULL,
    CONSTRAINT zoho_token_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS admin.customers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cust_id varchar(255) NOT NULL,
    full_name varchar(255) NOT NULL,
    date_of_birth date,
    gender varchar(255),
    phone_number varchar(255) NOT NULL,
    email varchar(255),
    city varchar(255),
    state varchar(255),
    occupation varchar(255),
    annual_income numeric(38, 2),
    dependent_count int4 DEFAULT 0,
    status varchar(255) DEFAULT 'ACTIVE',
    zoho_crm_id varchar(255),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    created_by uuid NOT NULL REFERENCES admin.admin_users(id),
    owner uuid REFERENCES admin.admin_users(id),
    notes varchar(255),
    CONSTRAINT customers_pkey PRIMARY KEY (id),
    CONSTRAINT cust_id_contsraint UNIQUE (cust_id),
    CONSTRAINT customers_phone_number_key UNIQUE (phone_number)
);
CREATE INDEX IF NOT EXISTS idx_customers_owner_created_at ON admin.customers(owner, created_at);
CREATE INDEX IF NOT EXISTS idx_customers_owner_created_at_composite ON admin.customers(owner, created_at) INCLUDE (id, cust_id, full_name, status);
CREATE INDEX IF NOT EXISTS idx_customers_owner_created_at_status ON admin.customers(owner, created_at, status);

CREATE TABLE IF NOT EXISTS admin.quotes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quote_id varchar(20),
    coverage_amount varchar(20),
    best_premium varchar(20),
    status varchar(20),
    created_date date,
    customer_id uuid NOT NULL REFERENCES admin.customers(id),
    companies text[],
    features text[],
    CONSTRAINT quotes_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_quotes_agent_created_date_count ON admin.quotes(created_date, customer_id) INCLUDE (status, best_premium);
CREATE INDEX IF NOT EXISTS idx_quotes_customer_created_date_status ON admin.quotes(customer_id, created_date, status);
CREATE INDEX IF NOT EXISTS idx_quotes_customer_status_created_date ON admin.quotes(customer_id, status, created_date);
CREATE INDEX IF NOT EXISTS idx_quotes_policy_issued_created_date ON admin.quotes(created_date, customer_id) WHERE status = 'POLICY_ISSUED';
CREATE INDEX IF NOT EXISTS idx_quotes_status_created_date_premium ON admin.quotes(status, created_date, best_premium) WHERE status = 'POLICY_ISSUED';

CREATE TABLE IF NOT EXISTS admin.quote_companies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    quote_id uuid NOT NULL REFERENCES admin.quotes(id),
    company_name text,
    premium int8,
    coverage_amount int8,
    claim_ratio numeric(5, 2),
    key_features text[],
    recommended bool,
    CONSTRAINT quote_companies_pkey PRIMARY KEY (id)
);

-- ============================================================================
-- 9. CROSS-SCHEMA FOREIGN KEYS (deferred because of creation order)
-- ============================================================================

DO $$ BEGIN
    ALTER TABLE cpc.endorsements ADD CONSTRAINT fk_endorsements_org
        FOREIGN KEY (organization_id) REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE cpc.endorsements ADD CONSTRAINT fk_endorsements_document
        FOREIGN KEY (document_id) REFERENCES document.documents(document_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE cpc.endorsements ADD CONSTRAINT fk_endorsements_uploaded_by
        FOREIGN KEY (uploaded_by) REFERENCES admin.admin_users(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE cpc.policies ADD CONSTRAINT policies_document_id_fkey
        FOREIGN KEY (document_id) REFERENCES document.documents(document_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
