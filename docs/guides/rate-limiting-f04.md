# F-04 — Global rate limiting

> **Finding:** IRDAI P0 F-04 (ISO 27001 A.8.6, A.8.20)  
> **Implementation:** `GlobalRateLimitFilter` + optional `@RateLimit` aspect

## Purpose

Protect the Vima backend API from abuse (credential stuffing, enrollment token brute force, general DoS) using **per-IP token buckets** (Bucket4j). Limits apply at the **servlet filter** layer so all `/api/**` routes are covered without annotating individual controllers.

## Architecture

```
Request → GlobalRateLimitFilter (@Order HIGHEST_PRECEDENCE + 20)
            → classify URI → scope (AUTH | ENROLLMENT | SENSITIVE | DEFAULT)
            → bucket key = scope + client IP
            → allow (X-RateLimit-* headers) or 429 + Retry-After
          → Spring Security / controllers
```

**Skipped paths** (not rate limited): `/health`, `/actuator/**`, Swagger/OpenAPI, `/public/**`, and any URI that does not contain `/api/`.

**Filter order:** Runs early; does not replace Keycloak brute-force or AWS WAF (defence in depth).

## Scope precedence

Longest / highest-precedence match after stripping the servlet context path (`/dev`, `/prod`, `/stage`, `/uat`, `/local`, `/test`):

| Scope | Path patterns | Default RPM (prod) |
|-------|---------------|-------------------|
| **AUTH** | `/api/v1/auth/**` | 10 |
| **ENROLLMENT** | `/api/v1/enrollment/**`, `enrollment-submissions/**`, `enrollments/**` | 20 |
| **SENSITIVE** | contains `/logins/create`, `/logins/resend-welcome`, `/send-password-reset` | 10 |
| **DEFAULT** | all other `/api/**` | 120 |

F-03 BFF endpoints (`POST /api/v1/auth/session`, `/refresh`, `/logout`) are covered by **AUTH** automatically.

## Configuration

| Property | Description | Prod default |
|----------|-------------|--------------|
| `ratelimit.enabled` | Master switch | `true` |
| `ratelimit.default.requests-per-min` | DEFAULT scope | `120` |
| `ratelimit.enrollment.requests-per-min` | ENROLLMENT scope | `20` |
| `ratelimit.auth.requests-per-min` | AUTH scope | `10` |
| `ratelimit.sensitive.requests-per-min` | SENSITIVE scope | `10` |
| `ratelimit.exempt-ips` | Comma-separated exact IPv4/IPv6 or IPv4 CIDR | empty |
| `ratelimit.cache.max-entries` | Max distinct IP×scope buckets (Caffeine) | `50000` |
| `ratelimit.cache.expire-after-access-minutes` | Idle eviction for buckets | `10` |

Profiles: see `application-prod.properties`, `application-dev.properties`, `application-stage.properties`.

For local E2E against a shared dev API, consider temporarily raising limits or setting `ratelimit.enabled=false` on a **local-only** profile — never disable in production.

## Response headers

| Status | Headers |
|--------|---------|
| **200** (allowed) | `X-RateLimit-Limit`, `X-RateLimit-Remaining` |
| **429** (throttled) | `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining: 0`, JSON body |

## Metrics and logging

- **Counter:** `vima.ratelimit.throttled` (tags: `scope`, `uri_prefix`) when a `MeterRegistry` bean is available (requires `micrometer-core`; export via actuator/CloudWatch if configured).
- **Logs:** `WARN` on throttle with `correlationId`, scope, IP, URI, `retryAfter`.

## Secondary limiters

| Mechanism | Use case |
|-----------|----------|
| `@RateLimit` + `RateLimitAspect` | Method-level cap (e.g. enrollment token validation `3`/min per IP); bounded Caffeine cache |
| `EnrollmentTokenRateLimitService` | Premium calculation `100`/min **per enrollment token** |

These stack on top of the global filter.

## Multi-node limitation

Buckets are **in-memory per JVM**. Vima currently runs one backend instance per environment; limits are effective per instance.

When scaling horizontally:

1. Set `vima.deploy.instance-count` and migrate to Redis-backed Bucket4j (phase 2 — see remediation plan F-04).
2. Keep AWS WAF rate rules (F-25) as edge protection.

## Verification (operations)

Auth tier smoke test (invalid body is fine):

```bash
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code} " \
    -X POST "https://api.vimainsurance.com/prod/api/v1/auth/me" \
    -H "Content-Type: application/json"
done
echo
```

Expected: responses succeed until the AUTH bucket is exhausted, then `429`. Tune `ratelimit.auth.requests-per-min` if Keycloak already enforces stricter login throttling.

Public enrollment path alternative:

```bash
for i in $(seq 1 25); do
  curl -s -o /dev/null -w "%{http_code} " \
    "https://api.vimainsurance.com/prod/api/v1/enrollment/invalid-token"
done
```

## Code map

| Component | Package / path |
|-----------|----------------|
| Filter | `config.GlobalRateLimitFilter` |
| Path classifier | `ratelimit.RateLimitPathClassifier` |
| Exempt IPs | `ratelimit.RateLimitExemptIpMatcher` |
| Buckets | `ratelimit.RateLimitBucketFactory` |
| Metrics | `ratelimit.RateLimitMetrics` |
| Tests | `RateLimitPathClassifierTest`, `GlobalRateLimitFilterTest` |

## Related docs

- [IRDAI P0 Manual Env Todo §7](../../../vima-web-portal/docs/prd/audit/IRDAI_P0_Manual_Env_Todo.md)
- [Keycloak auth architecture](../../../vima-web-portal/docs/architecture/auth/keycloak.md)
- [Token security implementation plan](../implementation/security/token-security-implementation-plan.md) (enrollment token limits)
