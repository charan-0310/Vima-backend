package com.vimainsurance.vimaadmin.ratelimit;

/**
 * F-04 rate-limit tiers. Precedence when classifying a request path: AUTH &gt; ENROLLMENT &gt; SENSITIVE &gt; DEFAULT.
 */
public enum RateLimitScope {
    AUTH,
    ENROLLMENT,
    SENSITIVE,
    DEFAULT
}
