package io.github.jobs.spring.alert;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.AlertRepository;
import io.github.jobs.application.MetricRepository;
import io.github.jobs.application.LogRepository;
import io.github.jobs.application.HealthRepository;
import io.github.jobs.domain.alert.*;
import io.github.jobs.domain.health.HealthCheckResult;
import io.github.jobs.domain.health.HealthStatus;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.domain.log.LogQuery;
import io.github.jobs.domain.metric.Metric;
import io.github.jobs.domain.metric.MetricQuery;
import io.github.jobs.domain.metric.MetricSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Alert evaluation engine that evaluates alert conditions.
 */
public class AlertEngine {

    private static final Logger log = LoggerFactory.getLogger(AlertEngine.class);

    private final AlertRepository alertRepository;
    private final AlertEventRepository alertEventRepository;
    private final MetricRepository metricRepository;
    private final LogRepository logRepository;
    private final HealthRepository healthRepository;

    public AlertEngine(
            AlertRepository alertRepository,
            AlertEventRepository alertEventRepository,
            MetricRepository metricRepository,
            LogRepository logRepository,
            HealthRepository healthRepository
    ) {
        this.alertRepository = alertRepository;
        this.alertEventRepository = alertEventRepository;
        this.metricRepository = metricRepository;
        this.logRepository = logRepository;
        this.healthRepository = healthRepository;
    }

    /**
     * Evaluates a single alert and returns the result.
     */
    public AlertEvaluationResult evaluate(Alert alert) {
        try {
            return switch (alert.type()) {
                case METRIC -> evaluateMetricAlert(alert);
                case LOG -> evaluateLogAlert(alert);
                case HEALTH -> evaluateHealthAlert(alert);
                case TRACE -> evaluateTraceAlert(alert);
            };
        } catch (Exception e) {
            log.error("Error evaluating alert {}: {}", alert.id(), e.getMessage(), e);
            return AlertEvaluationResult.notTriggered(alert.id());
        }
    }

    /**
     * Evaluates all enabled alerts.
     */
    public List<AlertEvaluationResult> evaluateAll() {
        List<Alert> enabledAlerts = alertRepository.findEnabled();
        log.debug("Evaluating {} enabled alerts", enabledAlerts.size());

        List<AlertEvaluationResult> results = new ArrayList<>();
        for (Alert alert : enabledAlerts) {
            AlertEvaluationResult result = evaluate(alert);
            results.add(result);

            if (result.triggered()) {
                log.info("Alert {} triggered: {}", alert.name(), result.message());
            }
        }
        return results;
    }

    /**
     * Creates an alert event from evaluation result.
     */
    public AlertEvent createEvent(Alert alert, AlertEvaluationResult result) {
        return AlertEvent.builder()
                .alertId(alert.id())
                .alertName(alert.name())
                .severity(alert.severity())
                .status(AlertEventStatus.FIRING)
                .message(result.message())
                .labels(result.labels())
                .build();
    }

    /**
     * Checks if alert is already firing (to avoid duplicates).
     */
    public boolean isAlreadyFiring(String alertId) {
        return alertEventRepository.findLatestByAlertId(alertId)
                .map(event -> event.status() == AlertEventStatus.FIRING)
                .orElse(false);
    }

    /**
     * Auto-resolves firing alerts that are no longer triggered.
     */
    public List<AlertEvent> autoResolve(List<AlertEvaluationResult> results) {
        Set<String> triggeredAlertIds = new HashSet<>();
        for (AlertEvaluationResult result : results) {
            if (result.triggered()) {
                triggeredAlertIds.add(result.alertId());
            }
        }

        List<AlertEvent> resolved = new ArrayList<>();
        for (AlertEvent event : alertEventRepository.findByStatus(AlertEventStatus.FIRING)) {
            if (!triggeredAlertIds.contains(event.alertId())) {
                AlertEvent resolvedEvent = event.resolve("system");
                alertEventRepository.save(resolvedEvent);
                resolved.add(resolvedEvent);
                log.info("Auto-resolved alert: {}", event.alertName());
            }
        }
        return resolved;
    }

    private AlertEvaluationResult evaluateMetricAlert(Alert alert) {
        AlertCondition condition = alert.condition();
        String metricName = condition.metric();

        // Query metrics by name pattern
        List<Metric> metrics = metricRepository.query(MetricQuery.byName(metricName));
        if (metrics.isEmpty()) {
            return AlertEvaluationResult.notTriggered(alert.id());
        }

        // Get the first matching metric and its snapshot
        Metric metric = metrics.get(0);
        MetricSnapshot snapshot = metricRepository.getSnapshot(metric.id(), Duration.ofMinutes(5));
        double value = snapshot.currentValue();
        boolean triggered = condition.evaluate(value);

        if (triggered) {
            String message = String.format("%s: %s is %.2f (threshold: %s %.2f)",
                    alert.name(),
                    metricName,
                    value,
                    condition.operator().symbol(),
                    condition.threshold()
            );
            return AlertEvaluationResult.triggered(
                    alert.id(),
                    message,
                    value,
                    condition.threshold(),
                    Map.of("metric", metricName)
            );
        }

        return AlertEvaluationResult.notTriggered(alert.id(), value, condition.threshold());
    }

    private AlertEvaluationResult evaluateLogAlert(Alert alert) {
        AlertCondition condition = alert.condition();
        Duration window = condition.window();
        Instant since = Instant.now().minus(window);

        // Build log query with time filter
        LogQuery.Builder queryBuilder = LogQuery.builder()
                .startTime(since)
                .limit(10000); // Large limit to get all logs in window

        // Apply level filter if present
        String levelFilter = condition.filters().get("level");
        if (levelFilter != null) {
            try {
                queryBuilder.minLevel(LogLevel.valueOf(levelFilter.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid level, ignore filter
            }
        }

        // Query logs within the window
        List<LogEntry> logs = logRepository.query(queryBuilder.build()).stream()
                .filter(entry -> matchesLogFilters(entry, condition.filters()))
                .toList();

        long errorCount = logs.stream()
                .filter(entry -> entry.level() == LogLevel.ERROR)
                .count();

        boolean triggered = condition.evaluate(errorCount);

        if (triggered) {
            String message = String.format("%s: %d error(s) in last %s (threshold: %s %.0f)",
                    alert.name(),
                    errorCount,
                    formatDuration(window),
                    condition.operator().symbol(),
                    condition.threshold()
            );
            return AlertEvaluationResult.triggered(
                    alert.id(),
                    message,
                    errorCount,
                    condition.threshold(),
                    Map.of("window", window.toString())
            );
        }

        return AlertEvaluationResult.notTriggered(alert.id(), errorCount, condition.threshold());
    }

    private AlertEvaluationResult evaluateHealthAlert(Alert alert) {
        AlertCondition condition = alert.condition();
        Map<String, String> filters = condition.filters();
        String componentFilter = filters.getOrDefault("component", null);

        HealthCheckResult healthResult = healthRepository.getHealth();

        // Check overall health or specific component
        if (componentFilter != null) {
            return healthResult.components().stream()
                    .filter(c -> c.name().equalsIgnoreCase(componentFilter))
                    .findFirst()
                    .map(component -> {
                        boolean unhealthy = component.status() == HealthStatus.DOWN ||
                                component.status() == HealthStatus.OUT_OF_SERVICE;
                        if (unhealthy) {
                            String message = String.format("%s: Component '%s' is %s",
                                    alert.name(),
                                    component.name(),
                                    component.status().displayName()
                            );
                            return AlertEvaluationResult.triggered(
                                    alert.id(),
                                    message,
                                    component.status().name(),
                                    "UP",
                                    Map.of("component", component.name())
                            );
                        }
                        return AlertEvaluationResult.notTriggered(alert.id());
                    })
                    .orElse(AlertEvaluationResult.notTriggered(alert.id()));
        }

        // Check overall health
        boolean unhealthy = healthResult.status() == HealthStatus.DOWN ||
                healthResult.status() == HealthStatus.OUT_OF_SERVICE;

        if (unhealthy) {
            String message = String.format("%s: Overall health is %s",
                    alert.name(),
                    healthResult.status().displayName()
            );
            return AlertEvaluationResult.triggered(
                    alert.id(),
                    message,
                    healthResult.status().name(),
                    "UP",
                    Map.of()
            );
        }

        return AlertEvaluationResult.notTriggered(alert.id());
    }

    private AlertEvaluationResult evaluateTraceAlert(Alert alert) {
        // Trace alerts would require TraceRepository access
        // For now, we just return not triggered
        // This can be extended when trace metrics are available
        return AlertEvaluationResult.notTriggered(alert.id());
    }

    private boolean matchesLogFilters(LogEntry entry, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String key = filter.getKey();
            String value = filter.getValue();

            switch (key.toLowerCase()) {
                case "level" -> {
                    if (!entry.level().name().equalsIgnoreCase(value)) {
                        return false;
                    }
                }
                case "logger" -> {
                    if (!entry.loggerName().contains(value)) {
                        return false;
                    }
                }
                case "message" -> {
                    if (!entry.message().contains(value)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String formatDuration(Duration duration) {
        if (duration.toHours() > 0) {
            return duration.toHours() + "h";
        }
        if (duration.toMinutes() > 0) {
            return duration.toMinutes() + "m";
        }
        return duration.toSeconds() + "s";
    }
}
