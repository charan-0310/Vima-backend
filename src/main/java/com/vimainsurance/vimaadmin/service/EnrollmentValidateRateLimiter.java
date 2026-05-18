package com.vimainsurance.vimaadmin.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.exception.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

/**
 * Brute-force protection for magic-link discovery ({@code GET /api/v1/enrollment/{token}}).
 * Not applied to in-session enrollment API calls that re-validate the same token.
 */
@Service
public class EnrollmentValidateRateLimiter {

    @Value("${enrollment.validate.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${enrollment.validate.rate-limit.ip-per-5-min:30}")
    private int ipLimit;

    @Value("${enrollment.validate.rate-limit.token-per-hour:120}")
    private int tokenLimit;

    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> tokenBuckets = new ConcurrentHashMap<>();

    public void consumeOrThrow(String clientIp, String tokenHash) {
        if (!enabled) {
            return;
        }
        if (clientIp != null && !clientIp.isBlank()) {
            consumeBucket(ipBuckets, "ip:" + clientIp, ipLimit, Duration.ofMinutes(5));
        }
        if (tokenHash != null && !tokenHash.isBlank()) {
            consumeBucket(tokenBuckets, "token:" + tokenHash, tokenLimit, Duration.ofHours(1));
        }
    }

    private static void consumeBucket(
            ConcurrentHashMap<String, Bucket> buckets,
            String key,
            int limit,
            Duration period) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(limit, period));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            throw new RateLimitExceededException(
                    "Too many enrollment validation attempts. Please retry after " + waitSeconds + " seconds.");
        }
    }

    private static Bucket createBucket(int limit, Duration period) {
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, period));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
