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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Auto-configuration for J-Obs log collection and real-time streaming.
 * <p>
 * WebSocket support is optional and will only be enabled when WebSocket
 * classes are available on the classpath.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.logs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsLogAutoConfiguration {

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

    /**
     * Configuration for WebSocket support (optional).
     * <p>
     * This configuration is only loaded when WebSocket classes are available
     * on the classpath AND a ServerContainer is present (not available in
     * MockServletContext test environments).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
        "org.springframework.web.socket.config.annotation.WebSocketConfigurer",
        "jakarta.websocket.server.ServerContainer"
    })
    @EnableWebSocket
    static class WebSocketConfiguration implements WebSocketConfigurer {

        private final JObsProperties properties;
        private final LogRepository logRepository;

        WebSocketConfiguration(JObsProperties properties, LogRepository logRepository) {
            this.properties = properties;
            this.logRepository = logRepository;
        }

        @Bean
        @ConditionalOnMissingBean
        public LogWebSocketHandler logWebSocketHandler() {
            return new LogWebSocketHandler(logRepository);
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            String wsPath = properties.getPath() + "/ws/logs";
            registry.addHandler(logWebSocketHandler(), wsPath)
                    .setAllowedOrigins("*");
        }
    }

    /**
     * Configuration for Logback integration (optional).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
    static class LogbackConfiguration {

        @Bean
        @ConditionalOnMissingBean
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
