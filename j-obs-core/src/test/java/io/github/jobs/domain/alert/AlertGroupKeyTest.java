package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertGroupKeyTest {

    @Test
    void shouldCreateSimpleKey() {
        AlertGroupKey key = AlertGroupKey.simple("high-cpu", AlertSeverity.CRITICAL);

        assertEquals("high-cpu", key.alertName());
        assertEquals(AlertSeverity.CRITICAL, key.severity());
        assertTrue(key.groupLabels().isEmpty());
    }

    @Test
    void shouldCreateKeyWithLabels() {
        Map<String, String> labels = Map.of("service", "api", "instance", "pod-1");
        AlertGroupKey key = new AlertGroupKey("high-cpu", AlertSeverity.WARNING, labels);

        assertEquals("high-cpu", key.alertName());
        assertEquals(AlertSeverity.WARNING, key.severity());
        assertEquals(2, key.groupLabels().size());
        assertEquals("api", key.groupLabels().get("service"));
    }

    @Test
    void shouldCreateKeyFromEvent() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-memory")
                .severity(AlertSeverity.WARNING)
                .message("Memory usage high")
                .labels(Map.of("service", "api", "instance", "pod-1", "region", "us-east"))
                .build();

        List<String> groupByLabels = List.of("service", "instance");
        AlertGroupKey key = AlertGroupKey.fromEvent(event, groupByLabels);

        assertEquals("high-memory", key.alertName());
        assertEquals(AlertSeverity.WARNING, key.severity());
        assertEquals(2, key.groupLabels().size());
        assertEquals("api", key.groupLabels().get("service"));
        assertEquals("pod-1", key.groupLabels().get("instance"));
        assertNull(key.groupLabels().get("region")); // Not in groupByLabels
    }

    @Test
    void shouldHandleNullGroupByLabels() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .build();

        AlertGroupKey key = AlertGroupKey.fromEvent(event, null);

        assertEquals("high-cpu", key.alertName());
        assertTrue(key.groupLabels().isEmpty());
    }

    @Test
    void shouldHandleMissingLabels() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "api"))
                .build();

        List<String> groupByLabels = List.of("service", "instance"); // instance not in event
        AlertGroupKey key = AlertGroupKey.fromEvent(event, groupByLabels);

        assertEquals(1, key.groupLabels().size());
        assertEquals("api", key.groupLabels().get("service"));
    }

    @Test
    void shouldMatchEventWithSameKey() {
        AlertEvent event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "api"))
                .build();

        List<String> groupByLabels = List.of("service");
        AlertGroupKey key = AlertGroupKey.fromEvent(event, groupByLabels);

        assertTrue(key.matches(event, groupByLabels));
    }

    @Test
    void shouldNotMatchEventWithDifferentKey() {
        AlertEvent event1 = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "api"))
                .build();

        AlertEvent event2 = AlertEvent.builder()
                .alertId("alert-2")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "web"))
                .build();

        List<String> groupByLabels = List.of("service");
        AlertGroupKey key = AlertGroupKey.fromEvent(event1, groupByLabels);

        assertFalse(key.matches(event2, groupByLabels));
    }

    @Test
    void shouldHaveConsistentEquality() {
        Map<String, String> labels1 = Map.of("b", "2", "a", "1");
        Map<String, String> labels2 = Map.of("a", "1", "b", "2");

        AlertGroupKey key1 = new AlertGroupKey("alert", AlertSeverity.WARNING, labels1);
        AlertGroupKey key2 = new AlertGroupKey("alert", AlertSeverity.WARNING, labels2);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void shouldGenerateDisplayString() {
        Map<String, String> labels = Map.of("service", "api");
        AlertGroupKey key = new AlertGroupKey("high-cpu", AlertSeverity.CRITICAL, labels);

        String display = key.toDisplayString();

        assertTrue(display.contains("high-cpu"));
        assertTrue(display.contains("CRITICAL"));
        assertTrue(display.contains("service=api"));
    }

    @Test
    void shouldGenerateDisplayStringWithoutLabels() {
        AlertGroupKey key = AlertGroupKey.simple("high-cpu", AlertSeverity.WARNING);

        String display = key.toDisplayString();

        assertEquals("high-cpu [WARNING]", display);
    }

    @Test
    void shouldRejectNullAlertName() {
        assertThrows(NullPointerException.class, () ->
                new AlertGroupKey(null, AlertSeverity.WARNING, Map.of()));
    }

    @Test
    void shouldRejectNullSeverity() {
        assertThrows(NullPointerException.class, () ->
                new AlertGroupKey("alert", null, Map.of()));
    }

    @Test
    void shouldHandleNullLabels() {
        AlertGroupKey key = new AlertGroupKey("alert", AlertSeverity.WARNING, null);

        assertNotNull(key.groupLabels());
        assertTrue(key.groupLabels().isEmpty());
    }
}
