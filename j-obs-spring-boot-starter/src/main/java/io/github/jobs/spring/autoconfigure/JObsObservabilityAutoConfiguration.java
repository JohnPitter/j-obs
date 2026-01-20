package io.github.jobs.spring.autoconfigure;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Auto-configuration for handling observability conflicts between J-Obs and Spring Boot Actuator.
 * <p>
 * This configuration resolves the common {@code TracingContext} errors that occur when:
 * <ul>
 *   <li>Spring Boot Actuator's tracing is disabled but observation handlers are still active</li>
 *   <li>{@code @Scheduled} methods are instrumented but tracing context is not available</li>
 *   <li>Multiple OpenTelemetry initializers conflict</li>
 * </ul>
 * <p>
 * When enabled, this configuration:
 * <ul>
 *   <li>Configures {@code @Scheduled} tasks to use {@link ObservationRegistry#NOOP} to prevent TracingContext errors</li>
 *   <li>Provides an {@link ObservationPredicate} to filter problematic observations</li>
 * </ul>
 *
 * @see <a href="https://github.com/micrometer-metrics/micrometer/issues/3901">Micrometer Issue #3901</a>
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/35669">Spring Boot Issue #35669</a>
 */
@AutoConfiguration
@ConditionalOnClass({ObservationRegistry.class, SchedulingConfigurer.class})
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsObservabilityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsObservabilityAutoConfiguration.class);

    /**
     * Configures scheduled tasks to use NOOP observation registry when J-Obs manages tracing.
     * <p>
     * This prevents the {@code IllegalArgumentException: Context does not have an entry for key
     * TracingObservationHandler$TracingContext} error that occurs when Spring Boot Actuator's
     * tracing observation handlers are active but the tracing context is not properly initialized.
     * <p>
     * The NOOP registry accepts all observation calls but performs no actual observation,
     * effectively disabling observation for scheduled tasks while allowing J-Obs to handle
     * tracing through its own mechanism.
     *
     * @return a SchedulingConfigurer that sets NOOP observation registry for scheduled tasks
     */
    @Bean
    @ConditionalOnProperty(name = "j-obs.observability.scheduled-tasks-noop", havingValue = "true", matchIfMissing = true)
    public SchedulingConfigurer jObsSchedulingConfigurer() {
        log.info("J-Obs: Configuring scheduled tasks with NOOP ObservationRegistry to prevent TracingContext conflicts");
        return taskRegistrar -> taskRegistrar.setObservationRegistry(ObservationRegistry.NOOP);
    }

    /**
     * Provides an ObservationPredicate that filters out tracing-related observations
     * when J-Obs is managing OpenTelemetry to prevent conflicts.
     * <p>
     * This predicate disables observations for:
     * <ul>
     *   <li>Spring Security observations (spring.security.*)</li>
     *   <li>HTTP client observations when J-Obs traces are enabled</li>
     * </ul>
     *
     * @param properties J-Obs configuration properties
     * @return an ObservationPredicate for filtering conflicting observations
     */
    @Bean
    @ConditionalOnProperty(name = "j-obs.observability.filter-conflicting", havingValue = "true", matchIfMissing = false)
    public ObservationPredicate jObsObservationPredicate(JObsProperties properties) {
        log.info("J-Obs: Registering ObservationPredicate to filter conflicting observations");
        return (name, context) -> {
            // Filter out Spring Security observations if they cause issues
            if (name.startsWith("spring.security.")) {
                log.debug("J-Obs: Filtering out observation: {}", name);
                return false;
            }
            return true;
        };
    }
}
