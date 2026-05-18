package com.vimainsurance.vimaadmin.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimitExemptIpMatcherTest {

    @Test
    void exactIpMatch() {
        RateLimitExemptIpMatcher matcher = new RateLimitExemptIpMatcher("203.0.113.50,10.0.0.1");
        assertTrue(matcher.isExempt("203.0.113.50"));
        assertFalse(matcher.isExempt("203.0.113.51"));
    }

    @Test
    void cidrMatch() {
        RateLimitExemptIpMatcher matcher = new RateLimitExemptIpMatcher("10.0.0.0/8");
        assertTrue(matcher.isExempt("10.1.2.3"));
        assertFalse(matcher.isExempt("192.168.1.1"));
    }

    @Test
    void emptyConfigMatchesNothing() {
        RateLimitExemptIpMatcher matcher = new RateLimitExemptIpMatcher("");
        assertFalse(matcher.isExempt("10.0.0.1"));
    }
}
