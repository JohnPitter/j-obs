package io.github.jobs.spring.alert;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertGroup;
import io.github.jobs.domain.alert.AlertGroupKey;
import io.github.jobs.domain.alert.AlertGroupStatus;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Service that groups similar alerts together before dispatching notifications.
 *
 * <p>Alert grouping reduces notification noise by combining related alerts into
 * a single notification. Groups are sent when one of the following conditions is met:</p>
 * <ul>
 *   <li>The configured wait time ({@code group-wait}) has elapsed</li>
 *   <li>The group reaches maximum size ({@code max-group-size})</li>
 *   <li>An explicit flush is requested</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     throttling:
 *       grouping: true
 *       group-wait: 30s
 *       max-group-size: 100
 *       group-by-labels:
 *         - service
 *         - instance
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe and uses a {@link ReentrantReadWriteLock}
 * to coordinate concurrent access to pending groups.</p>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertGroup
 * @see AlertGroupKey
 * @see AlertDispatcher
 */
public class AlertGrouper implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AlertGrouper.class);

    private final JObsProperties.Alerts.Throttling config;
    private final AlertDispatcher dispatcher;
    private final ScheduledExecutorService scheduler;
    private final Map<AlertGroupKey, AlertGroup> pendingGroups = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> groupTimers = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<Consumer<AlertGroup>> groupListeners = new ArrayList<>();

    public AlertGrouper(
            JObsProperties.Alerts.Throttling config,
            AlertDispatcher dispatcher,
            ScheduledExecutorService scheduler) {
        this.config = config;
        this.dispatcher = dispatcher;
        this.scheduler = scheduler;
    }

    /**
     * Adds an alert event to be grouped and dispatched.
     * If grouping is disabled, the alert is dispatched immediately.
     */
    public CompletableFuture<List<AlertNotificationResult>> addAlert(AlertEvent event) {
        if (!config.isGrouping()) {
            // Grouping disabled, dispatch immediately
            return dispatcher.dispatch(event);
        }

        AlertGroupKey key = AlertGroupKey.fromEvent(event, config.getGroupByLabels());

        lock.writeLock().lock();
        try {
            AlertGroup existingGroup = pendingGroups.get(key);

            if (existingGroup != null) {
                // Add to existing group
                AlertGroup updatedGroup = existingGroup.addEvent(event);
                pendingGroups.put(key, updatedGroup);
                log.debug("Added alert to existing group {}: {} events",
                        key.toDisplayString(), updatedGroup.eventCount());

                // Check if group is full
                if (updatedGroup.eventCount() >= config.getMaxGroupSize()) {
                    return flushGroupInternal(key);
                }

                return CompletableFuture.completedFuture(List.of());
            } else {
                // Create new group
                AlertGroup newGroup = AlertGroup.fromEvent(event, config.getGroupByLabels());
                pendingGroups.put(key, newGroup);
                log.debug("Created new group {}", key.toDisplayString());

                // Check if group is already full (max size 1)
                if (newGroup.eventCount() >= config.getMaxGroupSize()) {
                    return flushGroupInternal(key);
                }

                // Schedule flush after group wait time
                scheduleGroupFlush(key, newGroup.id());

                return CompletableFuture.completedFuture(List.of());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Flushes a specific group, sending all its alerts.
     */
    public CompletableFuture<List<AlertNotificationResult>> flushGroup(AlertGroupKey key) {
        lock.writeLock().lock();
        try {
            return flushGroupInternal(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal flush method that doesn't acquire the lock.
     * Must be called while holding the write lock.
     */
    private CompletableFuture<List<AlertNotificationResult>> flushGroupInternal(AlertGroupKey key) {
        AlertGroup group = pendingGroups.remove(key);
        if (group == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Cancel scheduled timer
        cancelTimer(group.id());

        return sendGroupedAlert(group);
    }

    /**
     * Flushes all pending groups immediately.
     */
    public CompletableFuture<List<AlertNotificationResult>> flushAll() {
        List<AlertGroup> groupsToSend = new ArrayList<>();

        lock.writeLock().lock();
        try {
            groupsToSend.addAll(pendingGroups.values());
            pendingGroups.clear();
            groupTimers.values().forEach(f -> f.cancel(false));
            groupTimers.clear();
        } finally {
            lock.writeLock().unlock();
        }

        if (groupsToSend.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<List<AlertNotificationResult>>> futures = groupsToSend.stream()
                .map(this::sendGroupedAlert)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .toList());
    }

    /**
     * Returns all pending groups.
     */
    public List<AlertGroup> getPendingGroups() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(pendingGroups.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the number of pending groups.
     */
    public int pendingGroupCount() {
        lock.readLock().lock();
        try {
            return pendingGroups.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the total number of pending alerts across all groups.
     */
    public int pendingAlertCount() {
        lock.readLock().lock();
        try {
            return pendingGroups.values().stream()
                    .mapToInt(AlertGroup::eventCount)
                    .sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Finds a pending group by its key.
     */
    public Optional<AlertGroup> findGroup(AlertGroupKey key) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(pendingGroups.get(key));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Registers a listener to be notified when groups are sent.
     */
    public void addGroupListener(Consumer<AlertGroup> listener) {
        groupListeners.add(listener);
    }

    /**
     * Returns the group wait duration.
     */
    public Duration getGroupWait() {
        return config.getGroupWait();
    }

    /**
     * Returns the maximum group size.
     */
    public int getMaxGroupSize() {
        return config.getMaxGroupSize();
    }

    /**
     * Returns the labels used for grouping.
     */
    public List<String> getGroupByLabels() {
        return config.getGroupByLabels();
    }

    /**
     * Returns whether grouping is enabled.
     */
    public boolean isGroupingEnabled() {
        return config.isGrouping();
    }

    private void scheduleGroupFlush(AlertGroupKey key, String groupId) {
        Duration groupWait = config.getGroupWait();
        ScheduledFuture<?> future = scheduler.schedule(
                () -> flushGroupById(groupId),
                groupWait.toMillis(),
                TimeUnit.MILLISECONDS
        );
        groupTimers.put(groupId, future);
        log.debug("Scheduled group flush in {} for group {}", groupWait, key.toDisplayString());
    }

    private void flushGroupById(String groupId) {
        lock.writeLock().lock();
        try {
            // Find and remove the group by ID
            AlertGroupKey keyToRemove = null;
            AlertGroup groupToSend = null;

            for (Map.Entry<AlertGroupKey, AlertGroup> entry : pendingGroups.entrySet()) {
                if (entry.getValue().id().equals(groupId)) {
                    keyToRemove = entry.getKey();
                    groupToSend = entry.getValue();
                    break;
                }
            }

            if (keyToRemove != null && groupToSend != null) {
                pendingGroups.remove(keyToRemove);
                groupTimers.remove(groupId);

                final AlertGroup finalGroup = groupToSend;
                // Send asynchronously to avoid blocking scheduler
                CompletableFuture.runAsync(() -> sendGroupedAlert(finalGroup));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cancelTimer(String groupId) {
        ScheduledFuture<?> timer = groupTimers.remove(groupId);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private CompletableFuture<List<AlertNotificationResult>> sendGroupedAlert(AlertGroup group) {
        log.info("Sending grouped alert: {} with {} events",
                group.key().toDisplayString(), group.eventCount());

        // Create a summary event for the group
        AlertEvent summaryEvent = createGroupSummaryEvent(group);

        // Mark group as sent
        AlertGroup sentGroup = group.markSent();

        // Notify listeners
        groupListeners.forEach(listener -> {
            try {
                listener.accept(sentGroup);
            } catch (Exception e) {
                log.warn("Group listener error: {}", e.getMessage());
            }
        });

        return dispatcher.dispatch(summaryEvent);
    }

    private AlertEvent createGroupSummaryEvent(AlertGroup group) {
        if (group.eventCount() == 1) {
            // Single event, return as-is
            return group.events().get(0);
        }

        // Create summary event
        AlertEvent firstEvent = group.events().get(0);
        String summaryMessage = String.format(
                "[%d alerts grouped] %s\n\nFirst: %s\nLast: %s\nDuration: %s",
                group.eventCount(),
                firstEvent.message(),
                formatTime(group.events().get(0).firedAt()),
                formatTime(group.events().get(group.eventCount() - 1).firedAt()),
                formatDuration(group.duration())
        );

        return AlertEvent.builder()
                .alertId(firstEvent.alertId() + "-group")
                .alertName(firstEvent.alertName())
                .severity(group.severity())
                .message(summaryMessage)
                .labels(group.commonLabels())
                .firedAt(group.createdAt())
                .build();
    }

    private String formatTime(Instant instant) {
        return instant.toString().replace("T", " ").substring(0, 19);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }

    /**
     * Shuts down the grouper, cancelling all pending timers and clearing groups.
     * Note: This does not shut down the scheduler as it's owned externally.
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            log.debug("Shutting down alert grouper with {} pending groups", pendingGroups.size());
            // Cancel all pending timers
            groupTimers.values().forEach(future -> future.cancel(false));
            groupTimers.clear();
            pendingGroups.clear();
            groupListeners.clear();
            log.debug("Alert grouper shutdown complete");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Called by Spring when the bean is destroyed.
     */
    @Override
    public void destroy() {
        shutdown();
    }
}
