package com.vimainsurance.vimaadmin.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.exception.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

/**
 * Rate limit per enrollment token: 100 requests per minute for premium calculation.
 */
@Service
public class EnrollmentTokenRateLimitService {

    private static final int LIMIT = 100;
    private static final int PERIOD_MINUTES = 1;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void consumeOrThrow(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(token, k -> createBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            throw new RateLimitExceededException("Too many requests. Please retry after " + waitSeconds + " seconds.");
        }
    }

    private static Bucket createBucket() {
        Bandwidth bandwidth = Bandwidth.classic(LIMIT, Refill.intervally(LIMIT, Duration.ofMinutes(PERIOD_MINUTES)));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
