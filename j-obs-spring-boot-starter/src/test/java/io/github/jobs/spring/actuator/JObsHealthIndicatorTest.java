package io.github.jobs.spring.actuator;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.LogRepository;
import io.github.jobs.application.MetricRepository;
import io.github.jobs.application.TraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JObsHealthIndicator.
 */
@ExtendWith(MockitoExtension.class)
class JObsHealthIndicatorTest {

    @Mock
    private TraceRepository traceRepository;
    @Mock
    private LogRepository logRepository;
    @Mock
    private MetricRepository metricRepository;
    @Mock
    private AlertEventRepository alertEventRepository;

    private JObsHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new JObsHealthIndicator(
                traceRepository,
                logRepository,
                metricRepository,
                alertEventRepository,
                1000,  // maxTraces
                10000, // maxLogs
                500    // maxAlertEvents
        );
    }

    private void setupRepositories(int traces, int logs, int metrics, long alertEvents) {
        when(traceRepository.stats()).thenReturn(new TraceRepository.TraceStats(traces, 0, 0, 0.0, 0.0, 0.0, 0.0));
        when(logRepository.stats()).thenReturn(new LogRepository.LogStats(logs, 0, 0, 0, 0, 0, 0, 0));
        when(metricRepository.stats()).thenReturn(new MetricRepository.MetricStats(metrics, 0, 0, 0, 0, 0, 0, 0));
        when(alertEventRepository.count()).thenReturn(alertEvents);
    }

    // ==================== Health Status ====================

    @Test
    void health_shouldReturnUpWhenUsageLow() {
        setupRepositories(100, 1000, 50, 10);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).doesNotContainKey("reason");
    }

    @Test
    void health_shouldReturnDegradedWhenUsageHigh() {
        // 85% traces usage
        setupRepositories(850, 1000, 50, 10);

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails().get("reason")).isEqualTo("Repository capacity high (>80% used)");
    }

    @Test
    void health_shouldReturnDownWhenUsageCritical() {
        // 96% logs usage
        setupRepositories(100, 9600, 50, 10);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("reason")).isEqualTo("Repository capacity critical (>95% used)");
    }

    @Test
    void health_shouldReturnDownWhenAlertEventsUsageCritical() {
        // 97% alert events usage
        setupRepositories(100, 1000, 50, 485);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    // ==================== Repository Details ====================

    @Test
    void health_shouldIncludeTraceDetails() {
        setupRepositories(200, 1000, 50, 10);

        Health health = healthIndicator.health();

        JObsHealthIndicator.RepositoryStatus traceStatus =
                (JObsHealthIndicator.RepositoryStatus) health.getDetails().get("traces");
        assertThat(traceStatus.current()).isEqualTo(200);
        assertThat(traceStatus.max()).isEqualTo(1000);
        assertThat(traceStatus.usagePercent()).isEqualTo(20.0);
    }

    @Test
    void health_shouldIncludeLogDetails() {
        setupRepositories(100, 5000, 50, 10);

        Health health = healthIndicator.health();

        JObsHealthIndicator.RepositoryStatus logStatus =
                (JObsHealthIndicator.RepositoryStatus) health.getDetails().get("logs");
        assertThat(logStatus.current()).isEqualTo(5000);
        assertThat(logStatus.max()).isEqualTo(10000);
        assertThat(logStatus.usagePercent()).isEqualTo(50.0);
    }

    @Test
    void health_shouldIncludeMetricDetails() {
        setupRepositories(100, 1000, 75, 10);

        Health health = healthIndicator.health();

        JObsHealthIndicator.MetricsStatus metricsStatus =
                (JObsHealthIndicator.MetricsStatus) health.getDetails().get("metrics");
        assertThat(metricsStatus.totalMetrics()).isEqualTo(75);
    }

    @Test
    void health_shouldIncludeAlertEventDetails() {
        setupRepositories(100, 1000, 50, 100);

        Health health = healthIndicator.health();

        JObsHealthIndicator.RepositoryStatus alertStatus =
                (JObsHealthIndicator.RepositoryStatus) health.getDetails().get("alertEvents");
        assertThat(alertStatus.current()).isEqualTo(100);
        assertThat(alertStatus.max()).isEqualTo(500);
        assertThat(alertStatus.usagePercent()).isEqualTo(20.0);
    }

    // ==================== Memory Estimation ====================

    @Test
    void health_shouldIncludeMemoryEstimation() {
        setupRepositories(100, 1000, 50, 10);

        Health health = healthIndicator.health();

        assertThat(health.getDetails()).containsKey("estimatedMemoryMb");
    }

    // ==================== Error Handling ====================

    @Test
    void health_shouldReturnDownOnException() {
        when(traceRepository.stats()).thenThrow(new RuntimeException("Database error"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error").toString())
                .contains("Failed to check J-Obs health");
    }

    // ==================== Edge Cases ====================

    @Test
    void health_shouldHandleZeroMaxValues() {
        JObsHealthIndicator zeroMaxIndicator = new JObsHealthIndicator(
                traceRepository, logRepository, metricRepository, alertEventRepository,
                0, 0, 0
        );

        when(traceRepository.stats()).thenReturn(new TraceRepository.TraceStats(100, 0, 0, 0.0, 0.0, 0.0, 0.0));
        when(logRepository.stats()).thenReturn(new LogRepository.LogStats(100, 0, 0, 0, 0, 0, 0, 0));
        when(metricRepository.stats()).thenReturn(new MetricRepository.MetricStats(50, 0, 0, 0, 0, 0, 0, 0));
        when(alertEventRepository.count()).thenReturn(10L);

        Health health = zeroMaxIndicator.health();

        // Should still work (usage = 0 when max = 0)
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void health_shouldHandleEmptyRepositories() {
        setupRepositories(0, 0, 0, 0);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);

        JObsHealthIndicator.RepositoryStatus traceStatus =
                (JObsHealthIndicator.RepositoryStatus) health.getDetails().get("traces");
        assertThat(traceStatus.usagePercent()).isEqualTo(0.0);
    }
}
