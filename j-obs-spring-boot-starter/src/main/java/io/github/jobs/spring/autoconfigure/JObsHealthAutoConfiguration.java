package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.HealthRepository;
import io.github.jobs.spring.health.ActuatorHealthRepository;
import io.github.jobs.spring.web.HealthApiController;
import io.github.jobs.spring.web.HealthController;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs health check functionality.
 */
@AutoConfiguration(after = HealthEndpointAutoConfiguration.class)
@ConditionalOnClass(HealthEndpoint.class)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
public class JObsHealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(HealthEndpoint.class)
    public HealthRepository healthRepository(HealthEndpoint healthEndpoint, JObsProperties properties) {
        int maxHistory = properties.getHealth().getMaxHistoryEntries();
        return new ActuatorHealthRepository(healthEndpoint, maxHistory);
    }

    @Bean
    @ConditionalOnBean(HealthRepository.class)
    public HealthApiController healthApiController(HealthRepository healthRepository) {
        return new HealthApiController(healthRepository);
    }

    @Bean
    @ConditionalOnBean(HealthRepository.class)
    public HealthController healthController(HealthRepository healthRepository, JObsProperties properties) {
        return new HealthController(healthRepository, properties);
    }
}
