package com.vimainsurance.vimaadmin.ratelimit;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Shared Bucket4j construction for F-04 global filter and {@code @RateLimit} aspect.
 */
public final class RateLimitBucketFactory {

    private RateLimitBucketFactory() {}

    public static Bucket perMinute(int requestsPerMinute) {
        Bandwidth bandwidth = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    public static Bucket forAnnotation(int limit, int periodMinutes) {
        int period = Math.max(1, periodMinutes);
        Bandwidth bandwidth = Bandwidth.classic(
                limit,
                Refill.intervally(limit, Duration.ofMinutes(period)));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
