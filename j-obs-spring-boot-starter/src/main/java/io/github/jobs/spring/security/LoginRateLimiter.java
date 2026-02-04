package io.github.jobs.spring.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter specifically designed for login attempts to prevent brute-force attacks.
 * <p>
 * Features:
 * <ul>
 *   <li>Tracks failed login attempts per IP address</li>
 *   <li>Implements progressive lockout after repeated failures</li>
 *   <li>Automatically resets after the lockout window expires</li>
 *   <li>Thread-safe for concurrent access</li>
 * </ul>
 * <p>
 * Default configuration:
 * <ul>
 *   <li>5 attempts per 15 minutes</li>
 *   <li>Lockout duration increases with repeated violations</li>
 * </ul>
 */
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private static final int MAX_TRACKED_IPS = 10_000;
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);

    private final int maxAttempts;
    private final Duration window;
    private final Duration baseLockoutDuration;

    private final Map<String, LoginAttemptInfo> attemptsByIp = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * Creates a login rate limiter with default configuration:
     * 5 attempts per 15 minutes, 15 minute base lockout.
     */
    public LoginRateLimiter() {
        this(5, Duration.ofMinutes(15), Duration.ofMinutes(15));
    }

    /**
     * Creates a login rate limiter with custom configuration.
     *
     * @param maxAttempts maximum login attempts before lockout
     * @param window time window for counting attempts
     * @param baseLockoutDuration base duration for lockout (increases progressively)
     */
    public LoginRateLimiter(int maxAttempts, Duration window, Duration baseLockoutDuration) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.baseLockoutDuration = baseLockoutDuration;

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "j-obs-login-ratelimiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                CLEANUP_INTERVAL.toSeconds(),
                CLEANUP_INTERVAL.toSeconds(),
                TimeUnit.SECONDS
        );
    }

    /**
     * Checks if a login attempt is allowed for the given IP address.
     *
     * @param ipAddress the IP address of the login attempt
     * @return true if the attempt is allowed, false if rate limited
     */
    public boolean isAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return true; // Allow if IP cannot be determined
        }

        LoginAttemptInfo info = attemptsByIp.get(ipAddress);
        if (info == null) {
            return true;
        }

        synchronized (info) {
            // Check if currently locked out
            if (info.isLockedOut()) {
                Duration remaining = info.getLockoutRemaining();
                log.debug("Login blocked for IP {}: locked out for {} more seconds",
                        ipAddress, remaining.toSeconds());
                return false;
            }

            // Clean old attempts and check count
            info.cleanOldAttempts(window);
            return info.getAttemptCount() < maxAttempts;
        }
    }

    /**
     * Records a failed login attempt for the given IP address.
     *
     * @param ipAddress the IP address of the failed attempt
     */
    public void recordFailedAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        // Prevent DoS via memory exhaustion
        if (attemptsByIp.size() >= MAX_TRACKED_IPS && !attemptsByIp.containsKey(ipAddress)) {
            log.warn("Login rate limiter at capacity ({}), cannot track new IP", MAX_TRACKED_IPS);
            return;
        }

        LoginAttemptInfo info = attemptsByIp.computeIfAbsent(ipAddress, k -> new LoginAttemptInfo());

        synchronized (info) {
            info.cleanOldAttempts(window);
            info.recordAttempt();

            if (info.getAttemptCount() >= maxAttempts) {
                // Calculate progressive lockout: base * 2^(violations-1)
                int violations = info.getViolationCount();
                long lockoutMultiplier = Math.min(8, (long) Math.pow(2, violations)); // Cap at 8x
                Duration lockoutDuration = baseLockoutDuration.multipliedBy(lockoutMultiplier);
                info.lockOut(lockoutDuration);

                log.warn("IP {} locked out for {} minutes after {} failed login attempts (violation #{})",
                        ipAddress, lockoutDuration.toMinutes(), maxAttempts, violations + 1);
            }
        }
    }

    /**
     * Records a successful login, resetting the attempt counter.
     *
     * @param ipAddress the IP address of the successful login
     */
    public void recordSuccessfulLogin(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        attemptsByIp.remove(ipAddress);
    }

    /**
     * Gets the remaining lockout time for an IP address.
     *
     * @param ipAddress the IP address to check
     * @return remaining lockout duration, or Duration.ZERO if not locked out
     */
    public Duration getLockoutRemaining(String ipAddress) {
        LoginAttemptInfo info = attemptsByIp.get(ipAddress);
        if (info == null) {
            return Duration.ZERO;
        }
        synchronized (info) {
            return info.getLockoutRemaining();
        }
    }

    /**
     * Gets the number of remaining attempts for an IP address.
     *
     * @param ipAddress the IP address to check
     * @return number of remaining attempts before lockout
     */
    public int getRemainingAttempts(String ipAddress) {
        LoginAttemptInfo info = attemptsByIp.get(ipAddress);
        if (info == null) {
            return maxAttempts;
        }
        synchronized (info) {
            info.cleanOldAttempts(window);
            return Math.max(0, maxAttempts - info.getAttemptCount());
        }
    }

    /**
     * Manually resets rate limiting for an IP address.
     *
     * @param ipAddress the IP address to reset
     */
    public void reset(String ipAddress) {
        attemptsByIp.remove(ipAddress);
    }

    /**
     * Cleans up stale entries to prevent memory leaks.
     */
    private void cleanup() {
        Instant cutoff = Instant.now().minus(window.multipliedBy(2));
        attemptsByIp.entrySet().removeIf(entry -> {
            LoginAttemptInfo info = entry.getValue();
            synchronized (info) {
                return info.getLastAttempt().isBefore(cutoff) && !info.isLockedOut();
            }
        });
    }

    /**
     * Shuts down the cleanup executor gracefully.
     * Waits up to 5 seconds for pending tasks to complete before forcing shutdown.
     */
    public void shutdown() {
        log.debug("Shutting down login rate limiter cleanup executor");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.debug("Cleanup executor did not terminate in time, forcing shutdown");
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted while waiting for cleanup executor shutdown");
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        attemptsByIp.clear();
        log.debug("Login rate limiter shutdown complete");
    }

    /**
     * Internal class to track login attempts for a single IP.
     */
    private static class LoginAttemptInfo {
        private int attemptCount = 0;
        private int violationCount = 0;
        private Instant lastAttempt = Instant.now();
        private Instant lockoutUntil = null;

        void recordAttempt() {
            attemptCount++;
            lastAttempt = Instant.now();
        }

        void cleanOldAttempts(Duration window) {
            Instant cutoff = Instant.now().minus(window);
            if (lastAttempt.isBefore(cutoff)) {
                attemptCount = 0;
            }
            // Also check if lockout has expired
            if (lockoutUntil != null && Instant.now().isAfter(lockoutUntil)) {
                lockoutUntil = null;
            }
        }

        void lockOut(Duration duration) {
            lockoutUntil = Instant.now().plus(duration);
            violationCount++;
            attemptCount = 0; // Reset for next window after lockout
        }

        boolean isLockedOut() {
            return lockoutUntil != null && Instant.now().isBefore(lockoutUntil);
        }

        Duration getLockoutRemaining() {
            if (!isLockedOut()) {
                return Duration.ZERO;
            }
            return Duration.between(Instant.now(), lockoutUntil);
        }

        int getAttemptCount() {
            return attemptCount;
        }

        int getViolationCount() {
            return violationCount;
        }

        Instant getLastAttempt() {
            return lastAttempt;
        }
    }
}
