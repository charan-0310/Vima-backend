package com.vimainsurance.vimaadmin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.vimainsurance.vimaadmin.ratelimit.RateLimitMetrics;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitScope;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class GlobalRateLimitFilterTest {

    private final AtomicInteger throttledEvents = new AtomicInteger();
    private GlobalRateLimitFilter filter;
    private CountingFilterChain chain;

    @BeforeEach
    void setUp() {
        RateLimitMetrics metrics = new RateLimitMetrics(null) {
            @Override
            public void recordThrottled(RateLimitScope scope, String uriPrefix) {
                throttledEvents.incrementAndGet();
            }
        };
        filter = new GlobalRateLimitFilter(metrics);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "defaultRpm", 120);
        ReflectionTestUtils.setField(filter, "enrollmentRpm", 20);
        ReflectionTestUtils.setField(filter, "authRpm", 3);
        ReflectionTestUtils.setField(filter, "sensitiveRpm", 10);
        ReflectionTestUtils.setField(filter, "exemptIps", "");
        ReflectionTestUtils.setField(filter, "maxCacheEntries", 10_000L);
        ReflectionTestUtils.setField(filter, "expireAfterAccessMinutes", 10L);
        filter.init();
        chain = new CountingFilterChain();
        throttledEvents.set(0);
    }

    @Test
    void authPathReturns429AfterLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prod/api/v1/auth/me");
        request.setRemoteAddr("198.51.100.10");

        for (int i = 0; i < 3; i++) {
            chain.reset();
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
            assertEquals(1, chain.getInvocations());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        chain.reset();
        filter.doFilterInternal(request, blocked, chain);

        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getHeader("Retry-After") != null && !blocked.getHeader("Retry-After").isEmpty());
        assertEquals("3", blocked.getHeader("X-RateLimit-Limit"));
        assertEquals("0", blocked.getHeader("X-RateLimit-Remaining"));
        assertEquals(1, throttledEvents.get());
        assertEquals(0, chain.getInvocations());
    }

    @Test
    void exemptIpBypassesThrottle() throws Exception {
        ReflectionTestUtils.setField(filter, "exemptIps", "198.51.100.99/32");
        filter.init();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prod/api/v1/auth/me");
        request.setRemoteAddr("198.51.100.99");

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        }

        assertEquals(10, chain.getInvocations());
        assertEquals(0, throttledEvents.get());
    }

    @Test
    void healthPathNotFiltered() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/prod/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void classifyForTest_authScope() {
        assertEquals(RateLimitScope.AUTH, filter.classifyForTest("/prod/api/v1/auth/session").scope());
    }

    private static final class CountingFilterChain implements FilterChain {
        private int invocations;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invocations++;
        }

        int getInvocations() {
            return invocations;
        }

        void reset() {
            invocations = 0;
        }
    }
}
