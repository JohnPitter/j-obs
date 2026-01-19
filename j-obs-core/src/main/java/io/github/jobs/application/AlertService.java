package io.github.jobs.application;

import io.github.jobs.domain.alert.Alert;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertEventStatus;
import io.github.jobs.domain.alert.AlertEvaluationResult;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing alerts and alert events.
 */
public interface AlertService {

    // ==================== Alert Configuration ====================

    /**
     * Creates or updates an alert configuration.
     */
    Alert saveAlert(Alert alert);

    /**
     * Finds an alert by ID.
     */
    Optional<Alert> findAlertById(String id);

    /**
     * Returns all configured alerts.
     */
    List<Alert> findAllAlerts();

    /**
     * Returns all enabled alerts.
     */
    List<Alert> findEnabledAlerts();

    /**
     * Deletes an alert configuration.
     */
    boolean deleteAlert(String id);

    /**
     * Enables or disables an alert.
     */
    Alert setAlertEnabled(String id, boolean enabled);

    // ==================== Alert Events ====================

    /**
     * Returns all alert events.
     */
    List<AlertEvent> findAllEvents();

    /**
     * Returns alert events by status.
     */
    List<AlertEvent> findEventsByStatus(AlertEventStatus status);

    /**
     * Returns active (firing or acknowledged) alert events.
     */
    List<AlertEvent> findActiveEvents();

    /**
     * Finds an alert event by ID.
     */
    Optional<AlertEvent> findEventById(String id);

    /**
     * Returns events for a specific alert within a time range.
     */
    List<AlertEvent> findEventsInRange(Instant start, Instant end);

    /**
     * Acknowledges an alert event.
     */
    AlertEvent acknowledgeEvent(String eventId, String acknowledgedBy);

    /**
     * Resolves an alert event.
     */
    AlertEvent resolveEvent(String eventId, String resolvedBy);

    // ==================== Alert Evaluation ====================

    /**
     * Evaluates a single alert condition.
     */
    AlertEvaluationResult evaluate(Alert alert);

    /**
     * Evaluates all enabled alerts.
     */
    List<AlertEvaluationResult> evaluateAll();

    /**
     * Fires an alert event based on evaluation result.
     */
    AlertEvent fireAlert(Alert alert, AlertEvaluationResult result);

    // ==================== Notifications ====================

    /**
     * Sends notifications for an alert event.
     */
    CompletableFuture<List<AlertNotificationResult>> sendNotifications(AlertEvent event);

    /**
     * Sends notification to a specific provider.
     */
    CompletableFuture<AlertNotificationResult> sendNotification(AlertEvent event, String providerName);

    /**
     * Tests a provider by sending a test notification.
     */
    CompletableFuture<AlertNotificationResult> testProvider(String providerName);

    // ==================== Providers ====================

    /**
     * Returns all registered alert providers.
     */
    List<AlertProvider> getProviders();

    /**
     * Returns configured (enabled) alert providers.
     */
    List<AlertProvider> getConfiguredProviders();

    /**
     * Finds a provider by name.
     */
    Optional<AlertProvider> findProviderByName(String name);

    // ==================== Statistics ====================

    /**
     * Returns statistics about alerts and events.
     */
    AlertStatistics getStatistics();

    /**
     * Statistics about alerts and events.
     */
    record AlertStatistics(
            long totalAlerts,
            long enabledAlerts,
            long totalEvents,
            long activeEvents,
            long firingEvents,
            long acknowledgedEvents,
            Map<String, Long> eventsByAlertId
    ) {}
}
