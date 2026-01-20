package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.AlertRepository;
import io.github.jobs.infrastructure.InMemoryAlertRepository;
import io.github.jobs.spring.web.AlertApiController;
import io.github.jobs.spring.web.AlertController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs alert management.
 */
@AutoConfiguration(after = JObsAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsAlertAutoConfiguration {

    private final JObsProperties properties;

    public JObsAlertAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertRepository alertRepository() {
        return new InMemoryAlertRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertController alertController() {
        return new AlertController(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertApiController alertApiController(AlertRepository alertRepository) {
        return new AlertApiController(alertRepository);
    }
}
