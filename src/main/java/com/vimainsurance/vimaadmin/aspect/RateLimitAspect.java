package com.vimainsurance.vimaadmin.aspect;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vimainsurance.vimaadmin.annotation.RateLimit;
import com.vimainsurance.vimaadmin.exception.RateLimitExceededException;
import com.vimainsurance.vimaadmin.ratelimit.RateLimitBucketFactory;
import com.vimainsurance.vimaadmin.util.IpAddressExtractor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AOP Aspect for rate limiting enforcement on methods annotated with {@link RateLimit}.
 *
 * <p>Secondary layer under {@link com.vimainsurance.vimaadmin.config.GlobalRateLimitFilter}.
 * Buckets are keyed by IP + annotation limits and stored in a bounded Caffeine cache (F-04).
 */
@Aspect
@Component
public class RateLimitAspect {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    @Value("${ratelimit.cache.max-entries:50000}")
    private long maxCacheEntries;

    @Value("${ratelimit.cache.expire-after-access-minutes:10}")
    private long expireAfterAccessMinutes;

    private Cache<String, Bucket> bucketCache;

    @PostConstruct
    void init() {
        bucketCache = Caffeine.newBuilder()
                .maximumSize(maxCacheEntries)
                .expireAfterAccess(Duration.ofMinutes(expireAfterAccessMinutes))
                .build();
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = extractHttpRequest(joinPoint);

        if (request == null) {
            logger.debug(
                    "[correlationId:{}] No HTTP request found in method arguments, skipping rate limit",
                    MDC.get("correlationId"));
            return joinPoint.proceed();
        }

        String ipAddress = IpAddressExtractor.extractIpAddress(request);

        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.warn("[correlationId:{}] Could not extract IP address, skipping rate limit", MDC.get("correlationId"));
            return joinPoint.proceed();
        }

        String cacheKey = ipAddress + "|" + rateLimit.limit() + "|" + rateLimit.periodMinutes();
        Bucket bucket = bucketCache.get(
                cacheKey, k -> RateLimitBucketFactory.forAnnotation(rateLimit.limit(), rateLimit.periodMinutes()));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            logger.debug(
                    "[correlationId:{}] Rate limit check passed for IP: {}. Remaining tokens: {}",
                    MDC.get("correlationId"),
                    ipAddress,
                    probe.getRemainingTokens());
            return joinPoint.proceed();
        }

        long waitTimeSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);

        logger.warn(
                "[correlationId:{}] Rate limit exceeded for IP: {}. Retry after {} seconds",
                MDC.get("correlationId"),
                ipAddress,
                waitTimeSeconds);

        throw new RateLimitExceededException(
                String.format("Too many requests. Please retry after %d seconds.", waitTimeSeconds));
    }

    private HttpServletRequest extractHttpRequest(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof HttpServletRequest httpRequest) {
                return httpRequest;
            }
        }
        return null;
    }
}
