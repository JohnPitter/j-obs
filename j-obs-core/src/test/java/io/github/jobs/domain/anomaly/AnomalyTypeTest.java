package io.github.jobs.domain.anomaly;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyTypeTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Latency Spike", AnomalyType.LATENCY_SPIKE.displayName());
        assertEquals("Error Rate Spike", AnomalyType.ERROR_RATE_SPIKE.displayName());
        assertEquals("Traffic Spike", AnomalyType.TRAFFIC_SPIKE.displayName());
        assertEquals("Traffic Drop", AnomalyType.TRAFFIC_DROP.displayName());
        assertEquals("Slow Dependency", AnomalyType.SLOW_DEPENDENCY.displayName());
    }

    @Test
    void shouldIdentifyCriticalAnomalies() {
        assertTrue(AnomalyType.LATENCY_SPIKE.isCritical());
        assertTrue(AnomalyType.ERROR_RATE_SPIKE.isCritical());
    }

    @Test
    void shouldIdentifyWarningAnomalies() {
        assertTrue(AnomalyType.TRAFFIC_SPIKE.isWarning());
        assertTrue(AnomalyType.TRAFFIC_DROP.isWarning());
        assertTrue(AnomalyType.SLOW_DEPENDENCY.isWarning());
    }

    @Test
    void shouldHaveCorrectSeverity() {
        assertEquals("critical", AnomalyType.LATENCY_SPIKE.severity());
        assertEquals("warning", AnomalyType.TRAFFIC_SPIKE.severity());
    }

    @Test
    void shouldHaveCssClasses() {
        assertNotNull(AnomalyType.LATENCY_SPIKE.cssClass());
        assertNotNull(AnomalyType.LATENCY_SPIKE.bgCssClass());
        assertTrue(AnomalyType.LATENCY_SPIKE.cssClass().contains("red"));
        assertTrue(AnomalyType.TRAFFIC_SPIKE.cssClass().contains("amber"));
    }

    @Test
    void shouldHaveDescriptions() {
        for (AnomalyType type : AnomalyType.values()) {
            assertNotNull(type.description(), "Description should not be null for " + type.name());
            assertFalse(type.description().isEmpty(), "Description should not be empty for " + type.name());
        }
    }

    @Test
    void shouldHaveEmoji() {
        assertNotNull(AnomalyType.LATENCY_SPIKE.emoji());
        assertNotNull(AnomalyType.TRAFFIC_SPIKE.emoji());
    }
}
