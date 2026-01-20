package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.LogRepository;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.log.LogEntryFactory;
import io.github.jobs.spring.metric.JObsInternalMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs internal metrics exposed via Micrometer.
 * <p>
 * This configuration is activated when:
 * <ul>
 *   <li>Micrometer's {@link MeterRegistry} is available on classpath</li>
 *   <li>A MeterRegistry bean exists in the context</li>
 *   <li>Property {@code j-obs.metrics.internal.enabled} is {@code true} (default)</li>
 * </ul>
 * <p>
 * Metrics exposed (prefix: {@code jobs}):
 * <p>
 * <strong>Log metrics:</strong>
 * <ul>
 *   <li>{@code jobs.logs.stored} - Total log entries in buffer</li>
 *   <li>{@code jobs.logs.buffer.capacity} - Maximum buffer capacity</li>
 *   <li>{@code jobs.logs.buffer.utilization} - Buffer utilization percentage</li>
 *   <li>{@code jobs.logs.by_level} - Log count by level (tagged)</li>
 * </ul>
 * <p>
 * <strong>Trace metrics:</strong>
 * <ul>
 *   <li>{@code jobs.traces.stored} - Total traces in repository</li>
 *   <li>{@code jobs.traces.spans.total} - Total spans across all traces</li>
 *   <li>{@code jobs.traces.with_errors} - Traces containing errors</li>
 *   <li>{@code jobs.traces.duration.avg} - Average trace duration (ms)</li>
 *   <li>{@code jobs.traces.duration.p50/p95/p99} - Trace duration percentiles</li>
 * </ul>
 * <p>
 * <strong>Factory metrics:</strong>
 * <ul>
 *   <li>{@code jobs.factory.pool.size} - Object pool size</li>
 *   <li>{@code jobs.factory.ids.generated} - Total IDs generated</li>
 * </ul>
 *
 * @see JObsInternalMetrics
 * @see JObsLogAutoConfiguration
 * @see JObsTraceAutoConfiguration
 */
@AutoConfiguration(after = {
        CompositeMeterRegistryAutoConfiguration.class,
        JObsLogAutoConfiguration.class,
        JObsTraceAutoConfiguration.class
})
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "j-obs.metrics.internal.enabled", havingValue = "true", matchIfMissing = true)
public class JObsInternalMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JObsInternalMetrics jObsInternalMetrics(
            MeterRegistry meterRegistry,
            ObjectProvider<LogRepository> logRepositoryProvider,
            ObjectProvider<TraceRepository> traceRepositoryProvider,
            ObjectProvider<LogEntryFactory> logEntryFactoryProvider) {

        LogRepository logRepository = logRepositoryProvider.getIfAvailable();
        TraceRepository traceRepository = traceRepositoryProvider.getIfAvailable();
        LogEntryFactory logEntryFactory = logEntryFactoryProvider.getIfAvailable();

        if (logRepository == null && traceRepository == null) {
            return null;
        }

        return new JObsInternalMetrics(
                meterRegistry,
                logRepository,
                traceRepository,
                logEntryFactory
        );
    }
}
