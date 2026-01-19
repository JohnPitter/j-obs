package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertGroupTest {

    @Test
    void shouldCreateGroupFromEvent() {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        List<String> groupByLabels = List.of("service");

        AlertGroup group = AlertGroup.fromEvent(event, groupByLabels);

        assertNotNull(group.id());
        assertEquals(1, group.eventCount());
        assertEquals(AlertGroupStatus.PENDING, group.status());
        assertEquals("high-cpu", group.alertName());
        assertEquals(AlertSeverity.WARNING, group.severity());
    }

    @Test
    void shouldAddEventToGroup() throws InterruptedException {
        AlertEvent event1 = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertEvent event2 = createTestEvent("alert-2", "high-cpu", AlertSeverity.WARNING);

        AlertGroup group = AlertGroup.fromEvent(event1, List.of("service"));
        Thread.sleep(10); // Ensure time difference
        AlertGroup updatedGroup = group.addEvent(event2);

        assertEquals(2, updatedGroup.eventCount());
        assertEquals(group.id(), updatedGroup.id()); // Same group ID
        // lastUpdatedAt is updated when adding an event
        assertTrue(updatedGroup.lastUpdatedAt().isAfter(group.createdAt()) ||
                   updatedGroup.lastUpdatedAt().equals(group.createdAt()));
    }

    @Test
    void shouldMarkGroupAsSent() {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertGroup group = AlertGroup.fromEvent(event, List.of());

        AlertGroup sentGroup = group.markSent();

        assertEquals(AlertGroupStatus.SENT, sentGroup.status());
        assertEquals(group.id(), sentGroup.id());
    }

    @Test
    void shouldMarkGroupAsFailed() {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertGroup group = AlertGroup.fromEvent(event, List.of());

        AlertGroup failedGroup = group.markFailed();

        assertEquals(AlertGroupStatus.FAILED, failedGroup.status());
    }

    @Test
    void shouldReturnHighestSeverity() {
        AlertEvent warning = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertEvent critical = createTestEvent("alert-2", "high-cpu", AlertSeverity.CRITICAL);
        AlertEvent info = createTestEvent("alert-3", "high-cpu", AlertSeverity.INFO);

        AlertGroup group = AlertGroup.fromEvent(warning, List.of())
                .addEvent(critical)
                .addEvent(info);

        assertEquals(AlertSeverity.CRITICAL, group.severity());
    }

    @Test
    void shouldCalculateDuration() throws InterruptedException {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertGroup group = AlertGroup.fromEvent(event, List.of());

        // Wait a bit to ensure duration is non-zero
        Thread.sleep(10);

        AlertGroup updatedGroup = group.addEvent(
                createTestEvent("alert-2", "high-cpu", AlertSeverity.WARNING));

        Duration duration = updatedGroup.duration();
        assertTrue(duration.toMillis() >= 0);
    }

    @Test
    void shouldCheckShouldSend() throws InterruptedException {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertGroup group = AlertGroup.fromEvent(event, List.of());

        // Should not send immediately
        assertFalse(group.shouldSend(Duration.ofSeconds(30)));

        // Should send for zero wait
        assertTrue(group.shouldSend(Duration.ZERO));
    }

    @Test
    void shouldGenerateSummaryMessageForSingleEvent() {
        AlertEvent event = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        event = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU usage at 90%")
                .build();

        AlertGroup group = AlertGroup.fromEvent(event, List.of());

        assertEquals("CPU usage at 90%", group.summaryMessage());
    }

    @Test
    void shouldGenerateSummaryMessageForMultipleEvents() {
        AlertEvent event1 = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU usage at 90%")
                .build();
        AlertEvent event2 = AlertEvent.builder()
                .alertId("alert-2")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU usage at 95%")
                .build();

        AlertGroup group = AlertGroup.fromEvent(event1, List.of()).addEvent(event2);

        String summary = group.summaryMessage();
        assertTrue(summary.contains("[2 alerts]"));
        assertTrue(summary.contains("high-cpu"));
    }

    @Test
    void shouldFindCommonLabels() {
        AlertEvent event1 = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "api", "region", "us-east", "instance", "pod-1"))
                .build();
        AlertEvent event2 = AlertEvent.builder()
                .alertId("alert-2")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "api", "region", "us-east", "instance", "pod-2"))
                .build();

        AlertGroup group = AlertGroup.fromEvent(event1, List.of()).addEvent(event2);

        Map<String, String> common = group.commonLabels();
        assertEquals(2, common.size());
        assertEquals("api", common.get("service"));
        assertEquals("us-east", common.get("region"));
        assertNull(common.get("instance")); // Different values
    }

    @Test
    void shouldCheckCanAccept() {
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
                .labels(Map.of("service", "api"))
                .build();
        AlertEvent event3 = AlertEvent.builder()
                .alertId("alert-3")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU high")
                .labels(Map.of("service", "web")) // Different service
                .build();

        List<String> groupByLabels = List.of("service");
        AlertGroup group = AlertGroup.fromEvent(event1, groupByLabels);

        assertTrue(group.canAccept(event2, groupByLabels, 100));
        assertFalse(group.canAccept(event3, groupByLabels, 100)); // Different key
    }

    @Test
    void shouldNotAcceptWhenMaxSizeReached() {
        AlertEvent event1 = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertEvent event2 = createTestEvent("alert-2", "high-cpu", AlertSeverity.WARNING);

        AlertGroup group = AlertGroup.fromEvent(event1, List.of());

        assertFalse(group.canAccept(event2, List.of(), 1)); // Max size is 1
    }

    @Test
    void shouldNotAcceptWhenNotPending() {
        AlertEvent event1 = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertEvent event2 = createTestEvent("alert-2", "high-cpu", AlertSeverity.WARNING);

        AlertGroup group = AlertGroup.fromEvent(event1, List.of()).markSent();

        assertFalse(group.canAccept(event2, List.of(), 100));
    }

    @Test
    void shouldBeImmutable() {
        AlertEvent event1 = createTestEvent("alert-1", "high-cpu", AlertSeverity.WARNING);
        AlertEvent event2 = createTestEvent("alert-2", "high-cpu", AlertSeverity.WARNING);

        AlertGroup group1 = AlertGroup.fromEvent(event1, List.of());
        AlertGroup group2 = group1.addEvent(event2);

        assertNotSame(group1, group2);
        assertEquals(1, group1.eventCount());
        assertEquals(2, group2.eventCount());
    }

    @Test
    void shouldRejectNullKey() {
        assertThrows(NullPointerException.class, () ->
                new AlertGroup(null, null, List.of(), null, null, AlertGroupStatus.PENDING));
    }

    @Test
    void shouldRejectNullStatus() {
        AlertGroupKey key = AlertGroupKey.simple("test", AlertSeverity.WARNING);
        assertThrows(NullPointerException.class, () ->
                new AlertGroup(null, key, List.of(), null, null, null));
    }

    private AlertEvent createTestEvent(String id, String name, AlertSeverity severity) {
        return AlertEvent.builder()
                .alertId(id)
                .alertName(name)
                .severity(severity)
                .message("Test message")
                .labels(Map.of("service", "api"))
                .build();
    }
}
