# Postman Testing Guide - Token Security

## What to Test

Since the implementation has no public endpoints yet, you need to test the **services directly** or wait for BE-005/BE-006 to be implemented.

---

## Current Status

✅ **Implemented (Backend Services)**:
- `TokenSecurityService` - Token generation, hashing, verification
- `EnrollmentTokenService` - Token validation logic
- `RateLimitAspect` - Rate limiting with Bucket4j
- Exception handling for all error scenarios

❌ **Not Yet Implemented (No Endpoints)**:
- No public `/api/v1/enrollment/{token}/validate` endpoint
- No admin invitation sending endpoint
- No enrollment flow endpoints

---

## How to Test Now

### Option 1: Wait for BE-006 (Recommended)

BE-006 will create the public enrollment endpoint:
```
GET /api/v1/enrollment/{token}/validate
```

Once that's implemented, you can test with Postman.

---

### Option 2: Create a Quick Test Endpoint (Manual)

Add this to any existing controller temporarily:

```java
@RestController
@RequestMapping("/api/test")
public class QuickTestController {
    
    @Autowired
    private TokenSecurityService tokenSecurityService;
    
    @Autowired
    private IEnrollmentTokenService enrollmentTokenService;
    
    // Test 1: Generate token and hash
    @GetMapping("/generate-token")
    public Map<String, String> generateToken() {
        String token = tokenSecurityService.generateToken();
        String hash = tokenSecurityService.hashToken(token);
        
        return Map.of(
            "token", token,
            "hash", hash,
            "note", "Store this hash in enrollment_invitations.token_hash"
        );
    }
    
    // Test 2: Validate token
    @GetMapping("/validate/{token}")
    public EnrollmentContext validateToken(
        @PathVariable String token,
        HttpServletRequest request
    ) {
        return enrollmentTokenService.validateToken(token, request);
    }
}
```

---

## What to Test Using Postman (After Endpoints Exist)

### Test 1: Token Generation ✅
**Endpoint**: Will be part of BE-005 (admin invitation sending)

**Expected**:
- Generates 43-character URL-safe token
- Creates SHA-256 hash (64 chars)
- Stores hash in database

---

### Test 2: Token Validation (Happy Path) ✅
**Endpoint**: `GET /api/v1/enrollment/{token}/validate`

**Request**:
```
GET http://localhost:8080/api/v1/enrollment/VQ8h2kX9zA.../validate
```

**Expected Response** (200 OK):
```json
{
  "rawToken": "VQ8h2kX9zA...",
  "invitationId": "uuid",
  "expiresAt": "2026-02-11T10:30:00",
  "daysRemaining": 7,
  "employeeId": "uuid",
  "employeeName": "John Doe",
  "employeeEmail": "john@example.com",
  "windowId": "uuid",
  "windowName": "Annual Enrollment 2026",
  "submissionId": "uuid",
  "submissionStatus": "DRAFT"
}
```

**What to Verify**:
- ✅ Returns complete enrollment context
- ✅ `rawToken` is included (for subsequent API calls)
- ✅ `submissionId` is created (new draft)
- ✅ First access changes invitation status to "OPENED"

---

### Test 3: Invalid Token ❌
**Request**:
```
GET http://localhost:8080/api/v1/enrollment/invalid-token-123/validate
```

**Expected Response** (400 Bad Request):
```json
{
  "status": "error",
  "message": "Invalid or unknown enrollment token"
}
```

---

### Test 4: Expired Token ⏰
**Setup**: Manually set `expires_at` to past in database

**Request**:
```
GET http://localhost:8080/api/v1/enrollment/{expired-token}/validate
```

**Expected Response** (410 Gone):
```json
{
  "status": "error",
  "message": "This enrollment link has expired. Please contact your HR department."
}
```

---

### Test 5: Already Completed ✔️
**Setup**: Manually set invitation status to "COMPLETED"

**Request**:
```
GET http://localhost:8080/api/v1/enrollment/{completed-token}/validate
```

**Expected Response** (409 Conflict):
```json
{
  "status": "error",
  "message": "Enrollment has already been completed for this invitation."
}
```

---

### Test 6: Window Not Active 🚫
**Setup**: Manually set window status to "CLOSED" or "SCHEDULED"

**Request**:
```
GET http://localhost:8080/api/v1/enrollment/{token}/validate
```

**Expected Response** (403 Forbidden):
```json
{
  "status": "error",
  "message": "The enrollment window is not currently active. Status: CLOSED"
}
```

---

### Test 7: Rate Limiting 🚦
**Request** (Call 4 times within 1 minute):
```
GET http://localhost:8080/api/v1/enrollment/{token}/validate
GET http://localhost:8080/api/v1/enrollment/{token}/validate
GET http://localhost:8080/api/v1/enrollment/{token}/validate
GET http://localhost:8080/api/v1/enrollment/{token}/validate  # This should fail
```

**Expected Response for 4th Request** (429 Too Many Requests):
```json
{
  "status": "error",
  "message": "Too many requests. Please retry after 45 seconds."
}
```

**What to Verify**:
- ✅ First 3 requests succeed
- ✅ 4th request fails with 429
- ✅ After 1 minute, requests work again
- ✅ Different IPs have independent limits

---

## Database Verification

After testing, check the database:

### Check Invitation Status Changed
```sql
SELECT id, status, opened_at, sent_at 
FROM cpc.enrollment_invitations 
WHERE token_hash = '<your-hash>';

-- First access: status should change from 'SENT' to 'OPENED'
-- opened_at should be set
```

### Check Draft Submission Created
```sql
SELECT id, status, employee_id, created_at 
FROM cpc.enrollment_submissions 
WHERE employee_id = '<employee-uuid>';

-- Should have one record with status='DRAFT'
```

---

## Postman Collection Structure

Create a collection with these folders:

```
📁 Enrollment Token Security
  📁 1. Happy Path
    └─ Validate Token (Valid)
  
  📁 2. Error Scenarios
    └─ Invalid Token
    └─ Expired Token
    └─ Already Completed
    └─ Window Not Active
  
  📁 3. Rate Limiting
    └─ Rate Limit Test (Call 4x)
```

---

## Environment Variables

Set these in Postman:

```
BASE_URL = http://localhost:8080
TOKEN = <paste-token-from-database>
EMPLOYEE_ID = <your-test-employee-uuid>
WINDOW_ID = <your-test-window-uuid>
```

---

## Current Testing Status

**What You Can Test Now**: ❌ Nothing (no endpoints yet)

**What to Test After BE-006**: ✅ Everything above

**Estimated Time**: BE-006 will take ~3-4 hours to implement

---

## Next Steps

1. **Wait for BE-006** - Token validation endpoint will be created
2. **Or manually add a test endpoint** - Use the code snippet above
3. **Once endpoint exists** - Follow the Postman tests above
4. **Verify database changes** - Check invitation/submission status

---

## Quick Reference

| Test | HTTP Status | Error Message |
|------|-------------|---------------|
| Valid token | 200 OK | Success |
| Invalid token | 400 Bad Request | "Invalid or unknown..." |
| Expired token | 410 Gone | "...has expired" |
| Already completed | 409 Conflict | "...already been completed" |
| Window closed | 403 Forbidden | "...not currently active" |
| Rate limit exceeded | 429 Too Many Requests | "Too many requests. Retry..." |

---

That's it! Simple and straightforward once the endpoints are created.
