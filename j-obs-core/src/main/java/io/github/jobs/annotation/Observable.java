package io.github.jobs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Combines @Traced and @Measured for full observability.
 * This is a convenience annotation that enables both tracing and metrics.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Service
 * @Observable  // Full observability for all methods
 * public class OrderService {
 *
 *     public Order create(OrderRequest request) {
 *         // Automatically:
 *         // - Creates trace span
 *         // - Records execution time metric
 *         // - Counts invocations
 *         // - Logs method entry/exit (if enabled)
 *     }
 * }
 * }
 * </pre>
 *
 * <p>Note: The {@code name} attribute is used for both span and metric names.
 * This annotation does not use Spring's @AliasFor to remain framework-agnostic
 * in the core module.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Observable {

    /**
     * Custom name for both span and metric.
     * If empty, uses "ClassName.methodName".
     */
    String value() default "";

    /**
     * Whether to log method entry and exit.
     * Default is false.
     */
    boolean logMethodCalls() default false;

    /**
     * Whether to record exceptions as span events.
     * Default is true.
     */
    boolean recordException() default true;

    /**
     * Whether to include method parameters as span attributes.
     * Default is false for security reasons.
     */
    boolean includeParameters() default false;
}
