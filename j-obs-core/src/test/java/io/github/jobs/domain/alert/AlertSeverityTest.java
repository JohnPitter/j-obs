package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertSeverityTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Info", AlertSeverity.INFO.displayName());
        assertEquals("Warning", AlertSeverity.WARNING.displayName());
        assertEquals("Critical", AlertSeverity.CRITICAL.displayName());
    }

    @Test
    void shouldHaveCssClasses() {
        for (AlertSeverity severity : AlertSeverity.values()) {
            assertNotNull(severity.textCssClass(), "textCssClass should not be null for " + severity.name());
            assertNotNull(severity.bgCssClass(), "bgCssClass should not be null for " + severity.name());
        }
    }

    @Test
    void shouldHaveEmoji() {
        for (AlertSeverity severity : AlertSeverity.values()) {
            assertNotNull(severity.emoji(), "emoji should not be null for " + severity.name());
            assertFalse(severity.emoji().isEmpty(), "emoji should not be empty for " + severity.name());
        }
    }

    @Test
    void shouldHaveExpectedCssColors() {
        assertTrue(AlertSeverity.INFO.textCssClass().contains("blue"));
        assertTrue(AlertSeverity.WARNING.textCssClass().contains("yellow"));
        assertTrue(AlertSeverity.CRITICAL.textCssClass().contains("red"));
    }
}
