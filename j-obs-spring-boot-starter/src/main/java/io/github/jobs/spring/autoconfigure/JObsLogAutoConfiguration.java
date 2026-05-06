package io.github.jobs.spring.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.jobs.application.LogRepository;
import io.github.jobs.infrastructure.InMemoryLogRepository;
import io.github.jobs.spring.log.JObsLog4j2Appender;
import io.github.jobs.spring.log.JObsLogAppender;
import io.github.jobs.spring.log.LogEntryFactory;
import io.github.jobs.spring.web.LogApiController;
import io.github.jobs.spring.web.LogController;
import io.github.jobs.spring.web.template.TemplateService;
import io.github.jobs.spring.websocket.LogWebSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
 * This configuration is activated when {@code j-obs.logs.enabled} is {@code true} (default).
 * <p>
 * Components configured:
 * <ul>
 *   <li>{@link LogRepository} - In-memory circular buffer for log storage</li>
 *   <li>{@link LogController} - UI controller for log viewer</li>
 *   <li>{@link LogApiController} - REST API for log queries</li>
 *   <li>{@link LogEntryFactory} - Factory with object pooling for log entries</li>
 * </ul>
 * <p>
 * Optional integrations (conditionally enabled):
 * <ul>
 *   <li><strong>WebSocket</strong> - Real-time log streaming when WebSocket is on classpath</li>
 *   <li><strong>Logback</strong> - Automatic log capture via custom appender when Logback is present</li>
 * </ul>
 * <p>
 * Configuration properties under {@code j-obs.logs}:
 * <ul>
 *   <li>{@code enabled} - Enable/disable log collection (default: true)</li>
 *   <li>{@code max-entries} - Maximum log entries in buffer (default: 10000)</li>
 *   <li>{@code min-level} - Minimum log level to capture (default: INFO)</li>
 *   <li>{@code websocket.*} - WebSocket streaming configuration</li>
 * </ul>
 *
 * @see LogRepository
 * @see JObsProperties.Logs
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
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
    public LogController logController(LogRepository logRepository, TemplateService templateService) {
        return new LogController(logRepository, properties, templateService);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogApiController logApiController(LogRepository logRepository) {
        return new LogApiController(logRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogEntryFactory logEntryFactory() {
        return new LogEntryFactory();
    }

    /**
     * Configuration for WebSocket support (optional).
     * <p>
     * This configuration is only loaded when WebSocket classes are available on the classpath
     * ({@code spring-boot-starter-websocket}).
     * <p>
     * In test environments, disable with {@code j-obs.enabled=false}.
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
            return new LogWebSocketHandler(logRepository, properties);
        }

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            String wsPath = properties.getPath() + "/ws/logs";
            LogWebSocketHandler handler = logWebSocketHandler();
            registry.addHandler(handler, wsPath)
                    .addInterceptors(handler.createAuthHandshakeInterceptor())
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
        public JObsLogAppender jObsLogAppender(LogRepository logRepository, LogEntryFactory logEntryFactory) {
            JObsLogAppender appender = new JObsLogAppender();
            appender.setLogRepository(logRepository);
            appender.setLogEntryFactory(logEntryFactory);
            appender.setName("J-OBS");
            appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
            appender.start();

            // Attach to root logger
            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(appender);

            return appender;
        }
    }
    /**
     * Configuration for Log4j2 integration (optional).
     * Only loaded when Log4j2 Core is on the classpath AND Logback is NOT present.
     * When both are on the classpath, Logback takes priority (Spring Boot default).
     *
     * Uses @ConditionalOnMissingClass instead of @ConditionalOnMissingBean because
     * @ConditionalOnMissingBean on a @Configuration class is evaluated before any beans
     * are created, making it unreliable for inter-configuration ordering.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.logging.log4j.core.appender.AbstractAppender")
    @ConditionalOnMissingClass("ch.qos.logback.classic.Logger")
    static class Log4j2Configuration {

        @Bean
        @ConditionalOnMissingBean
        public JObsLog4j2Appender jObsLog4j2Appender(LogRepository logRepository, LogEntryFactory logEntryFactory) {
            JObsLog4j2Appender appender = new JObsLog4j2Appender("J-OBS");
            appender.setLogRepository(logRepository);
            appender.setLogEntryFactory(logEntryFactory);
            appender.start();

            // Attach to root logger only when Log4j2 is the real logging backend.
            // When Log4j2 is bridged to SLF4J (log4j-to-slf4j), getContext() returns
            // an SLF4J-backed context that is not a Log4j2 LoggerContext — safe to skip.
            try {
                org.apache.logging.log4j.spi.LoggerContext ctx = LogManager.getContext(false);
                if (ctx instanceof org.apache.logging.log4j.core.LoggerContext log4j2Ctx) {
                    log4j2Ctx.getRootLogger().addAppender(appender);
                }
            } catch (Exception | Error ignored) {
                // Log4j2 context unavailable or bridged — appender bean is still valid
            }

            return appender;
        }
    }
}
