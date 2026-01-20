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
 * Auto-configuration for J-Obs internal metrics.
 * <p>
 * Exposes internal J-Obs metrics via Micrometer:
 * <ul>
 *   <li>jobs.logs.* - Log repository metrics</li>
 *   <li>jobs.traces.* - Trace repository metrics</li>
 *   <li>jobs.factory.* - LogEntryFactory pool metrics</li>
 * </ul>
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
