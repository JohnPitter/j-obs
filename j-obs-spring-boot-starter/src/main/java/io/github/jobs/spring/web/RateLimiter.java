package io.github.jobs.spring.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory rate limiter using sliding window algorithm.
 * Thread-safe and suitable for single-instance deployments.
 * Automatically cleans up stale entries to prevent memory leaks.
 * <p>
 * Implements {@link DisposableBean} for proper cleanup on Spring shutdown.
 */
public class RateLimiter implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Queue<Instant>> requestLog = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * Creates a rate limiter with the specified limits.
     * Starts a periodic cleanup task to prevent memory leaks.
     *
     * @param maxRequests maximum requests allowed in the window
     * @param window time window for rate limiting
     */
    public RateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "j-obs-ratelimiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        long cleanupIntervalSeconds = Math.max(30, window.toSeconds());
        this.cleanupExecutor.scheduleAtFixedRate(this::cleanup,
                cleanupIntervalSeconds, cleanupIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Checks if a request from the given key is allowed.
     * If allowed, records the request. Uses synchronized access per-key
     * to prevent race conditions between size check and add.
     *
     * @param key identifier for rate limiting (e.g., IP address, user ID)
     * @return true if request is allowed, false if rate limited
     */
    public boolean tryAcquire(String key) {
        // Reject if tracking too many keys (DoS protection)
        if (requestLog.size() >= MAX_TRACKED_KEYS && !requestLog.containsKey(key)) {
            return false;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Queue<Instant> timestamps = requestLog.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

        synchronized (timestamps) {
            // Remove old entries
            while (!timestamps.isEmpty() && timestamps.peek().isBefore(cutoff)) {
                timestamps.poll();
            }

            // Check if under limit (atomic with the add)
            if (timestamps.size() < maxRequests) {
                timestamps.add(now);
                return true;
            }
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

        synchronized (timestamps) {
            long validCount = timestamps.stream()
                    .filter(t -> !t.isBefore(cutoff))
                    .count();
            return Math.max(0, maxRequests - (int) validCount);
        }
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
     * Called automatically by the scheduled cleanup task.
     */
    public void cleanup() {
        Instant cutoff = Instant.now().minus(window);
        requestLog.entrySet().removeIf(entry -> {
            Queue<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                timestamps.removeIf(t -> t.isBefore(cutoff));
                return timestamps.isEmpty();
            }
        });
    }

    /**
     * Shuts down the cleanup executor gracefully.
     * Waits up to 5 seconds for pending tasks to complete.
     */
    public void shutdown() {
        log.debug("Shutting down rate limiter cleanup executor");
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
        requestLog.clear();
        log.debug("Rate limiter shutdown complete");
    }

    /**
     * Called by Spring when the bean is destroyed.
     */
    @Override
    public void destroy() {
        shutdown();
    }
}
