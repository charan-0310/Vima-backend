package com.vimainsurance.vimaadmin.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RateLimitPathClassifierTest {

    private static final int AUTH = 10;
    private static final int ENROLL = 20;
    private static final int SENSITIVE = 10;
    private static final int DEFAULT = 120;

    @Test
    void classify_authWithContextPath() {
        RateLimitRule rule = RateLimitPathClassifier.classify(
                "/prod/api/v1/auth/me", AUTH, ENROLL, SENSITIVE, DEFAULT);
        assertEquals(RateLimitScope.AUTH, rule.scope());
        assertEquals(AUTH, rule.requestsPerMinute());
    }

    @Test
    void classify_enrollmentWithContextPath() {
        RateLimitRule rule = RateLimitPathClassifier.classify(
                "/dev/api/v1/enrollment/abc-token", AUTH, ENROLL, SENSITIVE, DEFAULT);
        assertEquals(RateLimitScope.ENROLLMENT, rule.scope());
        assertEquals(ENROLL, rule.requestsPerMinute());
    }

    @Test
    void classify_sensitiveLoginCreate() {
        RateLimitRule rule = RateLimitPathClassifier.classify(
                "/prod/api/v1/organization/org-id/employees/logins/create",
                AUTH,
                ENROLL,
                SENSITIVE,
                DEFAULT);
        assertEquals(RateLimitScope.SENSITIVE, rule.scope());
    }

    @Test
    void classify_authPrecedenceOverEnrollment() {
        RateLimitRule rule = RateLimitPathClassifier.classify(
                "/api/v1/auth/session", AUTH, ENROLL, SENSITIVE, DEFAULT);
        assertEquals(RateLimitScope.AUTH, rule.scope());
    }

    @Test
    void classify_defaultApiPath() {
        RateLimitRule rule = RateLimitPathClassifier.classify(
                "/prod/api/v1/organizations", AUTH, ENROLL, SENSITIVE, DEFAULT);
        assertEquals(RateLimitScope.DEFAULT, rule.scope());
        assertEquals(DEFAULT, rule.requestsPerMinute());
    }

    @Test
    void stripContextPath_removesProdPrefix() {
        assertEquals("/api/v1/auth/me", RateLimitPathClassifier.stripContextPath("/prod/api/v1/auth/me"));
    }
}
