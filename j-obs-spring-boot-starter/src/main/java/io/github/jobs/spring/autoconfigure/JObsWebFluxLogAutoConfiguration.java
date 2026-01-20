package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.LogRepository;
import io.github.jobs.spring.webflux.ReactiveLogApiController;
import io.github.jobs.spring.webflux.ReactiveLogStreamHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for reactive log streaming in WebFlux applications.
 * <p>
 * Provides Server-Sent Events based log streaming as an alternative to WebSocket
 * in reactive applications.
 */
@AutoConfiguration(after = JObsLogAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.server.RouterFunction")
public class JObsWebFluxLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(LogRepository.class)
    public ReactiveLogStreamHandler reactiveLogStreamHandler(LogRepository logRepository) {
        return new ReactiveLogStreamHandler(logRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ReactiveLogStreamHandler.class)
    public ReactiveLogApiController reactiveLogApiController(ReactiveLogStreamHandler streamHandler) {
        return new ReactiveLogApiController(streamHandler);
    }
}
