package io.github.jobs.spring.alert;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.domain.alert.Alert;
import io.github.jobs.domain.alert.AlertEvaluationResult;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler for periodic alert evaluation.
 */
public class AlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

    private final AlertEngine alertEngine;
    private final AlertDispatcher alertDispatcher;
    private final AlertGrouper alertGrouper;
    private final AlertEventRepository alertEventRepository;
    private final TaskScheduler taskScheduler;
    private final JObsProperties.Alerts config;

    private ScheduledFuture<?> evaluationTask;
    private ScheduledFuture<?> cleanupTask;
    private volatile boolean running = false;

    public AlertScheduler(
            AlertEngine alertEngine,
            AlertDispatcher alertDispatcher,
            AlertGrouper alertGrouper,
            AlertEventRepository alertEventRepository,
            TaskScheduler taskScheduler,
            JObsProperties.Alerts config
    ) {
        this.alertEngine = alertEngine;
        this.alertDispatcher = alertDispatcher;
        this.alertGrouper = alertGrouper;
        this.alertEventRepository = alertEventRepository;
        this.taskScheduler = taskScheduler;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        if (!config.isEnabled()) {
            log.info("Alert scheduler is disabled");
            return;
        }

        Duration interval = config.getEvaluationInterval();
        log.info("Starting alert scheduler with evaluation interval: {}", interval);

        evaluationTask = taskScheduler.scheduleAtFixedRate(
                this::runEvaluationCycle,
                interval
        );

        // Schedule cleanup task every hour
        cleanupTask = taskScheduler.scheduleAtFixedRate(
                this::runCleanup,
                Duration.ofHours(1)
        );

        running = true;
    }

    @PreDestroy
    public void stop() {
        running = false;

        if (evaluationTask != null) {
            evaluationTask.cancel(false);
        }
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }

        // Flush any pending groups before shutdown
        if (alertGrouper.isGroupingEnabled() && alertGrouper.pendingGroupCount() > 0) {
            log.info("Flushing {} pending alert groups before shutdown", alertGrouper.pendingGroupCount());
            alertGrouper.flushAll().join();
        }

        log.info("Alert scheduler stopped");
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Manually triggers an evaluation cycle.
     */
    public void triggerEvaluation() {
        runEvaluationCycle();
    }

    private void runEvaluationCycle() {
        try {
            log.debug("Starting alert evaluation cycle");

            // Evaluate all alerts
            List<AlertEvaluationResult> results = alertEngine.evaluateAll();

            int firingCount = 0;
            for (AlertEvaluationResult result : results) {
                if (result.triggered()) {
                    processTriggeredAlert(result);
                    firingCount++;
                }
            }

            // Auto-resolve alerts that are no longer triggered
            List<AlertEvent> resolved = alertEngine.autoResolve(results);

            log.debug("Evaluation cycle complete: {} firing, {} resolved",
                    firingCount, resolved.size());

        } catch (Exception e) {
            log.error("Error during alert evaluation cycle: {}", e.getMessage(), e);
        }
    }

    private void processTriggeredAlert(AlertEvaluationResult result) {
        // Check if already firing to avoid duplicate notifications
        if (alertEngine.isAlreadyFiring(result.alertId())) {
            log.debug("Alert {} already firing, skipping notification", result.alertId());
            return;
        }

        // Find the alert configuration
        // Note: We need access to AlertRepository here, but it's in AlertEngine
        // For simplicity, we create a minimal event
        AlertEvent event = AlertEvent.builder()
                .alertId(result.alertId())
                .alertName(result.alertId()) // Will be overwritten if we have access to alert name
                .message(result.message())
                .labels(result.labels())
                .build();

        // Save the event
        alertEventRepository.save(event);

        // Use grouper if grouping is enabled, otherwise dispatch directly
        if (alertGrouper.isGroupingEnabled()) {
            alertGrouper.addAlert(event)
                    .whenComplete((results, error) -> {
                        if (error != null) {
                            log.error("Failed to add alert to group: {}", error.getMessage());
                        } else if (!results.isEmpty()) {
                            // Group was flushed immediately (e.g., max size reached)
                            logDispatchResults(event, results);
                        } else {
                            log.debug("Alert {} added to group for later dispatch", event.alertId());
                        }
                    });
        } else {
            // Dispatch directly without grouping
            alertDispatcher.dispatch(event)
                    .whenComplete((results, error) -> {
                        if (error != null) {
                            log.error("Failed to dispatch alert notifications: {}", error.getMessage());
                        } else {
                            logDispatchResults(event, results);
                        }
                    });
        }
    }

    private void logDispatchResults(AlertEvent event, List<io.github.jobs.domain.alert.AlertNotificationResult> results) {
        long successCount = results.stream()
                .filter(r -> r.success())
                .count();
        log.info("Alert {} dispatched to {}/{} providers",
                event.alertId(), successCount, results.size());
    }

    private void runCleanup() {
        try {
            Duration retention = config.getRetention();
            Instant cutoff = Instant.now().minus(retention);
            int deleted = alertEventRepository.deleteOlderThan(cutoff);
            if (deleted > 0) {
                log.info("Cleaned up {} old alert events (older than {})", deleted, retention);
            }
        } catch (Exception e) {
            log.error("Error during alert cleanup: {}", e.getMessage(), e);
        }
    }
}
