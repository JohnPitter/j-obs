package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertTypeTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Log Alert", AlertType.LOG.displayName());
        assertEquals("Trace Alert", AlertType.TRACE.displayName());
        assertEquals("Health Alert", AlertType.HEALTH.displayName());
        assertEquals("Metric Alert", AlertType.METRIC.displayName());
    }

    @Test
    void shouldHaveDescriptions() {
        for (AlertType type : AlertType.values()) {
            assertNotNull(type.description(), "description should not be null for " + type.name());
            assertFalse(type.description().isEmpty(), "description should not be empty for " + type.name());
        }
    }

    @Test
    void shouldHaveIcons() {
        for (AlertType type : AlertType.values()) {
            assertNotNull(type.icon(), "icon should not be null for " + type.name());
            assertFalse(type.icon().isEmpty(), "icon should not be empty for " + type.name());
        }
    }

    @Test
    void shouldHaveExpectedIcons() {
        assertEquals("📝", AlertType.LOG.icon());
        assertEquals("🔗", AlertType.TRACE.icon());
        assertEquals("💚", AlertType.HEALTH.icon());
        assertEquals("📊", AlertType.METRIC.icon());
    }
}
