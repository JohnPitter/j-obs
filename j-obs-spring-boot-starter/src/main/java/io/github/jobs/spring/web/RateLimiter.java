package io.github.jobs.spring.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple in-memory rate limiter using sliding window algorithm.
 * Thread-safe and suitable for single-instance deployments.
 */
public class RateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Queue<Instant>> requestLog = new ConcurrentHashMap<>();

    /**
     * Creates a rate limiter with the specified limits.
     *
     * @param maxRequests maximum requests allowed in the window
     * @param window time window for rate limiting
     */
    public RateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    /**
     * Checks if a request from the given key is allowed.
     * If allowed, records the request.
     *
     * @param key identifier for rate limiting (e.g., IP address, user ID)
     * @return true if request is allowed, false if rate limited
     */
    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Queue<Instant> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        // Remove old entries
        while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
            timestamps.poll();
        }

        // Check if under limit
        if (timestamps.size() < maxRequests) {
            timestamps.add(now);
            return true;
        }

        return false;
    }

    /**
     * Gets remaining requests for a key in the current window.
     *
     * @param key identifier for rate limiting
     * @return number of remaining requests allowed
     */
    public int getRemainingRequests(String key) {
        Instant cutoff = Instant.now().minus(window);
        Queue<Instant> timestamps = requestLog.get(key);

        if (timestamps == null) {
            return maxRequests;
        }

        // Count valid entries
        long validCount = timestamps.stream()
                .filter(t -> !t.isBefore(cutoff))
                .count();

        return Math.max(0, maxRequests - (int) validCount);
    }

    /**
     * Clears rate limit data for a specific key.
     */
    public void reset(String key) {
        requestLog.remove(key);
    }

    /**
     * Clears all rate limit data.
     */
    public void resetAll() {
        requestLog.clear();
    }

    /**
     * Performs cleanup of stale entries to prevent memory leaks.
     * Should be called periodically.
     */
    public void cleanup() {
        Instant cutoff = Instant.now().minus(window.multipliedBy(2));
        requestLog.entrySet().removeIf(entry -> {
            Queue<Instant> timestamps = entry.getValue();
            timestamps.removeIf(t -> t.isBefore(cutoff));
            return timestamps.isEmpty();
        });
    }
}
