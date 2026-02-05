package com.vimainsurance.vimaadmin.aspect;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.annotation.RateLimit;
import com.vimainsurance.vimaadmin.exception.RateLimitExceededException;
import com.vimainsurance.vimaadmin.util.IpAddressExtractor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AOP Aspect for rate limiting enforcement.
 * 
 * Intercepts methods annotated with @RateLimit and enforces rate limits
 * using the token bucket algorithm (Bucket4j).
 * 
 * Rate limits are applied per IP address. Each IP gets its own bucket
 * with tokens that refill over time according to the configured rate.
 */
@Aspect
@Component
public class RateLimitAspect {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);
    
    /**
     * Cache of rate limit buckets per IP address.
     * Key: IP address
     * Value: Bucket with rate limit configuration
     */
    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    
    /**
     * Enforces rate limiting on methods annotated with @RateLimit.
     * 
     * @param joinPoint The method execution join point
     * @param rateLimit The rate limit annotation
     * @return The result of the method execution
     * @throws Throwable If the method execution fails or rate limit is exceeded
     */
    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Extract HTTP request from method arguments
        HttpServletRequest request = extractHttpRequest(joinPoint);
        
        if (request == null) {
            // If no HTTP request found, skip rate limiting
            logger.debug("[correlationId:{}] No HTTP request found in method arguments, skipping rate limit", 
                MDC.get("correlationId"));
            return joinPoint.proceed();
        }
        
        // Extract client IP address
        String ipAddress = IpAddressExtractor.extractIpAddress(request);
        
        if (ipAddress == null || ipAddress.isEmpty()) {
            logger.warn("[correlationId:{}] Could not extract IP address, skipping rate limit", 
                MDC.get("correlationId"));
            return joinPoint.proceed();
        }
        
        // Get or create bucket for this IP
        Bucket bucket = bucketCache.computeIfAbsent(ipAddress, k -> createBucket(rateLimit));
        
        // Try to consume a token from the bucket
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Token consumed successfully - request allowed
            logger.debug("[correlationId:{}] Rate limit check passed for IP: {}. Remaining tokens: {}", 
                MDC.get("correlationId"), 
                ipAddress,
                probe.getRemainingTokens()
            );
            return joinPoint.proceed();
        } else {
            // Rate limit exceeded
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            
            logger.warn("[correlationId:{}] Rate limit exceeded for IP: {}. Retry after {} seconds", 
                MDC.get("correlationId"), 
                ipAddress, 
                waitTimeSeconds
            );
            
            throw new RateLimitExceededException(
                String.format("Too many requests. Please retry after %d seconds.", waitTimeSeconds)
            );
        }
    }
    
    /**
     * Creates a new rate limit bucket with the specified configuration.
     * 
     * @param rateLimit The rate limit annotation with configuration
     * @return New bucket configured with the specified rate limit
     */
    private Bucket createBucket(RateLimit rateLimit) {
        // Create bandwidth with specified limit and refill period
        Bandwidth bandwidth = Bandwidth.classic(
            rateLimit.limit(), 
            Refill.intervally(
                rateLimit.limit(), 
                Duration.ofMinutes(rateLimit.periodMinutes())
            )
        );
        
        return Bucket.builder()
            .addLimit(bandwidth)
            .build();
    }
    
    /**
     * Extracts HttpServletRequest from method arguments.
     * 
     * @param joinPoint The method execution join point
     * @return HttpServletRequest if found, null otherwise
     */
    private HttpServletRequest extractHttpRequest(ProceedingJoinPoint joinPoint) {
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }
        return null;
    }
}
