package io.github.jobs.spring.alert;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.ThrottleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AlertThrottler.
 */
class AlertThrottlerTest {

    private AlertThrottler throttler;
    private ThrottleConfig config;

    @BeforeEach
    void setUp() {
        config = new ThrottleConfig(
                5,                          // rateLimit
                Duration.ofSeconds(1),      // ratePeriod
                Duration.ofMillis(100),     // cooldown
                false,                      // grouping
                Duration.ofSeconds(30),     // groupWait
                Duration.ofHours(4)         // repeatInterval
        );
        throttler = new AlertThrottler(config);
    }

    private AlertEvent createEvent(String alertId) {
        return AlertEvent.builder()
                .alertId(alertId)
                .alertName("test-alert")
                .message("Test message")
                .build();
    }

    // ==================== shouldSend ====================

    @Test
    void shouldSend_shouldAllowFirstAlert() {
        AlertEvent event = createEvent("alert-1");
        assertThat(throttler.shouldSend(event)).isTrue();
    }

    @Test
    void shouldSend_shouldBlockAlertInCooldownPeriod() {
        AlertEvent event = createEvent("alert-1");

        // Record first alert
        throttler.recordSent(event);

        // Second alert should be blocked (in cooldown)
        assertThat(throttler.shouldSend(event)).isFalse();
    }

    @Test
    void shouldSend_shouldAllowDifferentAlertsIndependently() {
        AlertEvent event1 = createEvent("alert-1");
        AlertEvent event2 = createEvent("alert-2");

        throttler.recordSent(event1);

        // Different alert should be allowed
        assertThat(throttler.shouldSend(event2)).isTrue();
    }

    @Test
    void shouldSend_shouldAllowAlertAfterCooldownExpires() throws InterruptedException {
        AlertEvent event = createEvent("alert-1");

        throttler.recordSent(event);

        // Wait for cooldown to expire
        Thread.sleep(150);

        assertThat(throttler.shouldSend(event)).isTrue();
    }

    @Test
    void shouldSend_shouldEnforceGlobalRateLimit() {
        // Send 5 alerts (the rate limit)
        for (int i = 0; i < 5; i++) {
            AlertEvent event = createEvent("alert-" + i);
            throttler.recordSent(event);
        }

        // 6th alert should be blocked by rate limit
        AlertEvent event = createEvent("alert-new");
        assertThat(throttler.shouldSend(event)).isFalse();
    }

    // ==================== tryAcquire ====================

    @Test
    void tryAcquire_shouldAllowAndRecord() {
        AlertEvent event = createEvent("alert-1");

        assertThat(throttler.tryAcquire(event)).isTrue();

        // Second acquire should fail (cooldown)
        assertThat(throttler.tryAcquire(event)).isFalse();
    }

    @Test
    void tryAcquire_shouldReturnFalseWhenThrottled() {
        AlertEvent event = createEvent("alert-1");

        throttler.recordSent(event);

        assertThat(throttler.tryAcquire(event)).isFalse();
    }

    // ==================== reset ====================

    @Test
    void reset_shouldClearSpecificAlertCooldown() {
        AlertEvent event = createEvent("alert-1");
        AlertEvent event2 = createEvent("alert-2");

        throttler.recordSent(event);
        throttler.recordSent(event2);

        // Both should be in cooldown
        assertThat(throttler.shouldSend(event)).isFalse();
        assertThat(throttler.shouldSend(event2)).isFalse();

        // Reset alert-1
        throttler.reset("alert-1");

        // alert-1 should be allowed, alert-2 still in cooldown
        assertThat(throttler.shouldSend(event)).isTrue();
        assertThat(throttler.shouldSend(event2)).isFalse();
    }

    // ==================== resetAll ====================

    @Test
    void resetAll_shouldClearAllState() {
        // Record multiple alerts
        for (int i = 0; i < 5; i++) {
            AlertEvent event = createEvent("alert-" + i);
            throttler.recordSent(event);
        }

        // Reset all
        throttler.resetAll();

        // All alerts should be allowed now
        for (int i = 0; i < 5; i++) {
            AlertEvent event = createEvent("alert-" + i);
            assertThat(throttler.shouldSend(event)).isTrue();
        }
    }

    // ==================== Rate Limit Window ====================

    @Test
    void shouldAllowAlertsAfterRatePeriodExpires() throws InterruptedException {
        // Use short rate period
        ThrottleConfig shortConfig = new ThrottleConfig(
                2, Duration.ofMillis(50), Duration.ofMillis(10),
                false, Duration.ofSeconds(30), Duration.ofHours(4)
        );
        AlertThrottler shortThrottler = new AlertThrottler(shortConfig);

        // Use up rate limit
        shortThrottler.recordSent(createEvent("alert-1"));
        shortThrottler.recordSent(createEvent("alert-2"));

        // Should be rate limited
        assertThat(shortThrottler.shouldSend(createEvent("alert-3"))).isFalse();

        // Wait for rate period to expire
        Thread.sleep(60);

        // Should be allowed now
        assertThat(shortThrottler.shouldSend(createEvent("alert-3"))).isTrue();
    }

    // ==================== getConfig ====================

    @Test
    void getConfig_shouldReturnConfig() {
        assertThat(throttler.getConfig()).isEqualTo(config);
    }
}
