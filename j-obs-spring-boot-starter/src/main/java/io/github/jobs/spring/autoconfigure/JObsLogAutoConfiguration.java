package io.github.jobs.spring.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.jobs.application.LogRepository;
import io.github.jobs.infrastructure.InMemoryLogRepository;
import io.github.jobs.spring.log.JObsLogAppender;
import io.github.jobs.spring.web.LogApiController;
import io.github.jobs.spring.web.LogController;
import io.github.jobs.spring.websocket.LogWebSocketHandler;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Auto-configuration for J-Obs log collection and real-time streaming.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.logs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
@EnableWebSocket
public class JObsLogAutoConfiguration implements WebSocketConfigurer {

    private final JObsProperties properties;

    public JObsLogAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public LogRepository logRepository() {
        return new InMemoryLogRepository(properties.getLogs().getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean
    public LogController logController(LogRepository logRepository) {
        return new LogController(logRepository, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogApiController logApiController(LogRepository logRepository) {
        return new LogApiController(logRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogWebSocketHandler logWebSocketHandler(LogRepository logRepository) {
        return new LogWebSocketHandler(logRepository);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String wsPath = properties.getPath() + "/ws/logs";
        registry.addHandler(logWebSocketHandler(logRepository()), wsPath)
                .setAllowedOrigins("*");
    }

    /**
     * Configuration for Logback integration.
     */
    @ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
    static class LogbackConfiguration {

        @Bean
        public JObsLogAppender jObsLogAppender(LogRepository logRepository) {
            JObsLogAppender appender = new JObsLogAppender();
            appender.setLogRepository(logRepository);
            appender.setName("J-OBS");
            appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
            appender.start();

            // Attach to root logger
            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(appender);

            return appender;
        }
    }
}
