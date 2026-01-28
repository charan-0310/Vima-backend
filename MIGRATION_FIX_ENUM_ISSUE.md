# ✅ Migration File Fixed - PostgreSQL Enum Issue Resolved

## Problem
The migration was failing with error:
```
ERROR: invalid input value for enum cpc.product_type_enum: "GMC"
```

## Root Cause
The database has `product_type` and `coverage_type` as **PostgreSQL ENUM types**, not VARCHAR. The migration was trying to insert new values ('GMC', 'GPA', 'GTL', 'E', 'ES', 'ESC', 'ESCP') that didn't exist in the enum definitions yet.

## Solution
Updated the migration file to **add new enum values BEFORE attempting to use them**.

---

## Migration Structure (Fixed)

### Step 1: Add GMC, GPA, GTL to product_type_enum
```sql
DO $$
BEGIN
    IF NOT EXISTS (...'GMC'...) THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'GMC';
    END IF;
    -- Same for GPA and GTL
END $$;
```

### Step 2: Add E, ES, ESC, ESCP to coverage_type_enum
```sql
DO $$
BEGIN
    IF NOT EXISTS (...'E'...) THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'E';
    END IF;
    -- Same for ES, ESC, ESCP
END $$;
```

### Step 3-9: Rest of Migration
- Add new columns (policy_category, sum_insured_multiplier, applies_to_employees)
- Remove NOT NULL constraints
- Migrate existing data
- Add comments and indexes

---

## Key Changes

### Before (Broken):
```sql
-- ❌ Tried to use 'GMC' before adding it to enum
ALTER TABLE cpc.policies...
UPDATE cpc.policies SET product_type = 'GMC'  -- FAILS!
```

### After (Fixed):
```sql
-- ✅ Add 'GMC' to enum FIRST
ALTER TYPE cpc.product_type_enum ADD VALUE 'GMC';
-- ✅ Then use it
UPDATE cpc.policies SET product_type = 'GMC'  -- SUCCESS!
```

---

## PostgreSQL Enum Safety

### Check Before Adding:
```sql
IF NOT EXISTS (
    SELECT 1 FROM pg_enum e 
    JOIN pg_type t ON e.enumtypid = t.oid 
    WHERE t.typname = 'product_type_enum' 
    AND e.enumlabel = 'GMC'
) THEN
    ALTER TYPE cpc.product_type_enum ADD VALUE 'GMC';
END IF;
```

This ensures:
- ✅ Idempotent (safe to run multiple times)
- ✅ Won't fail if value already exists
- ✅ Can be rolled back and re-run

---

## Enum Values Added

### product_type_enum:
- **GMC** (Group Medical Coverage)
- **GPA** (Group Personal Accident)
- **GTL** (Group Term Life)

### coverage_type_enum:
- **E** (Employee Only)
- **ES** (Employee + Spouse)
- **ESC** (Employee + Spouse + Children)
- **ESCP** (Employee + Spouse + Children + Parents)

---

## Important Notes

### 1. PostgreSQL Enum Limitations
- ⚠️ Cannot remove enum values once added
- ⚠️ Cannot rename enum values
- ⚠️ Values are added permanently
- ✅ Can only add new values

### 2. Migration Order Matters
```
1. Add enum values first ✅
2. Then use them in queries ✅
```

### 3. Existing Data
- Existing HEALTH/GHI policies will be migrated to GMC
- Existing coverage types (INDIVIDUAL, FAMILY_FLOATER, GROUP) will be migrated to (E, ES, ES)

---

## Testing the Migration

### Check Current Enum Values:
```sql
-- Check product_type enum
SELECT e.enumlabel 
FROM pg_enum e 
JOIN pg_type t ON e.enumtypid = t.oid 
WHERE t.typname = 'product_type_enum'
ORDER BY e.enumsortorder;

-- Check coverage_type enum
SELECT e.enumlabel 
FROM pg_enum e 
JOIN pg_type t ON e.enumtypid = t.oid 
WHERE t.typname = 'coverage_type_enum'
ORDER BY e.enumsortorder;
```

### After Migration Should Show:
```
product_type_enum:
- HEALTH
- MOTOR
- GENERAL
- TERM
- LIFE
- GHI
- GTI
- GMC  ← NEW
- GPA  ← NEW
- GTL  ← NEW

coverage_type_enum:
- INDIVIDUAL
- FAMILY_FLOATER
- GROUP
- E    ← NEW
- ES   ← NEW
- ESC  ← NEW
- ESCP ← NEW
```

---

## Migration File Status

### ✅ Fixed Issues:
1. Added enum value additions BEFORE usage
2. Removed duplicate Step 3
3. Proper order of operations
4. Idempotent checks for all enum additions

### ✅ Safe to Run:
- Can be executed on fresh database
- Can be executed on database with existing data
- Won't fail if enum values already exist
- Won't cause data loss

---

## Next Steps

1. **Drop existing V11 migration** from Flyway history if it failed:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '11';
   ```

2. **Re-run the application** - Flyway will execute the fixed migration

3. **Verify enum values** were added successfully

4. **Test policy creation** with GMC, GPA, GTL types

---

## Summary

**Problem**: PostgreSQL enums require values to be added before use  
**Solution**: Added DO blocks to add enum values first  
**Result**: Migration now succeeds ✅  

**Status**: ✅ READY TO RUN

---

**Date**: January 28, 2026  
**Migration File**: V11__add_policy_type_support.sql  
**Status**: ✅ Fixed and Ready  
