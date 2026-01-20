package io.github.jobs.spring.autoconfigure;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Conditional @Conditional} that only matches when a WebSocket
 * {@code ServerContainer} is available in the {@code ServletContext}.
 * <p>
 * This annotation should be used on WebSocket-related configuration classes
 * to prevent them from loading in test environments that use
 * {@code MockServletContext}. The mock servlet context does not provide
 * a real WebSocket container, causing failures when Spring tries to
 * initialize WebSocket infrastructure.
 * <p>
 * Example usage:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnServerContainer
 * public class MyWebSocketConfig {
 *     // WebSocket configuration that requires a real ServerContainer
 * }
 * }</pre>
 *
 * @see OnServerContainerCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnServerContainerCondition.class)
public @interface ConditionalOnServerContainer {
}
