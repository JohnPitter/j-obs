package io.github.jobs.domain.anomaly;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyTest {

    @Test
    void shouldBuildAnomaly() {
        Instant now = Instant.now();
        List<PossibleCause> causes = List.of(
                new PossibleCause("Test cause", PossibleCause.Confidence.HIGH)
        );

        Anomaly anomaly = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .metric("latency_p99")
                .endpoint("GET /api/users")
                .service("user-service")
                .baselineValue(100.0)
                .currentValue(500.0)
                .deviation(3.5)
                .percentageChange(400.0)
                .detectedAt(now)
                .possibleCauses(causes)
                .status(AnomalyStatus.ACTIVE)
                .build();

        assertEquals("anomaly-1", anomaly.id());
        assertEquals(AnomalyType.LATENCY_SPIKE, anomaly.type());
        assertEquals("latency_p99", anomaly.metric());
        assertEquals("GET /api/users", anomaly.endpoint());
        assertEquals("user-service", anomaly.service());
        assertEquals(100.0, anomaly.baselineValue());
        assertEquals(500.0, anomaly.currentValue());
        assertEquals(3.5, anomaly.deviation());
        assertEquals(400.0, anomaly.percentageChange());
        assertEquals(now, anomaly.detectedAt());
        assertEquals(1, anomaly.possibleCauses().size());
        assertEquals(AnomalyStatus.ACTIVE, anomaly.status());
    }

    @Test
    void shouldRequireIdAndType() {
        assertThrows(NullPointerException.class, () ->
                Anomaly.builder()
                        .type(AnomalyType.LATENCY_SPIKE)
                        .build()
        );

        assertThrows(NullPointerException.class, () ->
                Anomaly.builder()
                        .id("anomaly-1")
                        .build()
        );
    }

    @Test
    void shouldDefaultStatusToActive() {
        Anomaly anomaly = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        assertEquals(AnomalyStatus.ACTIVE, anomaly.status());
    }

    @Test
    void shouldDefaultDetectedAtToNow() {
        Instant before = Instant.now();
        Anomaly anomaly = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();
        Instant after = Instant.now();

        assertNotNull(anomaly.detectedAt());
        assertTrue(anomaly.detectedAt().compareTo(before) >= 0);
        assertTrue(anomaly.detectedAt().compareTo(after) <= 0);
    }

    @Test
    void shouldDefaultPossibleCausesToEmptyList() {
        Anomaly anomaly = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        assertNotNull(anomaly.possibleCauses());
        assertTrue(anomaly.possibleCauses().isEmpty());
    }

    @Test
    void shouldIdentifyActiveAnomaly() {
        Anomaly active = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .status(AnomalyStatus.ACTIVE)
                .build();

        Anomaly resolved = Anomaly.builder()
                .id("anomaly-2")
                .type(AnomalyType.LATENCY_SPIKE)
                .status(AnomalyStatus.RESOLVED)
                .build();

        assertTrue(active.isActive());
        assertFalse(active.isResolved());
        assertFalse(resolved.isActive());
        assertTrue(resolved.isResolved());
    }

    @Test
    void shouldIdentifyCriticalAnomaly() {
        Anomaly critical = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        Anomaly warning = Anomaly.builder()
                .id("anomaly-2")
                .type(AnomalyType.TRAFFIC_SPIKE)
                .build();

        assertTrue(critical.isCritical());
        assertFalse(warning.isCritical());
    }

    @Test
    void shouldGenerateSummary() {
        Anomaly anomaly = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .baselineValue(100.0)
                .currentValue(500.0)
                .percentageChange(400.0)
                .build();

        String summary = anomaly.summary();
        assertTrue(summary.contains("Latency Spike"));
        assertTrue(summary.contains("100"));
        assertTrue(summary.contains("500"));
        assertTrue(summary.contains("400"));
    }

    @Test
    void shouldCompareByIdForEquality() {
        Anomaly anomaly1 = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        Anomaly anomaly2 = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.ERROR_RATE_SPIKE) // Different type
                .build();

        Anomaly anomaly3 = Anomaly.builder()
                .id("anomaly-2")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        assertEquals(anomaly1, anomaly2); // Same ID = equal
        assertNotEquals(anomaly1, anomaly3); // Different ID = not equal
    }

    @Test
    void shouldHaveConsistentHashCode() {
        Anomaly anomaly1 = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.LATENCY_SPIKE)
                .build();

        Anomaly anomaly2 = Anomaly.builder()
                .id("anomaly-1")
                .type(AnomalyType.ERROR_RATE_SPIKE)
                .build();

        assertEquals(anomaly1.hashCode(), anomaly2.hashCode());
    }
}
