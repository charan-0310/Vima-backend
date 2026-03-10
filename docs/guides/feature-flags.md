# Feature Flags — How They Work and How to Add a New Feature

This document explains how feature flags work across the Vima backend and web portal, and the **correct steps** to add a new feature so it appears on the Feature Flag management page and stays connected to navigation and the `auth/me` API.

---

## 1. How Feature Flags Work

### 1.1 Backend Data Model

- **`admin.feature_flags`**  
  Master list of flags. Columns: `flag_id` (UUID), `flag_key` (unique, e.g. `group-insurance.claims`), `description`, `parent_flag_id` (optional), `is_active` (global default).

- **`admin.feature_flag_roles`**  
  Per-role permission: which roles get which flags, with `is_active` and `actions` (READ, WRITE, APPROVE).  
  **A flag must have at least one row here** to be toggleable on the Feature Flag management page and to be returned as “enabled” for a user in `auth/me`.

- **`admin.feature_flag_companies`**  
  Per-organization overrides (optional): enable/disable a flag for specific organizations.

### 1.2 Flow: From DB to Frontend

1. **Login / session**  
   Frontend calls **`GET /api/v1/auth/me`** (see `AuthController.getFeatureFalgs()`).  
   Backend uses `FeatureFlagService.findAllMatchedFeatureFlags()`:
   - Loads **all** rows from `admin.feature_flags`.
   - For each flag, checks the current user’s **roles** and **organization IDs** (from `TenantContext`) against `feature_flag_roles` and `feature_flag_companies`.
   - Builds a list of `FeatureFlagResponseDto`: `flag_key`, `is_active`/`is_enabled`, `actions`, etc.
   - A flag is **enabled** for the user only if it has at least one matching role (or company) with `is_active = true`, or if the user is SUPER_ADMIN.

2. **Frontend**  
   - `authMeService.fetchUserFeaturePermissions()` calls `auth/me` and maps the payload to `UserFeaturePermission[]`.
   - `useAuthStore.setFeaturePermissions(permissions)` stores them and builds:
     - `featureFlags`: `Set<string>` of **active** `flag_key` values.
     - `actionPermissions`: `Map<flag_key, actions>`.
   - **Sidebar** (e.g. `AppSidebar.tsx`): for each nav item, it uses `hasFeatureAccess(item.flagKey)` which is `featureFlags.has(flagKey)`.  
     So the **exact `flag_key`** in the backend must match the **`flagKey`** in the frontend nav config; otherwise the menu item never shows as allowed from feature flags.

3. **Feature Flag management page**  
   - **By role:** `GET /api/v1/admin/features/roles` → `FeatureFlagService.getFeatureFlagsGroupedByType()`.  
     It uses `findAllWithRoles()` which returns only **parent** flags (no `parent_flag_id`). For each parent it loads roles and **sub-features** via `findSubFeatureFlagsByParentId`.  
     Flags that have **no** `feature_flag_roles` rows never get a “role” in the first pass; they are then added in the “fill missing” loop as **disabled** for every role. So they appear on the page but cannot be turned on until you add `feature_flag_roles` for them.
   - **By organization:** `GET /api/v1/admin/features/organizations` → grouped by organization (uses `feature_flag_companies`).
   - **Updates:**  
     - Toggling a flag for a **role**: `POST /api/v1/admin/features/roles?roleName=ROLE_XXX` with body `FeatureFlagUpdateDto` (list of `{ id: flagId, enabled, actions }`).  
     - Backend creates or updates rows in `feature_flag_roles` (see `updateFeatureFlagRoles`).  
     - If the frontend then refetches `auth/me` and calls `setFeaturePermissions`, the user’s sidebar and page access reflect the new state. So “when I disable the flag in frontend it updates backend and in me API it sends updated data and does not show that particular page” is correct: the UI updates the backend, then the next `auth/me` returns the updated list and the frontend hides the page/menu when the flag is off.

### 1.3 Summary

- **Backend** defines flags in `admin.feature_flags` and who can use them in `admin.feature_flag_roles` (and optionally `admin.feature_flag_companies`).
- **`auth/me`** returns the list of flags with `is_active`/`is_enabled` per current user (roles + orgs).
- **Frontend** stores that list and uses `flag_key` for:
  - Showing/hiding nav items (`hasFeatureAccess(flagKey)`).
  - Optional route guards (same key).
- **Feature management page** reads/updates the same tables; after an update, refreshing `auth/me` keeps the UI in sync.

---

## 2. Steps to Add a New Feature Correctly

Follow these in order so the new feature appears on the Feature Flag page and stays connected to navigation and `auth/me`.

### Step 1: Backend — Migration: Insert the flag

Add a Flyway migration under `src/main/resources/db/migration/` (e.g. `V29__feature_flag_my_feature.sql`):

```sql
-- Optional: if the feature is under a parent (e.g. group-insurance), use parent_flag_id.
-- Get parent flag_id from admin.feature_flags (e.g. group-insurance = '11112222-3333-4444-5555-666677778888').

INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES (
  gen_random_uuid(),           -- or a fixed UUID for idempotency
  'my-feature-key',            -- must match frontend flagKey exactly
  'Short description for admins',
  NULL,                         -- or parent flag_id if child
  FALSE
);
```

- Use a **single, canonical `flag_key`** (e.g. `my-feature-key` or `group-insurance.my-feature`). The frontend will use this exact string.

### Step 2: Backend — Migration: Wire roles

**Without this, the flag will appear on the Feature Flag page as disabled for all roles and will never be returned as enabled from `auth/me`.**

Add to the same migration (or a second one) inserts into `admin.feature_flag_roles`:

```sql
-- Example: enable for ROLE_VIMA_ADMIN and ROLE_HR_ADMIN
-- Replace <flag_id_uuid> with the flag_id from the INSERT above (or use a subquery).

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES
  (gen_random_uuid(), '<flag_id_uuid>', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]),
  (gen_random_uuid(), '<flag_id_uuid>', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE']::permission_action[]);
```

- If you use `gen_random_uuid()` for `flag_id` in Step 1, you must use a subquery to get that `flag_id` in Step 2, e.g.:

```sql
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), f.flag_id, 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]
FROM admin.feature_flags f WHERE f.flag_key = 'my-feature-key';
```

### Step 3: Frontend — Navigation and access

1. **Add the nav item** in `src/config/featureFlagNavigation.ts`:
   - Add an entry in the right group with:
     - `flagKey`: **exactly** the same as `flag_key` in the DB (e.g. `my-feature-key`).
     - `url`: route path (e.g. `/admin/my-feature`).
     - `title`, `icon`, `fallbackRoles` as needed.
2. **Sidebar:** No code change needed if you use the same `flagKey`; `hasFeatureAccess(flagKey)` will use the data from `auth/me`.
3. **Optional — Route guard:** If you want to block the route when the flag is off, use `getFlagKeyFromRoute(route)` and `hasFeatureAccess(flagKey)` in your route wrapper (see existing patterns).

### Step 4: Optional — Use the flag inside a page

- Anywhere you need to branch on the feature: `hasFeatureAccess('my-feature-key')` or `getFeatureActions('my-feature-key')` from `useAuthStore`.

### Step 5: Verify end-to-end

1. Run migrations; confirm the new row(s) in `admin.feature_flags` and `admin.feature_flag_roles`.
2. Open the Feature Flag management page: the new feature should appear under the relevant role(s) and be toggleable.
3. Toggle off for your role → save → ensure frontend refetches `auth/me` (or re-login). The menu item for that feature should disappear (and the page should be inaccessible if you guard the route).
4. Toggle on again → after refresh/refetch, the menu and page should be visible again.

---

## 3. What Was Wrong with “Claims” (and How to Fix It)

From the current migrations and code:

- **V2** defines **`group-insurance.claims`** (child of `group-insurance`) and **V3** adds `feature_flag_roles` for it (e.g. ROLE_VIMA_ADMIN, ROLE_HR_ADMIN). So that flag is fully wired and works for the **Group Insurance → company → Claims tab** (CompanyDetails uses `hasFeatureAccess('group-insurance.claims')`).
- **V28** adds a **standalone** flag **`claims`** (no `parent_flag_id`) with **no** corresponding inserts in **`feature_flag_roles`**. So:
  - The `claims` flag appears on the Feature Flag page (in the “fill missing” list) as **disabled** for every role and cannot be turned on.
  - It can be returned by `auth/me` only as disabled (no matching role has it enabled).
- **Frontend** uses **`claims-management`** as the nav `flagKey` for “Claims Management” in the sidebar. There is **no** `flag_key` **`claims-management`** in the backend. So:
  - The sidebar does **not** use feature flags for that item: `AppSidebar` has a special case so that for `claims-management` it only checks role (VIMA_ADMIN or HR_ADMIN). So the Claims Management menu item does not react to toggling any feature flag.

**Missing connections:**

1. **Flag key mismatch**  
   Backend has `group-insurance.claims` and `claims`; frontend nav uses `claims-management`. So even if you enable a claim-related flag in the backend, the sidebar does not use it for the main “Claims Management” link (by design of the current special case).

2. **Standalone `claims` flag has no roles**  
   So it never appears as “on” on the Feature Flag page and never turns on in `auth/me`. To fix: add a migration that inserts into `admin.feature_flag_roles` for the `claims` flag (same pattern as Step 2 above), for the roles that should see it.

3. **Unify naming and behavior (recommended)**  
   - Either:
     - Use **one** flag for the main Claims Management nav: e.g. add a flag with `flag_key = 'claims-management'` and wire `feature_flag_roles`, and in the frontend remove the sidebar special case so `hasFeatureAccess('claims-management')` controls visibility;  
     - Or keep using **`group-insurance.claims`** for both the Group Insurance company tab and the top-level Claims Management nav: add a nav item with `flagKey: 'group-insurance.claims'` for the Claims Management entry and remove the special case, so one flag controls both.
   - Optionally remove or repurpose the standalone `claims` flag to avoid two overlapping flags (e.g. rename to something else or drop it and use only `group-insurance.claims` / `claims-management`).

---

## 4. Checklist for a New Feature

- [ ] **Backend:** Migration inserts into `admin.feature_flags` with the exact `flag_key` you will use in the frontend.
- [ ] **Backend:** Migration inserts into `admin.feature_flag_roles` for every role that should see the feature (otherwise it stays disabled for everyone).
- [ ] **Frontend:** `featureFlagNavigation.ts` has an item with `flagKey` **identical** to backend `flag_key`.
- [ ] **Frontend:** No hardcoded role-only bypass for this feature in the sidebar (or document why it’s intentional).
- [ ] **Verify:** Feature appears on Feature Flag page and can be toggled; `auth/me` and nav visibility update after toggling.

---

## 5. Key Files Reference

| Layer   | File / Endpoint | Purpose |
|--------|------------------|---------|
| Backend | `db/migration/V1__feature_flags.sql` | Schema: feature_flags, feature_flag_roles, feature_flag_companies |
| Backend | `db/migration/V2__feature_flags_insert.sql` | Initial + group-insurance.claims flags |
| Backend | `db/migration/V3__feature_flags_roles_insert.sql` | Initial role mappings |
| Backend | `AuthController.getFeatureFalgs()` → `GET /api/v1/auth/me` | Returns user’s flags for frontend |
| Backend | `FeatureFlagServiceImpl.findAllMatchedFeatureFlags()` | Builds auth/me payload from roles/orgs |
| Backend | `FeatureFlagServiceImpl.getFeatureFlagsGroupedByType()` | Feature Flag page by role |
| Backend | `FeatureFlagServiceImpl.updateFeatureFlagRoles()` | Persists role toggles |
| Backend | `FeatureManagementController` | GET/POST `/api/v1/admin/features/roles` and `.../organizations` |
| Frontend | `src/config/featureFlagNavigation.ts` | Nav items and `flagKey` ↔ route |
| Frontend | `src/stores/useAuthStore.ts` | `featurePermissions`, `hasFeatureAccess(flagKey)` |
| Frontend | `src/services/authMeService.ts` | Fetches `auth/me` and maps to permissions |
| Frontend | `src/components/AppSidebar.tsx` | Uses `hasFeatureAccess(item.flagKey)` (and special cases) |
| Frontend | `src/hooks/queries/useFeaturePermissions.ts` | Mutations that call admin APIs then refetch `auth/me` |

Using this flow and checklist ensures new features are controlled by the Feature Flag page and stay in sync with `auth/me` and the UI.
