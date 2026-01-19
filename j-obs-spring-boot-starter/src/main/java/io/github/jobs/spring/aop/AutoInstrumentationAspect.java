package io.github.jobs.spring.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
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
 * Aspect that automatically instruments all Spring stereotype components
 * (@Service, @Repository, @Controller, @RestController, @Component)
 * without requiring any annotations.
 *
 * <p>This provides zero-configuration observability for Spring applications.</p>
 */
@Aspect
public class AutoInstrumentationAspect {

    private static final Logger log = LoggerFactory.getLogger(AutoInstrumentationAspect.class);

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public AutoInstrumentationAspect(Tracer tracer, MeterRegistry meterRegistry) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut for @Service classes.
     */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceClass() {}

    /**
     * Pointcut for @Repository classes.
     */
    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryClass() {}

    /**
     * Pointcut for @Component classes (excluding controllers).
     */
    @Pointcut("@within(org.springframework.stereotype.Component) && " +
              "!@within(org.springframework.stereotype.Controller) && " +
              "!@within(org.springframework.web.bind.annotation.RestController)")
    public void componentClass() {}

    /**
     * Exclude already traced methods.
     */
    @Pointcut("!@annotation(io.github.jobs.annotation.Traced) && " +
              "!@annotation(io.github.jobs.annotation.Measured) && " +
              "!@annotation(io.github.jobs.annotation.Observable) && " +
              "!@within(io.github.jobs.annotation.Traced) && " +
              "!@within(io.github.jobs.annotation.Measured) && " +
              "!@within(io.github.jobs.annotation.Observable)")
    public void notAlreadyInstrumented() {}

    /**
     * Exclude Spring internal methods.
     */
    @Pointcut("!execution(* org.springframework..*(..))")
    public void notSpringInternal() {}

    /**
     * Around advice for automatic instrumentation.
     */
    @Around("(serviceClass() || repositoryClass() || componentClass()) && " +
            "notAlreadyInstrumented() && notSpringInternal() && " +
            "execution(public * *(..))")
    public Object instrumentMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Skip synthetic and bridge methods
        if (method.isSynthetic() || method.isBridge()) {
            return joinPoint.proceed();
        }

        String className = getSimpleClassName(targetClass);
        String methodName = method.getName();
        String spanName = className + "." + methodName;
        String metricKey = spanName;

        // Get or create timer
        Timer timer = getOrCreateTimer(className, methodName, metricKey);

        // Create span
        Span span = tracer.spanBuilder(spanName)
                .setAttribute("code.function", methodName)
                .setAttribute("code.namespace", targetClass.getName())
                .setAttribute("component.type", getComponentType(targetClass))
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Record timing
            return timer.recordCallable(() -> {
                try {
                    Object result = joinPoint.proceed();
                    span.setStatus(StatusCode.OK);
                    return result;
                } catch (Throwable e) {
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR, e.getMessage());
                    incrementExceptionCounter(className, methodName);
                    if (e instanceof Exception) {
                        throw (Exception) e;
                    }
                    throw new RuntimeException(e);
                }
            });
        } finally {
            span.end();
        }
    }

    private String getSimpleClassName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        // Handle CGLIB proxies
        int idx = name.indexOf("$$");
        if (idx > 0) {
            name = name.substring(0, idx);
        }
        return name;
    }

    private String getComponentType(Class<?> targetClass) {
        if (targetClass.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            return "service";
        } else if (targetClass.isAnnotationPresent(org.springframework.stereotype.Repository.class)) {
            return "repository";
        } else {
            return "component";
        }
    }

    private Timer getOrCreateTimer(String className, String methodName, String key) {
        return timers.computeIfAbsent(key, k ->
                Timer.builder("method.execution")
                        .description("Method execution time")
                        .tag("class", className)
                        .tag("method", methodName)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(meterRegistry)
        );
    }

    private void incrementExceptionCounter(String className, String methodName) {
        String key = className + "." + methodName + ".exceptions";
        counters.computeIfAbsent(key, k ->
                Counter.builder("method.exceptions")
                        .description("Method exception count")
                        .tag("class", className)
                        .tag("method", methodName)
                        .register(meterRegistry)
        ).increment();
    }
}
