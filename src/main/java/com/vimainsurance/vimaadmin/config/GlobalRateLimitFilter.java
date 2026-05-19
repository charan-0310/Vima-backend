package com.vimainsurance.vimaadmin.config;

import java.io.IOException;
import java.time.Duration;
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
import com.vimainsurance.vimaadmin.ratelimit.RateLimitBucketFactory;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitExemptIpMatcher;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitMetrics;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitPathClassifier;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitRule;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitScope;
import com.vimainsurance.vimaadmin.util.IpAddressExtractor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
 * Rule precedence: AUTH &gt; ENROLLMENT &gt; SENSITIVE &gt; DEFAULT.
 *
 * Configuration (application.properties):
 *   ratelimit.enabled=true|false
 *   ratelimit.default.requests-per-min
 *   ratelimit.enrollment.requests-per-min
 *   ratelimit.auth.requests-per-min
 *   ratelimit.sensitive.requests-per-min
 *   ratelimit.exempt-ips=10.0.0.0/8,52.66.0.0/16
 *   ratelimit.cache.max-entries
 *   ratelimit.cache.expire-after-access-minutes
 *
 * Health/actuator paths are NOT rate-limited so liveness probes do not get throttled.
 *
 * See {@code docs/guides/rate-limiting-f04.md} for operations and configuration reference.
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

    @Value("${ratelimit.auth.requests-per-min:10}")
    private int authRpm;

    @Value("${ratelimit.sensitive.requests-per-min:10}")
    private int sensitiveRpm;

    @Value("${ratelimit.exempt-ips:}")
    private String exemptIps;

    @Value("${ratelimit.cache.max-entries:50000}")
    private long maxCacheEntries;

    @Value("${ratelimit.cache.expire-after-access-minutes:10}")
    private long expireAfterAccessMinutes;

    private final RateLimitMetrics rateLimitMetrics;

    private Cache<String, Bucket> buckets;
    private RateLimitExemptIpMatcher exemptIpMatcher;

    private final AtomicLong throttleCount = new AtomicLong();

    public GlobalRateLimitFilter(RateLimitMetrics rateLimitMetrics) {
        this.rateLimitMetrics = rateLimitMetrics;
    }

    @PostConstruct
    void init() {
        this.buckets = Caffeine.newBuilder()
                .maximumSize(maxCacheEntries)
                .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
                .build();
        this.exemptIpMatcher = new RateLimitExemptIpMatcher(exemptIps);
        logger.info(
                "GlobalRateLimitFilter initialised: defaultRpm={}, enrollmentRpm={}, authRpm={}, "
                        + "sensitiveRpm={}, cache maxEntries={}, expireAfterAccess={}min, exemptIpRules={}",
                defaultRpm,
                enrollmentRpm,
                authRpm,
                sensitiveRpm,
                maxCacheEntries,
                expireAfterAccessMinutes,
                exemptIpMatcher != null ? "configured" : "none");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
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

        if (exemptIpMatcher.isExempt(ip)) {
            chain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = RateLimitPathClassifier.classify(
                request.getRequestURI(), authRpm, enrollmentRpm, sensitiveRpm, defaultRpm);
        String key = rule.scope().name() + "|" + ip;
        Bucket bucket = buckets.get(key, k -> RateLimitBucketFactory.perMinute(rule.requestsPerMinute()));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(rule.requestsPerMinute()));
            chain.doFilter(request, response);
            return;
        }

        long waitSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        throttleCount.incrementAndGet();
        rateLimitMetrics.recordThrottled(rule.scope(), request.getRequestURI());
        logger.warn(
                "[correlationId:{}] Rate limit exceeded scope={} ip={} uri={} retryAfter={}s",
                MDC.get("correlationId"),
                rule.scope(),
                ip,
                request.getRequestURI(),
                waitSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.requestsPerMinute()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType("application/json");
        response.getWriter()
                .write(String.format(
                        "{\"status\":429,\"message\":\"Too many requests. Retry after %d seconds.\"}",
                        waitSeconds));
    }

    /** Test/diagnostic accessor. */
    public Map<String, Long> stats() {
        return Map.of("buckets", buckets.estimatedSize(), "throttled", throttleCount.get());
    }

    /** Package-visible for tests: classify without running the filter. */
    RateLimitRule classifyForTest(String requestUri) {
        return RateLimitPathClassifier.classify(
                requestUri, authRpm, enrollmentRpm, sensitiveRpm, defaultRpm);
    }
}
