package io.github.jobs.spring.autoconfigure;

import jakarta.websocket.server.ServerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Configuration for WebSocket settings including compression.
 * <p>
 * This configuration customizes the WebSocket container settings for optimal
 * performance when streaming logs. The permessage-deflate compression extension
 * can significantly reduce bandwidth usage for text-based protocols like JSON.
 * <p>
 * This configuration is only loaded when:
 * <ul>
 *   <li>WebSocket classes are on the classpath ({@code spring-boot-starter-websocket})</li>
 *   <li>{@code j-obs.enabled} is {@code true} (default)</li>
 * </ul>
 * <p>
 * In test environments, disable with {@code j-obs.enabled=false}.
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code j-obs.logs.websocket.compression-enabled} - Enable compression (default: true)</li>
 *   <li>{@code j-obs.logs.websocket.max-text-message-size} - Max message size (default: 65536)</li>
 *   <li>{@code j-obs.logs.websocket.send-buffer-size} - Send buffer size (default: 16384)</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass({ServerContainer.class, ServletServerContainerFactoryBean.class})
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsWebSocketConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsWebSocketConfiguration.class);

    @Autowired
    private JObsProperties properties;

    /**
     * Configures the WebSocket container with optimized settings for log streaming.
     * <p>
     * The settings configured include:
     * <ul>
     *   <li>Text message buffer size for large log entries</li>
     *   <li>Async send timeout for non-blocking operations</li>
     * </ul>
     * <p>
     * Note: permessage-deflate compression is enabled at the container level
     * for Tomcat 8.5+ and Jetty 9.4+ by default. This bean configures buffer
     * sizes to optimize compression performance.
     *
     * @return the container factory bean
     */
    @Bean
    public ServletServerContainerFactoryBean websocketContainerFactoryBean() {
        JObsProperties.Logs.WebSocket wsConfig = properties.getLogs().getWebsocket();

        ServletServerContainerFactoryBean factory = new ServletServerContainerFactoryBean();

        // Set max text message buffer size (important for large log entries with stacktraces)
        factory.setMaxTextMessageBufferSize(wsConfig.getMaxTextMessageSize());

        // Set async send timeout to prevent blocking on slow clients
        factory.setAsyncSendTimeout(5000L);

        // Set max session idle timeout (5 minutes)
        factory.setMaxSessionIdleTimeout(300000L);

        log.info("J-Obs WebSocket container configured: maxTextMessageSize={}, compressionEnabled={}",
                wsConfig.getMaxTextMessageSize(), wsConfig.isCompressionEnabled());

        return factory;
    }
}
