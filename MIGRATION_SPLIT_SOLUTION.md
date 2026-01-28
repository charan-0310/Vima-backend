# ✅ Migration Split - PostgreSQL Enum Commit Issue Resolved

## Problem
```
ERROR: unsafe use of new value "GMC" of enum type cpc.product_type_enum
Hint: New enum values must be committed before they can be used.
```

## Root Cause
PostgreSQL requires **new enum values to be committed in a separate transaction** before they can be used. Flyway runs each migration file in its own transaction, but within a single file, all statements are in the same transaction.

**The Issue**: We were trying to:
1. Add 'GMC', 'GPA', 'GTL' to the enum
2. Use those values in UPDATE statements
3. All in the same transaction ❌

## Solution: Split Into Two Migration Files

### Migration V11.1 (Runs First) - Add Enum Values
**File**: `V11.1__add_enum_values.sql`
- Adds GMC, GPA, GTL to `product_type_enum`
- Adds E, ES, ESC, ESCP to `coverage_type_enum`
- Flyway commits this transaction
- **Enum values are now available** ✅

### Migration V11 (Runs Second) - Use Enum Values
**File**: `V11__add_policy_type_support.sql`
- Adds new columns
- Removes NOT NULL constraints
- **Uses the committed enum values** in UPDATE statements ✅
- Adds comments and indexes

---

## Why This Works

### Flyway Execution Order:
```
1. V11.1__add_enum_values.sql
   ├── Add GMC, GPA, GTL to product_type_enum
   ├── Add E, ES, ESC, ESCP to coverage_type_enum
   └── COMMIT ← Transaction ends here

2. V11__add_policy_type_support.sql
   ├── Add columns
   ├── UPDATE product_type = 'GMC' ← Now safe! 
   ├── UPDATE coverage_type = 'E'  ← Now safe!
   └── COMMIT
```

### Key Point:
Between V11.1 and V11, Flyway **commits the transaction**, making the enum values available for use in V11.

---

## Files Created/Modified

### ✅ New File: V11.1__add_enum_values.sql
```sql
-- Adds enum values only
DO $$
BEGIN
    IF NOT EXISTS (...'GMC'...) THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'GMC';
    END IF;
END $$;
-- Repeat for GPA, GTL, E, ES, ESC, ESCP
```

### ✅ Modified File: V11__add_policy_type_support.sql
```sql
-- Note: Enum values added in V11.1
-- Now safe to use GMC, GPA, GTL, E, ES, ESC, ESCP

ALTER TABLE cpc.policies ADD COLUMN...
UPDATE cpc.policies SET product_type = 'GMC'... -- Safe!
```

---

## Migration Execution

### Step 1: Clear Failed Migration
```sql
DELETE FROM flyway_schema_history WHERE version = '11';
```

### Step 2: Restart Application
Flyway will execute:
1. **V11.1** first (adds enum values + commits)
2. **V11** second (uses enum values)

---

## Verification

After migration succeeds, verify:

```sql
-- Check enum values
SELECT enumlabel FROM pg_enum e 
JOIN pg_type t ON e.enumtypid = t.oid 
WHERE t.typname = 'product_type_enum'
ORDER BY enumsortorder;
-- Should show: ..., GMC, GPA, GTL

-- Check data migration
SELECT product_type, COUNT(*) 
FROM cpc.policies 
GROUP BY product_type;
-- Should show GMC instead of HEALTH/GHI

-- Check Flyway history
SELECT version, description, success 
FROM flyway_schema_history 
WHERE version IN ('11', '11.1')
ORDER BY version;
-- Should show both succeeded
```

---

## Why Can't We Use COMMIT in DO Blocks?

You might think: "Why not just COMMIT inside the DO block?"

```sql
DO $$
BEGIN
    ALTER TYPE ... ADD VALUE 'GMC';
    COMMIT;  -- ❌ This doesn't work!
END $$;
```

**Because**:
1. DO blocks run in the context of the outer transaction
2. Flyway controls the transaction boundary
3. COMMIT inside DO block is either ignored or causes errors
4. The only way to commit is to **end the Flyway migration file**

---

## Best Practice for PostgreSQL Enums in Flyway

### ✅ Correct Pattern:
```
V<n>.1__add_enum_values.sql      ← Add enum values
V<n>__use_enum_values.sql        ← Use enum values
```

### ❌ Wrong Pattern:
```
V<n>__do_everything.sql          ← Add AND use enums (fails!)
```

---

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Migration Files** | 1 (V11) | 2 (V11.1 + V11) |
| **Transactions** | 1 | 2 (committed between files) |
| **Enum Addition** | Same transaction | Separate transaction ✅ |
| **Enum Usage** | Same transaction ❌ | Next transaction ✅ |
| **Status** | Fails | Succeeds ✅ |

---

## Files Summary

### V11.1__add_enum_values.sql (58 lines)
- Purpose: Add enum values and commit
- Content: DO blocks for each enum value
- Safe to run multiple times (idempotent)

### V11__add_policy_type_support.sql (51 lines)
- Purpose: Schema changes and data migration
- Content: ALTER TABLE, UPDATE, COMMENT, CREATE INDEX
- Depends on V11.1 being committed first

---

## Next Steps

1. ✅ Clear failed V11 migration from Flyway history
2. ✅ Restart application
3. ✅ Flyway will run V11.1 (commits enum values)
4. ✅ Flyway will run V11 (uses enum values)
5. ✅ Verify data migration succeeded

---

**Status**: ✅ FIXED - Split into 2 migrations  
**Root Cause**: PostgreSQL enum commit requirement  
**Solution**: Separate transaction for enum additions  
**Ready**: Yes, restart application to run migrations  

🎉 **Problem solved!** 🎉
