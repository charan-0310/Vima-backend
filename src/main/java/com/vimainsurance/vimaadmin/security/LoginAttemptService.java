package com.vimainsurance.vimaadmin.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Per-username failed-login lockout.
 *
 * Replaces the abuse-control coverage previously provided by reCAPTCHA on the legacy
 * /api/v1/login and /api/v1/auth/login flows. Combined with {@link
 * com.vimainsurance.vimaadmin.config.GlobalRateLimitFilter} (per-IP), this gives both
 * IP-level and account-level brute-force protection.
 *
 * <h3>Single-node only</h3>
 * The failure counter lives in this JVM's heap. In a multi-node deployment each node has
 * its own counter, so an attacker can spread N×max-failures attempts across N nodes
 * before tripping any lockout. Vima currently runs a single backend container per
 * environment (verified at audit), so this is acceptable. If you ever scale horizontally:
 *
 *   1. Set vima.deploy.instance-count > 1 in the env (the warning at startup will remind you).
 *   2. Migrate this service to Redis-backed storage (Redisson or Spring Data Redis).
 *   3. See F-16 / F-17 in IRDAI_ISO27001_Remediation_Plan.md for the full upgrade path.
 *
 * <h3>Properties</h3>
 *   security.login.lockout.max-failures=5
 *   security.login.lockout.window-minutes=15
 *   vima.deploy.instance-count=1   (default; raise if you scale out — triggers a startup warning)
 *
 * <h3>Usage from AuthServiceImpl</h3>
 *   loginAttemptService.assertNotLocked(username);  // throws AccountLockedException
 *   ... try authenticate ...
 *   on failure: loginAttemptService.onFailure(username);
 *   on success: loginAttemptService.onSuccess(username);
 */
@Service
public class LoginAttemptService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginAttemptService.class);

    @Value("${security.login.lockout.max-failures:5}")
    private int maxFailures;

    @Value("${security.login.lockout.window-minutes:15}")
    private long windowMinutes;

    @Value("${vima.deploy.instance-count:1}")
    private int instanceCount;

    @jakarta.annotation.PostConstruct
    void warnOnMultiNode() {
        if (instanceCount > 1) {
            log.warn("⚠ LoginAttemptService is in-memory but vima.deploy.instance-count={} (>1). "
                    + "Per-username lockout state is NOT shared across nodes. An attacker can spread "
                    + "{}×max-failures attempts across nodes before lockout triggers. "
                    + "Migrate to Redis-backed storage — see F-16 in the remediation plan.",
                    instanceCount, instanceCount);
        } else {
            log.info("LoginAttemptService initialised: maxFailures={}, windowMinutes={}, single-node mode.",
                    maxFailures, windowMinutes);
        }
    }

    private record Record(AtomicInteger failures, Instant firstFailure) {}

    private final Map<String, Record> attempts = new ConcurrentHashMap<>();

    /**
     * Throws {@link AccountLockedException} if the username is currently locked out.
     * Call before authenticating.
     */
    public void assertNotLocked(String username) {
        if (username == null || username.isBlank()) return;
        Record r = attempts.get(username.toLowerCase());
        if (r == null) return;
        if (isExpired(r)) {
            attempts.remove(username.toLowerCase());
            return;
        }
        if (r.failures().get() >= maxFailures) {
            long secondsLeft = secondsUntilExpiry(r);
            throw new AccountLockedException(
                    String.format("Account temporarily locked due to repeated failed logins. Retry in %d seconds.", secondsLeft));
        }
    }

    /** Increment failure counter. */
    public void onFailure(String username) {
        if (username == null || username.isBlank()) return;
        attempts.compute(username.toLowerCase(), (k, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Record(new AtomicInteger(1), Instant.now());
            }
            existing.failures().incrementAndGet();
            return existing;
        });
    }

    /** Reset on successful login. */
    public void onSuccess(String username) {
        if (username == null || username.isBlank()) return;
        attempts.remove(username.toLowerCase());
    }

    /** Periodic cleanup (every 5 min) so the map can never grow unbounded from typo'd usernames. */
    @Scheduled(fixedDelay = 300_000L)
    void evictExpired() {
        attempts.entrySet().removeIf(e -> isExpired(e.getValue()));
    }

    private boolean isExpired(Record r) {
        return secondsUntilExpiry(r) <= 0;
    }

    private long secondsUntilExpiry(Record r) {
        Instant unlockAt = r.firstFailure().plusSeconds(windowMinutes * 60L);
        return Math.max(0L, unlockAt.getEpochSecond() - Instant.now().getEpochSecond());
    }

    public static class AccountLockedException extends RuntimeException {
        public AccountLockedException(String msg) { super(msg); }
    }
}
