package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.ProfilingService;
import io.github.jobs.infrastructure.InMemoryProfilingRepository;
import io.github.jobs.spring.profiling.DefaultProfilingService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs profiling features.
 */
@AutoConfiguration
@EnableConfigurationProperties(JObsProperties.class)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
public class JObsProfilingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryProfilingRepository profilingRepository(JObsProperties properties) {
        return new InMemoryProfilingRepository(properties.getProfiling().getMaxSessions());
    }

    @Bean
    @ConditionalOnMissingBean
    public ProfilingService profilingService(InMemoryProfilingRepository repository) {
        return new DefaultProfilingService(repository);
    }
}
