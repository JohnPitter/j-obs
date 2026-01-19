package io.github.jobs.spring.actuator;

import io.github.jobs.application.LogRepository;
import io.github.jobs.application.MetricRepository;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.application.AlertEventRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Spring Boot Actuator HealthIndicator for J-Obs.
 * Reports the health status of J-Obs repositories and internal state.
 */
public class JObsHealthIndicator implements HealthIndicator {

    private final TraceRepository traceRepository;
    private final LogRepository logRepository;
    private final MetricRepository metricRepository;
    private final AlertEventRepository alertEventRepository;

    private final int maxTraces;
    private final int maxLogs;
    private final int maxAlertEvents;

    public JObsHealthIndicator(
            TraceRepository traceRepository,
            LogRepository logRepository,
            MetricRepository metricRepository,
            AlertEventRepository alertEventRepository,
            int maxTraces,
            int maxLogs,
            int maxAlertEvents) {
        this.traceRepository = traceRepository;
        this.logRepository = logRepository;
        this.metricRepository = metricRepository;
        this.alertEventRepository = alertEventRepository;
        this.maxTraces = maxTraces;
        this.maxLogs = maxLogs;
        this.maxAlertEvents = maxAlertEvents;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // Check trace repository
            TraceRepository.TraceStats traceStats = traceRepository.stats();
            double traceUsage = maxTraces > 0 ? (double) traceStats.totalTraces() / maxTraces * 100 : 0;
            builder.withDetail("traces", new RepositoryStatus(
                    traceStats.totalTraces(),
                    maxTraces,
                    traceUsage
            ));

            // Check log repository
            LogRepository.LogStats logStats = logRepository.stats();
            double logUsage = maxLogs > 0 ? (double) logStats.totalEntries() / maxLogs * 100 : 0;
            builder.withDetail("logs", new RepositoryStatus(
                    logStats.totalEntries(),
                    maxLogs,
                    logUsage
            ));

            // Check metric repository
            MetricRepository.MetricStats metricStats = metricRepository.stats();
            builder.withDetail("metrics", new MetricsStatus(metricStats.totalMetrics()));

            // Check alert event repository
            long totalAlertEvents = alertEventRepository.count();
            double alertUsage = maxAlertEvents > 0 ? (double) totalAlertEvents / maxAlertEvents * 100 : 0;
            builder.withDetail("alertEvents", new RepositoryStatus(
                    totalAlertEvents,
                    maxAlertEvents,
                    alertUsage
            ));

            // Calculate overall status based on usage
            double maxUsage = Math.max(Math.max(traceUsage, logUsage), alertUsage);
            if (maxUsage >= 95) {
                builder.down()
                        .withDetail("reason", "Repository capacity critical (>95% used)");
            } else if (maxUsage >= 80) {
                builder.status("DEGRADED")
                        .withDetail("reason", "Repository capacity high (>80% used)");
            }

            // Memory estimation (rough calculation)
            long estimatedMemoryMb = estimateMemoryUsage(traceStats.totalTraces(), logStats.totalEntries(), totalAlertEvents);
            builder.withDetail("estimatedMemoryMb", estimatedMemoryMb);

        } catch (Exception e) {
            builder.down()
                    .withDetail("error", "Failed to check J-Obs health: " + e.getMessage());
        }

        return builder.build();
    }

    private long estimateMemoryUsage(long traces, long logs, long alertEvents) {
        // Rough estimates:
        // - Each trace: ~5KB average
        // - Each log: ~500 bytes average
        // - Each alert event: ~1KB average
        long tracesBytes = traces * 5 * 1024;
        long logsBytes = logs * 500;
        long alertEventsBytes = alertEvents * 1024;
        return (tracesBytes + logsBytes + alertEventsBytes) / (1024 * 1024);
    }

    /**
     * Status information for a repository.
     */
    public record RepositoryStatus(
            long current,
            long max,
            double usagePercent
    ) {}

    /**
     * Status information for metrics.
     */
    public record MetricsStatus(
            long totalMetrics
    ) {}
}
