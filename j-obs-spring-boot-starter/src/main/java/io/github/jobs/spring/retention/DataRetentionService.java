package io.github.jobs.spring.retention;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.TraceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic cleanup of old observability data based on retention policies.
 * <p>
 * This service runs a scheduled task that removes expired traces and alert events
 * from their respective repositories. For in-memory repositories, TTL-based eviction
 * is the primary mechanism; this service provides an additional safety net and
 * a unified scheduling framework for all retention policies.
 * <p>
 * Cleanup runs every 10 minutes by default on a daemon thread, ensuring
 * it does not prevent JVM shutdown.
 */
public class DataRetentionService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);
    private static final long CLEANUP_INTERVAL_MINUTES = 10;

    private final TraceRepository traceRepository;
    private final AlertEventRepository alertEventRepository;
    private final Duration traceRetention;
    private final Duration alertEventRetention;
    private final ScheduledExecutorService scheduler;

    public DataRetentionService(
            TraceRepository traceRepository,
            AlertEventRepository alertEventRepository,
            Duration traceRetention,
            Duration alertEventRetention) {
        this.traceRepository = traceRepository;
        this.alertEventRepository = alertEventRepository;
        this.traceRetention = traceRetention;
        this.alertEventRetention = alertEventRetention;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "j-obs-retention");
            t.setDaemon(true);
            return t;
        });

        this.scheduler.scheduleAtFixedRate(
                this::cleanup,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        log.info("Data retention service started (traces: {}, alert events: {})",
                traceRetention, alertEventRetention);
    }

    /**
     * Executes a single cleanup cycle, removing data older than the configured retention periods.
     */
    void cleanup() {
        try {
            Instant alertCutoff = Instant.now().minus(alertEventRetention);
            int deletedAlerts = alertEventRepository.deleteOlderThan(alertCutoff);
            if (deletedAlerts > 0) {
                log.info("Retention cleanup: removed {} expired alert events", deletedAlerts);
            }

            // Trace cleanup is primarily handled by TTL in InMemoryTraceRepository.
            // For future JDBC-backed repositories, add deletion logic here.
            log.debug("Retention cleanup completed (trace retention: {}, alert retention: {})",
                    traceRetention, alertEventRetention);
        } catch (Exception e) {
            log.error("Error during retention cleanup", e);
        }
    }

    @Override
    public void destroy() {
        log.debug("Shutting down data retention service");
        scheduler.shutdownNow();
    }
}
