package io.github.jobs.domain.alert;

import io.github.jobs.domain.alert.AlertCondition.ComparisonOperator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    private AlertCondition createCondition() {
        return AlertCondition.of("error_rate", ComparisonOperator.GREATER_THAN, 5);
    }

    @Test
    void shouldBuildAlert() {
        Instant now = Instant.now();
        AlertCondition condition = createCondition();

        Alert alert = Alert.builder()
                .id("alert-1")
                .name("High Error Rate")
                .description("Alert when error rate exceeds 5%")
                .type(AlertType.METRIC)
                .condition(condition)
                .severity(AlertSeverity.CRITICAL)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals("alert-1", alert.id());
        assertEquals("High Error Rate", alert.name());
        assertEquals("Alert when error rate exceeds 5%", alert.description());
        assertEquals(AlertType.METRIC, alert.type());
        assertEquals(condition, alert.condition());
        assertEquals(AlertSeverity.CRITICAL, alert.severity());
        assertTrue(alert.enabled());
        assertEquals(now, alert.createdAt());
        assertEquals(now, alert.updatedAt());
    }

    @Test
    void shouldGenerateIdIfNotProvided() {
        Alert alert = Alert.builder()
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .build();

        assertNotNull(alert.id());
        assertFalse(alert.id().isEmpty());
    }

    @Test
    void shouldDefaultSeverityToWarning() {
        Alert alert = Alert.builder()
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .build();

        assertEquals(AlertSeverity.WARNING, alert.severity());
    }

    @Test
    void shouldDefaultEnabledToTrue() {
        Alert alert = Alert.builder()
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .build();

        assertTrue(alert.enabled());
    }

    @Test
    void shouldDefaultCreatedAtToNow() {
        Instant before = Instant.now();
        Alert alert = Alert.builder()
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .build();
        Instant after = Instant.now();

        assertNotNull(alert.createdAt());
        assertTrue(alert.createdAt().compareTo(before) >= 0);
        assertTrue(alert.createdAt().compareTo(after) <= 0);
    }

    @Test
    void shouldDefaultUpdatedAtToCreatedAt() {
        Alert alert = Alert.builder()
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .build();

        assertEquals(alert.createdAt(), alert.updatedAt());
    }

    @Test
    void shouldRequireName() {
        assertThrows(NullPointerException.class, () ->
                Alert.builder()
                        .type(AlertType.LOG)
                        .condition(createCondition())
                        .build()
        );
    }

    @Test
    void shouldRequireType() {
        assertThrows(NullPointerException.class, () ->
                Alert.builder()
                        .name("Test Alert")
                        .condition(createCondition())
                        .build()
        );
    }

    @Test
    void shouldRequireCondition() {
        assertThrows(NullPointerException.class, () ->
                Alert.builder()
                        .name("Test Alert")
                        .type(AlertType.LOG)
                        .build()
        );
    }

    @Test
    void shouldCreateCopyWithEnabledStatus() {
        Alert original = Alert.builder()
                .id("alert-1")
                .name("Test Alert")
                .type(AlertType.LOG)
                .condition(createCondition())
                .enabled(true)
                .build();

        Alert disabled = original.withEnabled(false);

        assertEquals(original.id(), disabled.id());
        assertEquals(original.name(), disabled.name());
        assertEquals(original.type(), disabled.type());
        assertEquals(original.condition(), disabled.condition());
        assertEquals(original.createdAt(), disabled.createdAt());
        assertTrue(original.enabled());
        assertFalse(disabled.enabled());
        // updatedAt should be updated
        assertTrue(disabled.updatedAt().compareTo(original.updatedAt()) >= 0);
    }
}
