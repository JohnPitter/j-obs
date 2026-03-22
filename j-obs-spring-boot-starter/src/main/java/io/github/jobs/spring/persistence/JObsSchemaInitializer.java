package io.github.jobs.spring.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Initializes J-Obs database schema on startup.
 * Reads and executes the j-obs-schema.sql script from the classpath.
 */
public class JObsSchemaInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(JObsSchemaInitializer.class);
    private static final String SCHEMA_RESOURCE = "j-obs-schema.sql";

    private final DataSource dataSource;

    public JObsSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        try (Connection connection = dataSource.getConnection()) {
            ClassPathResource resource = new ClassPathResource(SCHEMA_RESOURCE);
            if (!resource.exists()) {
                log.warn("J-Obs schema file not found on classpath: {}", SCHEMA_RESOURCE);
                return;
            }

            ScriptUtils.executeSqlScript(connection, resource);
            log.info("J-Obs database schema initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize J-Obs database schema: {}", e.getMessage());
            // Do not propagate -- persistence failure should not crash the host application
        }
    }
}
