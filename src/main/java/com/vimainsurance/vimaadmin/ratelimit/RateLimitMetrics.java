package com.vimainsurance.vimaadmin.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * F-04 — Micrometer counter for throttled requests (SOC / CloudWatch via actuator export when enabled).
 */
@Component
public class RateLimitMetrics {

    public static final String COUNTER_NAME = "vima.ratelimit.throttled";

    private final MeterRegistry meterRegistry;

    public RateLimitMetrics(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordThrottled(RateLimitScope scope, String uriPrefix) {
        if (meterRegistry == null) {
            return;
        }
        String safePrefix = sanitizeUriPrefix(uriPrefix);
        meterRegistry
                .counter(COUNTER_NAME, "scope", scope.name(), "uri_prefix", safePrefix)
                .increment();
    }

    static String sanitizeUriPrefix(String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        String path = RateLimitPathClassifier.stripContextPath(uri);
        if (path == null) {
            return "unknown";
        }
        if (path.startsWith("/api/v1/auth/")) {
            return "/api/v1/auth";
        }
        if (path.startsWith("/api/v1/enrollment/")) {
            return "/api/v1/enrollment";
        }
        if (path.startsWith("/api/v1/enrollment-submissions/")) {
            return "/api/v1/enrollment-submissions";
        }
        if (path.startsWith("/api/v1/enrollments/")) {
            return "/api/v1/enrollments";
        }
        if (path.contains("/logins/") || path.contains("password-reset")) {
            return "/api/v1/sensitive-logins";
        }
        return "/api/v1/other";
    }
}
