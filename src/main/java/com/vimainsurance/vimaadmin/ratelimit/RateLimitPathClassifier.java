package com.vimainsurance.vimaadmin.ratelimit;

import java.util.List;

/**
 * F-04 — Maps request URIs to rate-limit scope (highest-precedence match).
 */
public final class RateLimitPathClassifier {

    private static final List<String> CONTEXT_PREFIXES = List.of(
            "/dev", "/prod", "/uat", "/stage", "/local", "/test");

    private RateLimitPathClassifier() {}

    /**
     * @param requestUri full servlet path (may include context path segment e.g. {@code /prod/api/...})
     * @param authRpm requests per minute for {@link RateLimitScope#AUTH}
     * @param enrollmentRpm requests per minute for {@link RateLimitScope#ENROLLMENT}
     * @param sensitiveRpm requests per minute for {@link RateLimitScope#SENSITIVE}
     * @param defaultRpm requests per minute for {@link RateLimitScope#DEFAULT}
     */
    public static RateLimitRule classify(
            String requestUri,
            int authRpm,
            int enrollmentRpm,
            int sensitiveRpm,
            int defaultRpm) {
        String path = stripContextPath(requestUri);
        if (path == null || path.isEmpty()) {
            return new RateLimitRule(RateLimitScope.DEFAULT, defaultRpm);
        }

        if (path.startsWith("/api/v1/auth/") || "/api/v1/auth".equals(path)) {
            return new RateLimitRule(RateLimitScope.AUTH, authRpm);
        }

        if (path.startsWith("/api/v1/enrollment/")
                || path.startsWith("/api/v1/enrollment-submissions/")
                || path.startsWith("/api/v1/enrollments/")) {
            return new RateLimitRule(RateLimitScope.ENROLLMENT, enrollmentRpm);
        }

        if (isSensitivePath(path)) {
            return new RateLimitRule(RateLimitScope.SENSITIVE, sensitiveRpm);
        }

        return new RateLimitRule(RateLimitScope.DEFAULT, defaultRpm);
    }

    static String stripContextPath(String uri) {
        if (uri == null) {
            return null;
        }
        String path = uri;
        for (String prefix : CONTEXT_PREFIXES) {
            if (path.startsWith(prefix + "/")) {
                return path.substring(prefix.length());
            }
        }
        return path;
    }

    private static boolean isSensitivePath(String path) {
        return path.contains("/logins/create")
                || path.contains("/logins/resend-welcome")
                || path.contains("/send-password-reset");
    }
}
