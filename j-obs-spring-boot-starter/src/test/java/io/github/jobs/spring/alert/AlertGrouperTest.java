package io.github.jobs.spring.alert;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertGroup;
import io.github.jobs.domain.alert.AlertGroupKey;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertSeverity;
import io.github.jobs.domain.alert.ThrottleConfig;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertGrouperTest {

    private JObsProperties.Alerts.Throttling config;
    private AlertDispatcher dispatcher;
    private ScheduledExecutorService scheduler;
    private AlertGrouper grouper;

    @BeforeEach
    void setUp() {
        config = new JObsProperties.Alerts.Throttling();
        config.setGrouping(true);
        config.setGroupWait(Duration.ofSeconds(30));
        config.setMaxGroupSize(100);
        config.setGroupByLabels(List.of("service", "instance"));

        dispatcher = mock(AlertDispatcher.class);
        when(dispatcher.dispatch(any())).thenReturn(
                CompletableFuture.completedFuture(List.of(AlertNotificationResult.success("test")))
        );

        scheduler = Executors.newScheduledThreadPool(2);
        grouper = new AlertGrouper(config, dispatcher, scheduler);
    }

    @Test
    void shouldGroupSimilarAlerts() {
        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "api"));

        grouper.addAlert(event1);
        grouper.addAlert(event2);

        assertEquals(1, grouper.pendingGroupCount());
        assertEquals(2, grouper.pendingAlertCount());
    }

    @Test
    void shouldCreateSeparateGroupsForDifferentKeys() {
        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "web"));

        grouper.addAlert(event1);
        grouper.addAlert(event2);

        assertEquals(2, grouper.pendingGroupCount());
        assertEquals(2, grouper.pendingAlertCount());
    }

    @Test
    void shouldDispatchImmediatelyWhenGroupingDisabled() {
        config.setGrouping(false);
        AlertGrouper nonGroupingGrouper = new AlertGrouper(config, dispatcher, scheduler);

        AlertEvent event = createEvent("alert-1", "high-cpu", Map.of("service", "api"));

        nonGroupingGrouper.addAlert(event).join();

        verify(dispatcher).dispatch(event);
        assertEquals(0, nonGroupingGrouper.pendingGroupCount());
    }

    @Test
    void shouldFlushGroupWhenMaxSizeReached() {
        config.setMaxGroupSize(2);
        AlertGrouper smallGrouper = new AlertGrouper(config, dispatcher, scheduler);

        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "api"));

        smallGrouper.addAlert(event1);
        smallGrouper.addAlert(event2).join();

        verify(dispatcher).dispatch(any(AlertEvent.class));
        assertEquals(0, smallGrouper.pendingGroupCount());
    }

    @Test
    void shouldFlushAllGroups() {
        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "web"));

        grouper.addAlert(event1);
        grouper.addAlert(event2);
        assertEquals(2, grouper.pendingGroupCount());

        grouper.flushAll().join();

        verify(dispatcher, times(2)).dispatch(any(AlertEvent.class));
        assertEquals(0, grouper.pendingGroupCount());
    }

    @Test
    void shouldFlushSpecificGroup() {
        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "web"));

        grouper.addAlert(event1);
        grouper.addAlert(event2);

        AlertGroupKey key = AlertGroupKey.fromEvent(event1, config.getGroupByLabels());
        grouper.flushGroup(key).join();

        verify(dispatcher, times(1)).dispatch(any(AlertEvent.class));
        assertEquals(1, grouper.pendingGroupCount());
    }

    @Test
    void shouldFindGroupByKey() {
        AlertEvent event = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        grouper.addAlert(event);

        AlertGroupKey key = AlertGroupKey.fromEvent(event, config.getGroupByLabels());
        var group = grouper.findGroup(key);

        assertTrue(group.isPresent());
        assertEquals(1, group.get().eventCount());
    }

    @Test
    void shouldReturnEmptyForUnknownGroup() {
        AlertGroupKey key = AlertGroupKey.simple("unknown", AlertSeverity.WARNING);
        var group = grouper.findGroup(key);

        assertTrue(group.isEmpty());
    }

    @Test
    void shouldNotifyListeners() {
        AtomicReference<AlertGroup> capturedGroup = new AtomicReference<>();
        grouper.addGroupListener(capturedGroup::set);

        config.setMaxGroupSize(1);
        AlertGrouper smallGrouper = new AlertGrouper(config, dispatcher, scheduler);
        smallGrouper.addGroupListener(capturedGroup::set);

        AlertEvent event = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        smallGrouper.addAlert(event).join();

        assertNotNull(capturedGroup.get());
        assertEquals(1, capturedGroup.get().eventCount());
    }

    @Test
    void shouldScheduleFlushAfterGroupWait() throws Exception {
        config.setGroupWait(Duration.ofMillis(100));
        AlertGrouper quickGrouper = new AlertGrouper(config, dispatcher, scheduler);

        AlertEvent event = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        quickGrouper.addAlert(event);

        assertEquals(1, quickGrouper.pendingGroupCount());

        // Wait for the group wait time plus some buffer
        Thread.sleep(200);

        // Group should be flushed automatically
        verify(dispatcher, timeout(1000)).dispatch(any(AlertEvent.class));
    }

    @Test
    void shouldCreateSummaryEventForMultipleAlerts() {
        config.setMaxGroupSize(2);
        AlertGrouper smallGrouper = new AlertGrouper(config, dispatcher, scheduler);

        // Both events have the same severity so they'll be grouped together
        AlertEvent event1 = AlertEvent.builder()
                .alertId("alert-1")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING)
                .message("CPU at 80%")
                .labels(Map.of("service", "api"))
                .build();
        AlertEvent event2 = AlertEvent.builder()
                .alertId("alert-2")
                .alertName("high-cpu")
                .severity(AlertSeverity.WARNING) // Same severity to be in same group
                .message("CPU at 95%")
                .labels(Map.of("service", "api"))
                .build();

        smallGrouper.addAlert(event1);
        smallGrouper.addAlert(event2).join();

        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(dispatcher).dispatch(captor.capture());

        AlertEvent summaryEvent = captor.getValue();
        assertTrue(summaryEvent.message().contains("[2 alerts grouped]"));
        assertEquals(AlertSeverity.WARNING, summaryEvent.severity());
    }

    @Test
    void shouldReturnGroupConfiguration() {
        assertEquals(Duration.ofSeconds(30), grouper.getGroupWait());
        assertEquals(100, grouper.getMaxGroupSize());
        assertEquals(List.of("service", "instance"), grouper.getGroupByLabels());
        assertTrue(grouper.isGroupingEnabled());
    }

    @Test
    void shouldGetPendingGroups() {
        AlertEvent event1 = createEvent("alert-1", "high-cpu", Map.of("service", "api"));
        AlertEvent event2 = createEvent("alert-2", "high-cpu", Map.of("service", "web"));

        grouper.addAlert(event1);
        grouper.addAlert(event2);

        List<AlertGroup> groups = grouper.getPendingGroups();

        assertEquals(2, groups.size());
    }

    private AlertEvent createEvent(String id, String name, Map<String, String> labels) {
        return AlertEvent.builder()
                .alertId(id)
                .alertName(name)
                .severity(AlertSeverity.WARNING)
                .message("Test message")
                .labels(labels)
                .build();
    }
}
