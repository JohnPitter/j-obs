package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.MetricRepository;
import io.github.jobs.spring.metric.CachedMetricRepository;
import io.github.jobs.spring.metric.MicrometerMetricRepository;
import io.github.jobs.spring.web.MetricApiController;
import io.github.jobs.spring.web.MetricController;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * Auto-configuration for J-Obs metrics functionality.
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "j-obs.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class JObsMetricAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public MetricRepository metricRepository(MeterRegistry meterRegistry, JObsProperties properties) {
        int maxHistory = properties.getMetrics().getMaxHistoryPoints();
        MicrometerMetricRepository baseRepository = new MicrometerMetricRepository(meterRegistry, maxHistory);
        // Wrap with caching layer - cache TTL is based on refresh interval
        Duration cacheTtl = Duration.ofMillis(properties.getMetrics().getRefreshInterval());
        return new CachedMetricRepository(baseRepository, cacheTtl);
    }

    @Bean
    @ConditionalOnBean(MetricRepository.class)
    public MetricApiController metricApiController(MetricRepository metricRepository) {
        return new MetricApiController(metricRepository);
    }

    @Bean
    @ConditionalOnBean(MetricRepository.class)
    public MetricController metricController(MetricRepository metricRepository, JObsProperties properties) {
        return new MetricController(metricRepository, properties);
    }

    @Bean
    @ConditionalOnBean(MetricRepository.class)
    public MetricRefreshScheduler metricRefreshScheduler(MetricRepository metricRepository) {
        return new MetricRefreshScheduler(metricRepository);
    }

    /**
     * Scheduler for periodic metric history refresh.
     */
    public static class MetricRefreshScheduler {
        private final MetricRepository metricRepository;

        public MetricRefreshScheduler(MetricRepository metricRepository) {
            this.metricRepository = metricRepository;
        }

        @Scheduled(fixedRateString = "${j-obs.metrics.refresh-interval:10000}")
        public void refreshMetrics() {
            metricRepository.refresh();
        }
    }
}
