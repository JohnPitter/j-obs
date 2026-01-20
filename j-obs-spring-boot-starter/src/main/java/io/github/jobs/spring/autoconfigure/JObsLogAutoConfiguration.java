package io.github.jobs.spring.autoconfigure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.github.jobs.application.LogRepository;
import io.github.jobs.infrastructure.InMemoryLogRepository;
import io.github.jobs.spring.log.JObsLogAppender;
import io.github.jobs.spring.log.LogEntryFactory;
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
    public LogEntryFactory logEntryFactory() {
        return new LogEntryFactory();
    }

    /**
     * Configuration for WebSocket support (optional).
     * <p>
     * This configuration is only loaded when:
     * <ul>
     *   <li>WebSocket classes are available on the classpath</li>
     *   <li>A real {@code ServerContainer} is present in the ServletContext</li>
     * </ul>
     * <p>
     * In test environments using {@code MockServletContext}, the ServerContainer
     * attribute is not set, so this configuration will be skipped automatically.
     * This prevents failures when running tests without a real servlet container.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
        "org.springframework.web.socket.config.annotation.WebSocketConfigurer",
        "jakarta.websocket.server.ServerContainer"
    })
    @ConditionalOnServerContainer
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
}
