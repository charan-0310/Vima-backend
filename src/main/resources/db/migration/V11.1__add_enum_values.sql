-- Migration to support GMC, GPA, and GTL policy types - Part 1
-- V11.1__add_enum_values.sql
-- This migration adds enum values that will be used in V11.2
-- PostgreSQL requires enum values to be committed before use, so this is a separate migration

-- Add new values to product_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'GMC') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'GMC';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'GPA') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'GPA';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'GTL') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'GTL';
    END IF;
END $$;

-- Add new values to coverage_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'coverage_type_enum' AND e.enumlabel = 'E') THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'E';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'coverage_type_enum' AND e.enumlabel = 'ES') THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'ES';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'coverage_type_enum' AND e.enumlabel = 'ESC') THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'ESC';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'coverage_type_enum' AND e.enumlabel = 'ESCP') THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'ESCP';
    END IF;
END $$;
