package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.LogRepository;
import io.github.jobs.application.MetricRepository;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.actuator.JObsHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs Spring Boot Actuator integration.
 */
@AutoConfiguration(after = {
        JObsTraceAutoConfiguration.class,
        JObsLogAutoConfiguration.class,
        JObsMetricAutoConfiguration.class,
        JObsAlertNotificationAutoConfiguration.class
})
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsActuatorAutoConfiguration {

    private final JObsProperties properties;

    public JObsActuatorAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "jObsHealthIndicator")
    @ConditionalOnBean({TraceRepository.class, LogRepository.class, MetricRepository.class, AlertEventRepository.class})
    public HealthIndicator jObsHealthIndicator(
            TraceRepository traceRepository,
            LogRepository logRepository,
            MetricRepository metricRepository,
            AlertEventRepository alertEventRepository) {

        return new JObsHealthIndicator(
                traceRepository,
                logRepository,
                metricRepository,
                alertEventRepository,
                properties.getTraces().getMaxTraces(),
                properties.getLogs().getMaxEntries(),
                properties.getAlerts().getMaxEvents()
        );
    }
}
