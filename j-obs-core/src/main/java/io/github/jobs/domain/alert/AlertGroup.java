package io.github.jobs.domain.alert;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Represents an immutable group of similar alert events for batch notification.
 *
 * <p>Alert grouping reduces notification noise by combining related alerts into
 * a single notification. Alerts are grouped by their {@link AlertGroupKey}
 * (alert name, severity, and configured labels).</p>
 *
 * <h2>Lifecycle</h2>
 * <pre>
 * PENDING → (collecting events) → SENT or FAILED
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AlertGroup group = AlertGroup.fromEvent(event, List.of("service"));
 * group = group.addEvent(anotherEvent);  // immutable - returns new instance
 *
 * if (group.shouldSend(Duration.ofSeconds(30))) {
 *     sendNotification(group.summaryMessage());
 *     group = group.markSent();
 * }
 * }</pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertGroupKey
 * @see AlertGroupStatus
 */
public record AlertGroup(
        String id,
        AlertGroupKey key,
        List<AlertEvent> events,
        Instant createdAt,
        Instant lastUpdatedAt,
        AlertGroupStatus status
) {
    public AlertGroup {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(events, "events cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastUpdatedAt == null) {
            lastUpdatedAt = createdAt;
        }
        events = List.copyOf(events);
    }

    /**
     * Creates a new group from a single alert event.
     */
    public static AlertGroup fromEvent(AlertEvent event, List<String> groupByLabels) {
        AlertGroupKey key = AlertGroupKey.fromEvent(event, groupByLabels);
        return new AlertGroup(
                null,
                key,
                List.of(event),
                null,
                null,
                AlertGroupStatus.PENDING
        );
    }

    /**
     * Adds an event to this group, returning a new immutable group.
     */
    public AlertGroup addEvent(AlertEvent event) {
        List<AlertEvent> newEvents = new ArrayList<>(events);
        newEvents.add(event);
        return new AlertGroup(
                id,
                key,
                newEvents,
                createdAt,
                Instant.now(),
                status
        );
    }

    /**
     * Marks this group as sent.
     */
    public AlertGroup markSent() {
        return new AlertGroup(
                id,
                key,
                events,
                createdAt,
                Instant.now(),
                AlertGroupStatus.SENT
        );
    }

    /**
     * Marks this group as failed.
     */
    public AlertGroup markFailed() {
        return new AlertGroup(
                id,
                key,
                events,
                createdAt,
                Instant.now(),
                AlertGroupStatus.FAILED
        );
    }

    /**
     * Returns the number of events in this group.
     */
    public int eventCount() {
        return events.size();
    }

    /**
     * Returns the severity of the group (highest severity among events).
     */
    public AlertSeverity severity() {
        return events.stream()
                .map(AlertEvent::severity)
                .max(AlertSeverity::compareTo)
                .orElse(AlertSeverity.INFO);
    }

    /**
     * Returns the alert name for this group.
     */
    public String alertName() {
        return key.alertName();
    }

    /**
     * Returns the duration since the first event in the group.
     */
    public Duration duration() {
        if (events.isEmpty()) {
            return Duration.ZERO;
        }
        Instant firstFired = events.stream()
                .map(AlertEvent::firedAt)
                .min(Instant::compareTo)
                .orElse(createdAt);
        return Duration.between(firstFired, lastUpdatedAt);
    }

    /**
     * Checks if the group should be sent based on the wait duration.
     */
    public boolean shouldSend(Duration groupWait) {
        return Duration.between(createdAt, Instant.now()).compareTo(groupWait) >= 0;
    }

    /**
     * Generates a summary message for this group.
     */
    public String summaryMessage() {
        if (events.size() == 1) {
            return events.get(0).message();
        }
        return String.format("[%d alerts] %s - %s",
                events.size(),
                alertName(),
                events.get(0).message());
    }

    /**
     * Returns labels that are common across all events in the group.
     */
    public Map<String, String> commonLabels() {
        if (events.isEmpty()) {
            return Map.of();
        }
        Map<String, String> common = new TreeMap<>(events.get(0).labels());
        for (int i = 1; i < events.size(); i++) {
            Map<String, String> eventLabels = events.get(i).labels();
            common.entrySet().removeIf(entry ->
                    !entry.getValue().equals(eventLabels.get(entry.getKey())));
        }
        return Collections.unmodifiableMap(common);
    }

    /**
     * Checks if this group can accept a new event.
     */
    public boolean canAccept(AlertEvent event, List<String> groupByLabels, int maxGroupSize) {
        if (status != AlertGroupStatus.PENDING) {
            return false;
        }
        if (events.size() >= maxGroupSize) {
            return false;
        }
        AlertGroupKey eventKey = AlertGroupKey.fromEvent(event, groupByLabels);
        return key.equals(eventKey);
    }
}
