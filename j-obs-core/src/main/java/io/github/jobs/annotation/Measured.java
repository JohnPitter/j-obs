package io.github.jobs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class for automatic metrics collection.
 * Automatically creates a Timer for method execution time and a Counter for invocations.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Service
 * @Measured  // All methods will have metrics
 * public class OrderService {
 *
 *     public Order create(OrderRequest request) {
 *         // Automatically records:
 *         // - Timer: "method.timed" with tags class=OrderService, method=create
 *         // - Counter: "method.calls" with tags class=OrderService, method=create
 *     }
 *
 *     @Measured(name = "payment.process", percentiles = {0.5, 0.95, 0.99})
 *     public void processPayment(Payment payment) {
 *         // Custom metric name with percentiles
 *     }
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Measured {

    /**
     * Custom metric name. If empty, uses "method.timed" with class/method tags.
     */
    String name() default "";

    /**
     * Description for the metric.
     */
    String description() default "";

    /**
     * Whether to record a counter for method invocations.
     * Default is true.
     */
    boolean countCalls() default true;

    /**
     * Whether to record execution time.
     * Default is true.
     */
    boolean recordTime() default true;

    /**
     * Whether to record a counter for exceptions.
     * Default is true.
     */
    boolean countExceptions() default true;

    /**
     * Percentiles to compute for the timer.
     * Default is p50, p95, p99.
     */
    double[] percentiles() default {0.5, 0.95, 0.99};

    /**
     * Additional tags to add to the metrics.
     * Format: "key=value"
     */
    String[] tags() default {};
}
