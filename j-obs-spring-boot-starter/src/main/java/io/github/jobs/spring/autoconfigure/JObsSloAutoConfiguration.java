package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.SloRepository;
import io.github.jobs.application.SloService;
import io.github.jobs.infrastructure.InMemorySloRepository;
import io.github.jobs.spring.slo.DefaultSloService;
import io.github.jobs.spring.slo.SloScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for SLO/SLI tracking.
 */
@AutoConfiguration
@EnableConfigurationProperties(JObsProperties.class)
@ConditionalOnProperty(prefix = "j-obs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JObsSloAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsSloAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "j-obs.slo", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SloRepository sloRepository(JObsProperties properties) {
        int maxHistory = properties.getSlo().getMaxEvaluationHistory();
        log.info("Creating in-memory SLO repository with max history: {}", maxHistory);
        return new InMemorySloRepository(maxHistory);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({SloRepository.class, MeterRegistry.class})
    @ConditionalOnProperty(prefix = "j-obs.slo", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SloService sloService(SloRepository repository, MeterRegistry meterRegistry) {
        log.debug("Creating SLO service");
        return new DefaultSloService(repository, meterRegistry);
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnBean(SloService.class)
    @ConditionalOnProperty(prefix = "j-obs.slo", name = "enabled", havingValue = "true", matchIfMissing = true)
    public SloScheduler sloScheduler(SloService sloService, JObsProperties properties) {
        SloScheduler scheduler = new SloScheduler(
                sloService,
                properties.getSlo().getEvaluationInterval()
        );
        scheduler.start();
        return scheduler;
    }
}
