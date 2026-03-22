package io.github.jobs.spring.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.AlertRepository;
import io.github.jobs.application.SloRepository;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.persistence.JdbcAlertEventRepository;
import io.github.jobs.spring.persistence.JdbcAlertRepository;
import io.github.jobs.spring.persistence.JdbcSloRepository;
import io.github.jobs.spring.persistence.JdbcTraceRepository;
import io.github.jobs.spring.persistence.JObsSchemaInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Auto-configuration for JDBC-backed persistence in J-Obs.
 * <p>
 * Activates when:
 * <ul>
 *   <li>{@code j-obs.enabled} is true (default)</li>
 *   <li>{@code j-obs.persistence.enabled} is true (NOT default -- must be explicitly enabled)</li>
 *   <li>{@link JdbcTemplate} is on the classpath</li>
 *   <li>A {@link DataSource} bean is available</li>
 * </ul>
 * <p>
 * This configuration runs before the in-memory auto-configurations so that JDBC beans
 * are registered first, preventing the in-memory fallbacks from being created.
 */
@AutoConfiguration
@AutoConfigureBefore({
        JObsTraceAutoConfiguration.class,
        JObsAlertAutoConfiguration.class,
        JObsAlertNotificationAutoConfiguration.class,
        JObsSloAutoConfiguration.class
})
@ConditionalOnProperty(name = "j-obs.persistence.enabled", havingValue = "true")
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsPersistenceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsPersistenceAutoConfiguration.class);

    @Bean
    public JObsSchemaInitializer jObsSchemaInitializer(DataSource dataSource) {
        log.info("Initializing J-Obs JDBC schema");
        return new JObsSchemaInitializer(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(TraceRepository.class)
    public TraceRepository traceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        log.info("Creating JDBC-backed TraceRepository");
        return new JdbcTraceRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AlertRepository.class)
    public AlertRepository alertRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        log.info("Creating JDBC-backed AlertRepository");
        return new JdbcAlertRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AlertEventRepository.class)
    public AlertEventRepository alertEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        log.info("Creating JDBC-backed AlertEventRepository");
        return new JdbcAlertEventRepository(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SloRepository.class)
    public SloRepository sloRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        log.info("Creating JDBC-backed SloRepository");
        return new JdbcSloRepository(jdbcTemplate, objectMapper);
    }
}
