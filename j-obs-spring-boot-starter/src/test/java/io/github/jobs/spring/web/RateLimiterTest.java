package io.github.jobs.spring.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RateLimiter sliding window algorithm.
 */
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(5, Duration.ofSeconds(1));
    }

    // ==================== tryAcquire ====================

    @Test
    void tryAcquire_shouldAllowRequestsUnderLimit() {
        String key = "client-1";

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryAcquire(key)).isTrue();
        }
    }

    @Test
    void tryAcquire_shouldBlockRequestsOverLimit() {
        String key = "client-1";

        // Use up the limit
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire(key);
        }

        // Next request should be blocked
        assertThat(rateLimiter.tryAcquire(key)).isFalse();
    }

    @Test
    void tryAcquire_shouldTrackDifferentKeysIndependently() {
        String key1 = "client-1";
        String key2 = "client-2";

        // Use up limit for key1
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire(key1);
        }

        // key1 should be blocked
        assertThat(rateLimiter.tryAcquire(key1)).isFalse();

        // key2 should still be allowed
        assertThat(rateLimiter.tryAcquire(key2)).isTrue();
    }

    @Test
    void tryAcquire_shouldAllowRequestsAfterWindowExpires() throws InterruptedException {
        // Use short window for testing
        RateLimiter shortWindow = new RateLimiter(2, Duration.ofMillis(50));
        String key = "client-1";

        // Use up the limit
        assertThat(shortWindow.tryAcquire(key)).isTrue();
        assertThat(shortWindow.tryAcquire(key)).isTrue();
        assertThat(shortWindow.tryAcquire(key)).isFalse();

        // Wait for window to expire
        Thread.sleep(60);

        // Should be allowed again
        assertThat(shortWindow.tryAcquire(key)).isTrue();
    }

    // ==================== getRemainingRequests ====================

    @Test
    void getRemainingRequests_shouldReturnMaxForNewKey() {
        assertThat(rateLimiter.getRemainingRequests("unknown-key")).isEqualTo(5);
    }

    @Test
    void getRemainingRequests_shouldDecrementWithRequests() {
        String key = "client-1";

        assertThat(rateLimiter.getRemainingRequests(key)).isEqualTo(5);

        rateLimiter.tryAcquire(key);
        assertThat(rateLimiter.getRemainingRequests(key)).isEqualTo(4);

        rateLimiter.tryAcquire(key);
        assertThat(rateLimiter.getRemainingRequests(key)).isEqualTo(3);
    }

    @Test
    void getRemainingRequests_shouldReturnZeroWhenExhausted() {
        String key = "client-1";

        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire(key);
        }

        assertThat(rateLimiter.getRemainingRequests(key)).isEqualTo(0);
    }

    // ==================== reset ====================

    @Test
    void reset_shouldClearSpecificKey() {
        String key1 = "client-1";
        String key2 = "client-2";

        // Use some requests
        rateLimiter.tryAcquire(key1);
        rateLimiter.tryAcquire(key1);
        rateLimiter.tryAcquire(key2);

        // Reset key1
        rateLimiter.reset(key1);

        // key1 should have full quota
        assertThat(rateLimiter.getRemainingRequests(key1)).isEqualTo(5);

        // key2 should still have used quota
        assertThat(rateLimiter.getRemainingRequests(key2)).isEqualTo(4);
    }

    @Test
    void reset_shouldBeNoOpForUnknownKey() {
        rateLimiter.reset("unknown-key");
        // Should not throw
    }

    // ==================== resetAll ====================

    @Test
    void resetAll_shouldClearAllKeys() {
        rateLimiter.tryAcquire("client-1");
        rateLimiter.tryAcquire("client-1");
        rateLimiter.tryAcquire("client-2");

        rateLimiter.resetAll();

        assertThat(rateLimiter.getRemainingRequests("client-1")).isEqualTo(5);
        assertThat(rateLimiter.getRemainingRequests("client-2")).isEqualTo(5);
    }

    // ==================== cleanup ====================

    @Test
    void cleanup_shouldRemoveStaleEntries() throws InterruptedException {
        RateLimiter shortWindow = new RateLimiter(5, Duration.ofMillis(20));
        String key = "client-1";

        // Make some requests
        shortWindow.tryAcquire(key);
        shortWindow.tryAcquire(key);

        // Wait for entries to become stale (window * 2)
        Thread.sleep(50);

        // Cleanup
        shortWindow.cleanup();

        // Key should have full quota again
        assertThat(shortWindow.getRemainingRequests(key)).isEqualTo(5);
    }

    @Test
    void cleanup_shouldKeepRecentEntries() {
        String key = "client-1";

        rateLimiter.tryAcquire(key);
        rateLimiter.tryAcquire(key);

        rateLimiter.cleanup();

        // Recent entries should not be removed
        assertThat(rateLimiter.getRemainingRequests(key)).isEqualTo(3);
    }

    // ==================== Edge Cases ====================

    @Test
    void shouldHandleNullKey() {
        // This tests defensive behavior - implementation uses ConcurrentHashMap
        // which doesn't allow null keys, but the API doesn't explicitly reject them
        try {
            rateLimiter.tryAcquire(null);
        } catch (NullPointerException e) {
            // Expected for ConcurrentHashMap
        }
    }

    @Test
    void shouldHandleEmptyKey() {
        assertThat(rateLimiter.tryAcquire("")).isTrue();
        assertThat(rateLimiter.getRemainingRequests("")).isEqualTo(4);
    }
}
