package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertEventTest {

    @Test
    void shouldBuildAlertEvent() {
        Instant now = Instant.now();
        Map<String, String> labels = Map.of("service", "api", "env", "prod");

        AlertEvent event = AlertEvent.builder()
                .id("event-1")
                .alertId("alert-1")
                .alertName("High Error Rate")
                .severity(AlertSeverity.CRITICAL)
                .status(AlertEventStatus.FIRING)
                .message("Error rate is 10%, threshold is 5%")
                .labels(labels)
                .firedAt(now)
                .build();

        assertEquals("event-1", event.id());
        assertEquals("alert-1", event.alertId());
        assertEquals("High Error Rate", event.alertName());
        assertEquals(AlertSeverity.CRITICAL, event.severity());
        assertEquals(AlertEventStatus.FIRING, event.status());
        assertEquals("Error rate is 10%, threshold is 5%", event.message());
        assertEquals(labels, event.labels());
        assertEquals(now, event.firedAt());
    }

    @Test
    void shouldGenerateIdIfNotProvided() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        assertNotNull(event.id());
        assertFalse(event.id().isEmpty());
    }

    @Test
    void shouldDefaultSeverityToWarning() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        assertEquals(AlertSeverity.WARNING, event.severity());
    }

    @Test
    void shouldDefaultStatusToFiring() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        assertEquals(AlertEventStatus.FIRING, event.status());
    }

    @Test
    void shouldDefaultLabelsToEmptyMap() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        assertNotNull(event.labels());
        assertTrue(event.labels().isEmpty());
    }

    @Test
    void shouldDefaultFiredAtToNow() {
        Instant before = Instant.now();
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();
        Instant after = Instant.now();

        assertNotNull(event.firedAt());
        assertTrue(event.firedAt().compareTo(before) >= 0);
        assertTrue(event.firedAt().compareTo(after) <= 0);
    }

    @Test
    void shouldRequireAlertId() {
        assertThrows(NullPointerException.class, () ->
                AlertEvent.builder()
                        .alertName("Test Alert")
                        .message("Test message")
                        .build()
        );
    }

    @Test
    void shouldRequireAlertName() {
        assertThrows(NullPointerException.class, () ->
                AlertEvent.builder()
                        .alertId("alert-1")
                        .message("Test message")
                        .build()
        );
    }

    @Test
    void shouldRequireMessage() {
        assertThrows(NullPointerException.class, () ->
                AlertEvent.builder()
                        .alertId("alert-1")
                        .alertName("Test Alert")
                        .build()
        );
    }

    @Test
    void shouldAcknowledgeEvent() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        AlertEvent acknowledged = event.acknowledge("operator@example.com");

        assertEquals(event.id(), acknowledged.id());
        assertEquals(event.alertId(), acknowledged.alertId());
        assertEquals(event.firedAt(), acknowledged.firedAt());
        assertEquals(AlertEventStatus.ACKNOWLEDGED, acknowledged.status());
        assertEquals("operator@example.com", acknowledged.acknowledgedBy());
        assertNotNull(acknowledged.acknowledgedAt());
    }

    @Test
    void shouldResolveEvent() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .build();

        AlertEvent resolved = event.resolve("operator@example.com");

        assertEquals(event.id(), resolved.id());
        assertEquals(event.alertId(), resolved.alertId());
        assertEquals(event.firedAt(), resolved.firedAt());
        assertEquals(AlertEventStatus.RESOLVED, resolved.status());
        assertEquals("operator@example.com", resolved.resolvedBy());
        assertNotNull(resolved.resolvedAt());
    }

    @Test
    void shouldIdentifyActiveEvent() {
        AlertEvent firing = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("Test Alert")
                .message("Test message")
                .status(AlertEventStatus.FIRING)
                .build();

        AlertEvent acknowledged = AlertEvent.builder()
                .alertId("alert-2")
                .alertName("Test Alert")
                .message("Test message")
                .status(AlertEventStatus.ACKNOWLEDGED)
                .build();

        AlertEvent resolved = AlertEvent.builder()
                .alertId("alert-3")
                .alertName("Test Alert")
                .message("Test message")
                .status(AlertEventStatus.RESOLVED)
                .build();

        assertTrue(firing.isActive());
        assertTrue(acknowledged.isActive());
        assertFalse(resolved.isActive());
    }
}
