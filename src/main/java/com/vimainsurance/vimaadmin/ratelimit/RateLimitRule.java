package com.vimainsurance.vimaadmin.ratelimit;

/**
 * Resolved rate-limit tier and requests-per-minute for a request path.
 */
public record RateLimitRule(RateLimitScope scope, int requestsPerMinute) {}
