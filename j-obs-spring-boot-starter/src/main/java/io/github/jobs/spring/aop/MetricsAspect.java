package io.github.jobs.spring.aop;

import io.github.jobs.annotation.Measured;
import io.github.jobs.annotation.Observable;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect that automatically records metrics for methods
 * annotated with @Measured or @Observable.
 */
@Aspect
public class MetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricsAspect.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Counter> exceptionCounters = new ConcurrentHashMap<>();

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut for methods annotated with @Measured.
     */
    @Pointcut("@annotation(io.github.jobs.annotation.Measured)")
    public void measuredMethod() {}

    /**
     * Pointcut for methods in classes annotated with @Measured.
     */
    @Pointcut("@within(io.github.jobs.annotation.Measured)")
    public void measuredClass() {}

    /**
     * Pointcut for methods annotated with @Observable.
     */
    @Pointcut("@annotation(io.github.jobs.annotation.Observable)")
    public void observableMethod() {}

    /**
     * Pointcut for methods in classes annotated with @Observable.
     */
    @Pointcut("@within(io.github.jobs.annotation.Observable)")
    public void observableClass() {}

    /**
     * Around advice for measured methods.
     */
    @Around("(measuredMethod() || measuredClass() || observableMethod() || observableClass()) && execution(* *(..))")
    public Object measureMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Get annotation configuration
        Measured measured = getMeasuredAnnotation(method, targetClass);
        if (measured == null) {
            return joinPoint.proceed();
        }

        String className = targetClass.getSimpleName();
        String methodName = method.getName();
        String metricKey = className + "." + methodName;

        // Get or create metrics
        Timer timer = measured.recordTime() ? getOrCreateTimer(measured, className, methodName, metricKey) : null;
        Counter callCounter = measured.countCalls() ? getOrCreateCounter(measured, className, methodName, metricKey) : null;
        Counter exceptionCounter = measured.countExceptions() ? getOrCreateExceptionCounter(measured, className, methodName, metricKey) : null;

        // Increment call counter
        if (callCounter != null) {
            callCounter.increment();
        }

        // Record time
        if (timer != null) {
            return timer.recordCallable(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (exceptionCounter != null) {
                        exceptionCounter.increment();
                    }
                    if (e instanceof Exception) {
                        throw (Exception) e;
                    }
                    throw new RuntimeException(e);
                }
            });
        } else {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (exceptionCounter != null) {
                    exceptionCounter.increment();
                }
                throw e;
            }
        }
    }

    private Measured getMeasuredAnnotation(Method method, Class<?> targetClass) {
        // Method-level annotation takes precedence
        Measured methodAnnotation = method.getAnnotation(Measured.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // Check for @Observable on method
        Observable observableMethod = method.getAnnotation(Observable.class);
        if (observableMethod != null) {
            return createMeasuredFromObservable(observableMethod);
        }

        // Class-level annotation
        Measured classAnnotation = targetClass.getAnnotation(Measured.class);
        if (classAnnotation != null) {
            return classAnnotation;
        }

        // Check for @Observable on class
        Observable observableClass = targetClass.getAnnotation(Observable.class);
        if (observableClass != null) {
            return createMeasuredFromObservable(observableClass);
        }

        return null;
    }

    private Measured createMeasuredFromObservable(Observable observable) {
        return new Measured() {
            @Override
            public Class<Measured> annotationType() {
                return Measured.class;
            }

            @Override
            public String name() {
                return observable.value();
            }

            @Override
            public String description() {
                return "";
            }

            @Override
            public boolean countCalls() {
                return true;
            }

            @Override
            public boolean recordTime() {
                return true;
            }

            @Override
            public boolean countExceptions() {
                return true;
            }

            @Override
            public double[] percentiles() {
                return new double[]{0.5, 0.95, 0.99};
            }

            @Override
            public String[] tags() {
                return new String[0];
            }
        };
    }

    private Timer getOrCreateTimer(Measured measured, String className, String methodName, String key) {
        return timers.computeIfAbsent(key, k -> {
            String name = measured.name().isEmpty() ? "method.timed" : measured.name();
            Timer.Builder builder = Timer.builder(name)
                    .description(measured.description().isEmpty() ? "Method execution time" : measured.description())
                    .tag("class", className)
                    .tag("method", methodName);

            // Add percentiles
            for (double percentile : measured.percentiles()) {
                builder.publishPercentiles(percentile);
            }

            // Add custom tags
            for (String tag : measured.tags()) {
                String[] parts = tag.split("=", 2);
                if (parts.length == 2) {
                    builder.tag(parts[0], parts[1]);
                }
            }

            return builder.register(meterRegistry);
        });
    }

    private Counter getOrCreateCounter(Measured measured, String className, String methodName, String key) {
        return counters.computeIfAbsent(key + ".calls", k -> {
            String name = measured.name().isEmpty() ? "method.calls" : measured.name() + ".calls";
            Counter.Builder builder = Counter.builder(name)
                    .description("Method invocation count")
                    .tag("class", className)
                    .tag("method", methodName);

            // Add custom tags
            for (String tag : measured.tags()) {
                String[] parts = tag.split("=", 2);
                if (parts.length == 2) {
                    builder.tag(parts[0], parts[1]);
                }
            }

            return builder.register(meterRegistry);
        });
    }

    private Counter getOrCreateExceptionCounter(Measured measured, String className, String methodName, String key) {
        return exceptionCounters.computeIfAbsent(key + ".exceptions", k -> {
            String name = measured.name().isEmpty() ? "method.exceptions" : measured.name() + ".exceptions";
            Counter.Builder builder = Counter.builder(name)
                    .description("Method exception count")
                    .tag("class", className)
                    .tag("method", methodName);

            // Add custom tags
            for (String tag : measured.tags()) {
                String[] parts = tag.split("=", 2);
                if (parts.length == 2) {
                    builder.tag(parts[0], parts[1]);
                }
            }

            return builder.register(meterRegistry);
        });
    }
}
