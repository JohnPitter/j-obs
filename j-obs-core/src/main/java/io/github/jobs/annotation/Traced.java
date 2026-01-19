package io.github.jobs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class for automatic tracing.
 * When applied to a class, all public methods will be traced.
 * When applied to a method, only that method will be traced.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Service
 * @Traced  // All methods in this class will be traced
 * public class OrderService {
 *
 *     public Order create(OrderRequest request) {
 *         // Automatically creates span: "OrderService.create"
 *     }
 *
 *     @Traced(name = "process-payment", recordException = true)
 *     public void processPayment(Payment payment) {
 *         // Creates span with custom name: "process-payment"
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {

    /**
     * Custom span name. If empty, uses "ClassName.methodName".
     */
    String name() default "";

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

    /**
     * Whether to include the return value as a span attribute.
     * Default is false for security reasons.
     */
    boolean includeResult() default false;

    /**
     * Additional static attributes to add to the span.
     * Format: "key=value"
     */
    String[] attributes() default {};
}
