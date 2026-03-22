package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.retention.DataRetentionService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs data retention policies.
 * <p>
 * Registers a {@link DataRetentionService} that periodically removes expired
 * traces and alert events based on the configured retention durations.
 * <p>
 * This configuration is only activated when both {@link TraceRepository} and
 * {@link AlertEventRepository} beans are present in the application context.
 */
@AutoConfiguration(after = {JObsTraceAutoConfiguration.class, JObsAlertAutoConfiguration.class})
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsRetentionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({TraceRepository.class, AlertEventRepository.class})
    public DataRetentionService dataRetentionService(
            TraceRepository traceRepository,
            AlertEventRepository alertEventRepository,
            JObsProperties properties) {
        return new DataRetentionService(
                traceRepository,
                alertEventRepository,
                properties.getTraces().getRetention(),
                properties.getAlerts().getRetention()
        );
    }
}
