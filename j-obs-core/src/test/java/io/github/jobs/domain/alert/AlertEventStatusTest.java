package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertEventStatusTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Firing", AlertEventStatus.FIRING.displayName());
        assertEquals("Acknowledged", AlertEventStatus.ACKNOWLEDGED.displayName());
        assertEquals("Resolved", AlertEventStatus.RESOLVED.displayName());
    }

    @Test
    void shouldHaveCssClasses() {
        for (AlertEventStatus status : AlertEventStatus.values()) {
            assertNotNull(status.textCssClass(), "textCssClass should not be null for " + status.name());
            assertNotNull(status.bgCssClass(), "bgCssClass should not be null for " + status.name());
        }
    }

    @Test
    void shouldHaveExpectedCssColors() {
        assertTrue(AlertEventStatus.FIRING.textCssClass().contains("red"));
        assertTrue(AlertEventStatus.ACKNOWLEDGED.textCssClass().contains("yellow"));
        assertTrue(AlertEventStatus.RESOLVED.textCssClass().contains("green"));
    }
}
