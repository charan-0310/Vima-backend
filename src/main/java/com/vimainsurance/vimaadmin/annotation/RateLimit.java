package com.vimainsurance.vimaadmin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level rate limiting.
 * Uses Bucket4j token bucket algorithm to limit requests per IP address.
 * 
 * Example usage:
 * <pre>
 * {@code @RateLimit(limit = 3, periodMinutes = 1)}
 * public void someMethod() {
 *     // This method can be called max 3 times per minute per IP
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Maximum number of requests allowed within the time period.
     * Default: 3 requests
     */
    int limit() default 3;
    
    /**
     * Time period in minutes for the rate limit.
     * Default: 1 minute
     */
    int periodMinutes() default 1;
}
