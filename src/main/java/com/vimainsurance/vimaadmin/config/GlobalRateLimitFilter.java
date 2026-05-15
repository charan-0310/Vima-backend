package com.vimainsurance.vimaadmin.config;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vimainsurance.vimaadmin.util.IpAddressExtractor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * F-04 — Global per-IP rate limiting.
 *
 * Applied to every /api/** request. Uses Bucket4j token buckets in memory; in a multi-node
 * deployment this should be backed by Bucket4j-Redis (or AWS WAF rate-based rules).
 *
 * Rule precedence (longest prefix wins):
 *   /api/v1/enrollment/**, /api/v1/enrollment-submissions/**     -> ENROLL  (20 req / min)
 *   anything else under /api/**                                  -> DEFAULT (120 req / min)
 *
 * Configuration overrides (application.properties):
 *   ratelimit.enabled=true|false
 *   ratelimit.default.requests-per-min
 *   ratelimit.enrollment.requests-per-min
 *   ratelimit.exempt-ips=10.0.0.0/8,52.66.0.0/16
 *
 * Health/actuator paths are NOT rate-limited so liveness probes do not get throttled.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalRateLimitFilter.class);

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${ratelimit.default.requests-per-min:120}")
    private int defaultRpm;

    @Value("${ratelimit.enrollment.requests-per-min:20}")
    private int enrollmentRpm;

    @Value("${ratelimit.cache.max-entries:50000}")
    private long maxCacheEntries;

    @Value("${ratelimit.cache.expire-after-access-minutes:10}")
    private long expireAfterAccessMinutes;

    /**
     * Bounded "scope|ip" -> Bucket cache. Caffeine enforces both an upper size cap and an
     * idle-eviction timer. This blocks the previous unbounded-growth concern when an attacker
     * floods spoofed source IPs.
     */
    private Cache<String, Bucket> buckets;

    private final AtomicLong throttleCount = new AtomicLong();

    @PostConstruct
    void init() {
        this.buckets = Caffeine.newBuilder()
                .maximumSize(maxCacheEntries)
                .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
                .build();
        logger.info("GlobalRateLimitFilter initialised: defaultRpm={}, enrollmentRpm={}, "
                        + "cache maxEntries={}, expireAfterAccess={}min",
                defaultRpm, enrollmentRpm, maxCacheEntries, expireAfterAccessMinutes);
    }

    private enum Scope {
        ENROLLMENT,
        DEFAULT,
    }

    private record RuleMatch(Scope scope, int rpm) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return true;
        // Skip health, actuator, swagger, favicon, public, and non-/api paths
        return uri.contains("/health")
                || uri.contains("/actuator")
                || uri.contains("/v3/api-docs")
                || uri.contains("/swagger")
                || uri.contains("/favicon")
                || uri.contains("/public/")
                || !uri.contains("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String ip = IpAddressExtractor.extractIpAddress(request);
        if (ip == null || ip.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        RuleMatch rule = classify(request.getRequestURI());
        String key = rule.scope().name() + "|" + ip;
        Bucket bucket = buckets.get(key, k -> newBucket(rule.rpm()));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(rule.rpm()));
            chain.doFilter(request, response);
            return;
        }

        long waitSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        throttleCount.incrementAndGet();
        logger.warn("[correlationId:{}] Rate limit exceeded scope={} ip={} uri={} retryAfter={}s",
                MDC.get("correlationId"), rule.scope(), ip, request.getRequestURI(), waitSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.rpm()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"status\":429,\"message\":\"Too many requests. Retry after %d seconds.\"}", waitSeconds));
    }

    private RuleMatch classify(String uri) {
        if (uri == null) return new RuleMatch(Scope.DEFAULT, defaultRpm);
        // strip context path heuristically
        String path = uri;
        for (String prefix : List.of("/dev", "/prod", "/uat", "/stage", "/local", "/test")) {
            if (path.startsWith(prefix + "/")) {
                path = path.substring(prefix.length());
                break;
            }
        }
        if (path.startsWith("/api/v1/enrollment/")
                || path.startsWith("/api/v1/enrollment-submissions/")
                || path.startsWith("/api/v1/enrollments/")) {
            return new RuleMatch(Scope.ENROLLMENT, enrollmentRpm);
        }
        return new RuleMatch(Scope.DEFAULT, defaultRpm);
    }

    private Bucket newBucket(int requestsPerMinute) {
        Bandwidth bw = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bw).build();
    }

    /** Test/diagnostic accessor. */
    public Map<String, Long> stats() {
        return Map.of("buckets", buckets.estimatedSize(), "throttled", throttleCount.get());
    }
}
