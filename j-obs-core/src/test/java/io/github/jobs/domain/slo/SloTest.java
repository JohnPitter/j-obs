package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class SloTest {

    @Test
    void shouldCreateAvailabilitySlo() {
        Slo slo = Slo.availability(
                "api-availability",
                "99.9% availability",
                "http_requests_total",
                99.9,
                Duration.ofDays(30)
        );

        assertEquals("api-availability", slo.name());
        assertEquals("99.9% availability", slo.description());
        assertEquals(SliType.AVAILABILITY, slo.sli().type());
        assertEquals(99.9, slo.objective());
        assertEquals(Duration.ofDays(30), slo.window());
    }

    @Test
    void shouldCreateLatencySlo() {
        Slo slo = Slo.latency(
                "api-latency",
                "p99 < 200ms",
                "http_request_duration",
                200.0,
                99,
                99.0,
                Duration.ofDays(7)
        );

        assertEquals("api-latency", slo.name());
        assertEquals(SliType.LATENCY, slo.sli().type());
        assertEquals(200.0, slo.sli().threshold());
        assertEquals(99, slo.sli().percentile());
        assertEquals(99.0, slo.objective());
    }

    @Test
    void shouldUseBuilderPattern() {
        Sli sli = Sli.availability("http_requests_total", "status < 500", "status >= 0");

        Slo slo = Slo.builder()
                .name("custom-slo")
                .description("Custom SLO")
                .sli(sli)
                .objective(99.5)
                .window(Duration.ofDays(14))
                .build();

        assertEquals("custom-slo", slo.name());
        assertEquals(99.5, slo.objective());
        assertEquals(Duration.ofDays(14), slo.window());
    }

    @Test
    void shouldDefaultWindowTo30Days() {
        Sli sli = Sli.availability("m", "g", "t");
        Slo slo = new Slo("test", "desc", sli, 99.0, null, null);

        assertEquals(Duration.ofDays(30), slo.window());
    }

    @Test
    void shouldDefaultBurnRateAlerts() {
        Sli sli = Sli.availability("m", "g", "t");
        Slo slo = new Slo("test", "desc", sli, 99.0, Duration.ofDays(30), null);

        assertNotNull(slo.burnRateAlerts());
        assertNotNull(slo.burnRateAlerts().shortWindow());
        assertNotNull(slo.burnRateAlerts().longWindow());
    }

    @Test
    void shouldCalculateErrorBudget() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));
        ErrorBudget budget = slo.calculateErrorBudget(99.95);

        assertNotNull(budget);
        assertEquals(0.1, budget.totalBudget(), 0.001);
    }

    @Test
    void shouldDetermineHealthyStatus() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        assertEquals(SloStatus.HEALTHY, slo.determineStatus(99.95));
    }

    @Test
    void shouldDetermineAtRiskStatus() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        // For 99.9% SLO, budget is 0.1%
        // At risk when remaining <= 25% of budget
        // Current = 99.82 leaves ~20% budget remaining
        assertEquals(SloStatus.AT_RISK, slo.determineStatus(99.82));
    }

    @Test
    void shouldDetermineBreachedStatus() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        // 99.7 means 0.2% consumed when only 0.1% available = breached
        assertEquals(SloStatus.BREACHED, slo.determineStatus(99.7));
    }

    @Test
    void shouldDetermineNoDataStatus() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        assertEquals(SloStatus.NO_DATA, slo.determineStatus(Double.NaN));
    }

    @Test
    void shouldRequireName() {
        Sli sli = Sli.availability("m", "g", "t");
        assertThrows(NullPointerException.class, () ->
                new Slo(null, "desc", sli, 99.0, null, null));
    }

    @Test
    void shouldRequireSli() {
        assertThrows(NullPointerException.class, () ->
                new Slo("test", "desc", null, 99.0, null, null));
    }

    @Test
    void shouldRejectInvalidObjective() {
        Sli sli = Sli.availability("m", "g", "t");

        assertThrows(IllegalArgumentException.class, () ->
                new Slo("test", "desc", sli, 0, null, null));

        assertThrows(IllegalArgumentException.class, () ->
                new Slo("test", "desc", sli, 101, null, null));
    }
}
