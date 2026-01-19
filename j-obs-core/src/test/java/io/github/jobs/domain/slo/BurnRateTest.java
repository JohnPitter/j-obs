package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BurnRateTest {

    @Test
    void shouldCalculateBurnRate() {
        // 0.05% consumed in 12 hours of a 30-day window with 0.1% budget
        BurnRate rate = BurnRate.calculate(
                0.05,           // consumed
                0.1,            // total budget
                Duration.ofHours(12),
                Duration.ofDays(30),
                Duration.ofHours(1)
        );

        // Expected rate calculation:
        // Expected consumption in 12 hours = 0.1 / (30*24) * 12 = 0.00167%
        // Actual consumption = 0.05%
        // Burn rate = 0.05 / 0.00167 = ~30x
        assertTrue(rate.rate() > 0);
        assertNotNull(rate.window());
        assertNotNull(rate.calculatedAt());
    }

    @Test
    void shouldIdentifySafeBurnRate() {
        BurnRate safe = new BurnRate(0.5, Duration.ofHours(1), Duration.ofDays(30), Instant.now());
        assertTrue(safe.isSafe());
        assertFalse(safe.isElevated());
        assertFalse(safe.isHigh());
        assertFalse(safe.isCritical());
    }

    @Test
    void shouldIdentifyElevatedBurnRate() {
        BurnRate elevated = new BurnRate(1.5, Duration.ofHours(1), Duration.ofDays(10), Instant.now());
        assertFalse(elevated.isSafe());
        assertTrue(elevated.isElevated());
        assertFalse(elevated.isHigh());
        assertFalse(elevated.isCritical());
    }

    @Test
    void shouldIdentifyHighBurnRate() {
        BurnRate high = new BurnRate(5.0, Duration.ofHours(1), Duration.ofDays(5), Instant.now());
        assertFalse(high.isSafe());
        assertFalse(high.isElevated());
        assertTrue(high.isHigh());
        assertFalse(high.isCritical());
    }

    @Test
    void shouldIdentifyCriticalBurnRate() {
        BurnRate critical = new BurnRate(15.0, Duration.ofHours(1), Duration.ofDays(1), Instant.now());
        assertFalse(critical.isSafe());
        assertFalse(critical.isElevated());
        assertFalse(critical.isHigh());
        assertTrue(critical.isCritical());
    }

    @Test
    void shouldReturnCorrectSeverity() {
        assertEquals("safe", new BurnRate(0.5, Duration.ofHours(1), Duration.ZERO, Instant.now()).severity());
        assertEquals("elevated", new BurnRate(1.5, Duration.ofHours(1), Duration.ZERO, Instant.now()).severity());
        assertEquals("high", new BurnRate(5.0, Duration.ofHours(1), Duration.ZERO, Instant.now()).severity());
        assertEquals("critical", new BurnRate(15.0, Duration.ofHours(1), Duration.ZERO, Instant.now()).severity());
    }

    @Test
    void shouldFormatBurnRate() {
        BurnRate rate = new BurnRate(2.5, Duration.ofHours(1), Duration.ZERO, Instant.now());
        assertEquals("2.5x", rate.format());
    }

    @Test
    void shouldCreateZeroBurnRate() {
        BurnRate zero = BurnRate.zero(Duration.ofHours(1));
        assertEquals(0.0, zero.rate());
        assertEquals(Duration.ofHours(1), zero.window());
    }

    @Test
    void shouldFormatProjectedExhaustion() {
        BurnRate rate = new BurnRate(1.0, Duration.ofHours(1), Duration.ofHours(5), Instant.now());
        String formatted = rate.formatProjectedExhaustion();
        assertNotNull(formatted);
        assertTrue(formatted.contains("5h"));
    }

    @Test
    void shouldHandleZeroProjectedExhaustion() {
        BurnRate rate = new BurnRate(1.0, Duration.ofHours(1), Duration.ZERO, Instant.now());
        assertEquals("N/A", rate.formatProjectedExhaustion());
    }
}
