package io.github.jobs.spring.alert;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.ThrottleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles alert throttling to prevent notification spam.
 * Thread-safe with atomic check-and-acquire operations.
 */
public class AlertThrottler {

    private static final Logger log = LoggerFactory.getLogger(AlertThrottler.class);
    private static final Duration STALE_ENTRY_TTL = Duration.ofHours(24);

    private final ThrottleConfig config;
    private final Map<String, Instant> lastAlertTime = new ConcurrentHashMap<>();
    private final Queue<Instant> globalAlertTimes = new ConcurrentLinkedQueue<>();
    private final ReentrantLock throttleLock = new ReentrantLock();

    public AlertThrottler(ThrottleConfig config) {
        this.config = config;
    }

    /**
     * Checks if an alert should be throttled.
     *
     * @param event the alert event to check
     * @return true if the alert should be sent, false if throttled
     */
    public boolean shouldSend(AlertEvent event) {
        Instant now = Instant.now();

        // Check global rate limit
        if (!checkGlobalRateLimit(now)) {
            log.debug("Alert {} throttled due to global rate limit", event.alertId());
            return false;
        }

        // Check per-alert cooldown
        if (!checkCooldown(event.alertId(), now)) {
            log.debug("Alert {} throttled due to cooldown", event.alertId());
            return false;
        }

        return true;
    }

    /**
     * Atomically checks if an alert should be sent and acquires a slot if allowed.
     * This prevents race conditions where multiple threads could send the same alert.
     *
     * @param event the alert event to check
     * @return true if the slot was acquired and alert should be sent, false if throttled
     */
    public boolean tryAcquire(AlertEvent event) {
        throttleLock.lock();
        try {
            if (!shouldSend(event)) {
                return false;
            }
            // Immediately record to prevent race condition
            recordSent(event);
            return true;
        } finally {
            throttleLock.unlock();
        }
    }

    /**
     * Records that an alert was sent.
     */
    public void recordSent(AlertEvent event) {
        Instant now = Instant.now();
        lastAlertTime.put(event.alertId(), now);
        globalAlertTimes.add(now);
        cleanupOldEntries(now);
    }

    /**
     * Resets throttle state for a specific alert.
     */
    public void reset(String alertId) {
        lastAlertTime.remove(alertId);
    }

    /**
     * Resets all throttle state.
     */
    public void resetAll() {
        lastAlertTime.clear();
        globalAlertTimes.clear();
    }

    private boolean checkGlobalRateLimit(Instant now) {
        cleanupOldEntries(now);
        return globalAlertTimes.size() < config.rateLimit();
    }

    private boolean checkCooldown(String alertId, Instant now) {
        Instant lastTime = lastAlertTime.get(alertId);
        if (lastTime == null) {
            return true;
        }
        return lastTime.plus(config.cooldown()).isBefore(now);
    }

    private void cleanupOldEntries(Instant now) {
        // Cleanup global rate limit entries
        Instant cutoff = now.minus(config.ratePeriod());
        while (!globalAlertTimes.isEmpty() && globalAlertTimes.peek().isBefore(cutoff)) {
            globalAlertTimes.poll();
        }

        // Cleanup stale per-alert cooldown entries to prevent memory leak
        Instant staleCutoff = now.minus(STALE_ENTRY_TTL);
        lastAlertTime.entrySet().removeIf(entry ->
                entry.getValue().isBefore(staleCutoff));
    }

    public ThrottleConfig getConfig() {
        return config;
    }
}
