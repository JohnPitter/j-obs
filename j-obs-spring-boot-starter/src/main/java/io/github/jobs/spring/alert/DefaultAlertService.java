package io.github.jobs.spring.alert;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.AlertRepository;
import io.github.jobs.application.AlertService;
import io.github.jobs.domain.alert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Default implementation of AlertService.
 */
public class DefaultAlertService implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAlertService.class);

    private final AlertRepository alertRepository;
    private final AlertEventRepository alertEventRepository;
    private final AlertEngine alertEngine;
    private final AlertDispatcher alertDispatcher;

    public DefaultAlertService(
            AlertRepository alertRepository,
            AlertEventRepository alertEventRepository,
            AlertEngine alertEngine,
            AlertDispatcher alertDispatcher
    ) {
        this.alertRepository = alertRepository;
        this.alertEventRepository = alertEventRepository;
        this.alertEngine = alertEngine;
        this.alertDispatcher = alertDispatcher;
    }

    // ==================== Alert Configuration ====================

    @Override
    public Alert saveAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    @Override
    public Optional<Alert> findAlertById(String id) {
        return alertRepository.findById(id);
    }

    @Override
    public List<Alert> findAllAlerts() {
        return alertRepository.findAll();
    }

    @Override
    public List<Alert> findEnabledAlerts() {
        return alertRepository.findEnabled();
    }

    @Override
    public boolean deleteAlert(String id) {
        return alertRepository.delete(id);
    }

    @Override
    public Alert setAlertEnabled(String id, boolean enabled) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
        Alert updated = alert.withEnabled(enabled);
        return alertRepository.save(updated);
    }

    // ==================== Alert Events ====================

    @Override
    public List<AlertEvent> findAllEvents() {
        return alertEventRepository.findAll();
    }

    @Override
    public List<AlertEvent> findEventsByStatus(AlertEventStatus status) {
        return alertEventRepository.findByStatus(status);
    }

    @Override
    public List<AlertEvent> findActiveEvents() {
        return alertEventRepository.findActive();
    }

    @Override
    public Optional<AlertEvent> findEventById(String id) {
        return alertEventRepository.findById(id);
    }

    @Override
    public List<AlertEvent> findEventsInRange(Instant start, Instant end) {
        return alertEventRepository.findInTimeRange(start, end);
    }

    @Override
    public AlertEvent acknowledgeEvent(String eventId, String acknowledgedBy) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (event.status() == AlertEventStatus.RESOLVED) {
            throw new IllegalStateException("Cannot acknowledge a resolved event");
        }

        AlertEvent acknowledged = event.acknowledge(acknowledgedBy);
        return alertEventRepository.save(acknowledged);
    }

    @Override
    public AlertEvent resolveEvent(String eventId, String resolvedBy) {
        AlertEvent event = alertEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (event.status() == AlertEventStatus.RESOLVED) {
            throw new IllegalStateException("Event is already resolved");
        }

        AlertEvent resolved = event.resolve(resolvedBy);
        return alertEventRepository.save(resolved);
    }

    // ==================== Alert Evaluation ====================

    @Override
    public AlertEvaluationResult evaluate(Alert alert) {
        return alertEngine.evaluate(alert);
    }

    @Override
    public List<AlertEvaluationResult> evaluateAll() {
        return alertEngine.evaluateAll();
    }

    @Override
    public AlertEvent fireAlert(Alert alert, AlertEvaluationResult result) {
        AlertEvent event = alertEngine.createEvent(alert, result);
        AlertEvent saved = alertEventRepository.save(event);

        // Dispatch notifications asynchronously
        sendNotifications(saved);

        return saved;
    }

    // ==================== Notifications ====================

    @Override
    public CompletableFuture<List<AlertNotificationResult>> sendNotifications(AlertEvent event) {
        return alertDispatcher.dispatch(event);
    }

    @Override
    public CompletableFuture<AlertNotificationResult> sendNotification(AlertEvent event, String providerName) {
        return alertDispatcher.dispatchTo(event, providerName);
    }

    @Override
    public CompletableFuture<AlertNotificationResult> testProvider(String providerName) {
        return alertDispatcher.testProvider(providerName);
    }

    // ==================== Providers ====================

    @Override
    public List<AlertProvider> getProviders() {
        return alertDispatcher.getAllProviders();
    }

    @Override
    public List<AlertProvider> getConfiguredProviders() {
        return alertDispatcher.getConfiguredProviders();
    }

    @Override
    public Optional<AlertProvider> findProviderByName(String name) {
        return alertDispatcher.findProvider(name);
    }

    // ==================== Statistics ====================

    @Override
    public AlertStatistics getStatistics() {
        long totalAlerts = alertRepository.count();
        long enabledAlerts = alertRepository.findEnabled().size();
        long totalEvents = alertEventRepository.count();
        long activeEvents = alertEventRepository.countActive();

        List<AlertEvent> activeList = alertEventRepository.findActive();
        long firingEvents = activeList.stream()
                .filter(e -> e.status() == AlertEventStatus.FIRING)
                .count();
        long acknowledgedEvents = activeList.stream()
                .filter(e -> e.status() == AlertEventStatus.ACKNOWLEDGED)
                .count();

        Map<String, Long> eventsByAlertId = alertEventRepository.findAll().stream()
                .collect(Collectors.groupingBy(AlertEvent::alertId, Collectors.counting()));

        return new AlertStatistics(
                totalAlerts,
                enabledAlerts,
                totalEvents,
                activeEvents,
                firingEvents,
                acknowledgedEvents,
                eventsByAlertId
        );
    }
}
