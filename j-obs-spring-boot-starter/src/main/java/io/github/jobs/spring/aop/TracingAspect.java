package io.github.jobs.spring.aop;

import io.github.jobs.annotation.Observable;
import io.github.jobs.annotation.Traced;
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
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Aspect that automatically creates OpenTelemetry spans for methods
 * annotated with @Traced or @Observable, or methods in annotated classes.
 */
@Aspect
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    private final Tracer tracer;

    public TracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Pointcut for methods annotated with @Traced.
     */
    @Pointcut("@annotation(io.github.jobs.annotation.Traced)")
    public void tracedMethod() {}

    /**
     * Pointcut for methods in classes annotated with @Traced.
     */
    @Pointcut("@within(io.github.jobs.annotation.Traced)")
    public void tracedClass() {}

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
     * Around advice for traced methods.
     */
    @Around("(tracedMethod() || tracedClass() || observableMethod() || observableClass()) && execution(* *(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Get annotation configuration
        Traced traced = getTracedAnnotation(method, targetClass);
        if (traced == null) {
            return joinPoint.proceed();
        }

        // Determine span name
        String spanName = traced.name().isEmpty()
                ? targetClass.getSimpleName() + "." + method.getName()
                : traced.name();

        // Create span
        Span span = tracer.spanBuilder(spanName)
                .setAttribute("code.function", method.getName())
                .setAttribute("code.namespace", targetClass.getName())
                .startSpan();

        // Add static attributes
        for (String attr : traced.attributes()) {
            String[] parts = attr.split("=", 2);
            if (parts.length == 2) {
                span.setAttribute(parts[0], parts[1]);
            }
        }

        // Add parameter attributes if enabled
        if (traced.includeParameters()) {
            addParameterAttributes(span, method, joinPoint.getArgs());
        }

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();

            // Add result attribute if enabled
            if (traced.includeResult() && result != null) {
                span.setAttribute("result", truncate(result.toString(), 1000));
            }

            span.setStatus(StatusCode.OK);
            return result;

        } catch (Throwable e) {
            if (traced.recordException()) {
                span.recordException(e);
            }
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;

        } finally {
            span.end();
        }
    }

    private Traced getTracedAnnotation(Method method, Class<?> targetClass) {
        // Method-level annotation takes precedence
        Traced methodAnnotation = method.getAnnotation(Traced.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // Check for @Observable on method
        Observable observableMethod = method.getAnnotation(Observable.class);
        if (observableMethod != null) {
            return createTracedFromObservable(observableMethod);
        }

        // Class-level annotation
        Traced classAnnotation = targetClass.getAnnotation(Traced.class);
        if (classAnnotation != null) {
            return classAnnotation;
        }

        // Check for @Observable on class
        Observable observableClass = targetClass.getAnnotation(Observable.class);
        if (observableClass != null) {
            return createTracedFromObservable(observableClass);
        }

        return null;
    }

    private Traced createTracedFromObservable(Observable observable) {
        // Create a synthetic @Traced annotation from @Observable
        return new Traced() {
            @Override
            public Class<Traced> annotationType() {
                return Traced.class;
            }

            @Override
            public String name() {
                return observable.value();
            }

            @Override
            public boolean recordException() {
                return observable.recordException();
            }

            @Override
            public boolean includeParameters() {
                return observable.includeParameters();
            }

            @Override
            public boolean includeResult() {
                return false;
            }

            @Override
            public String[] attributes() {
                return new String[0];
            }
        };
    }

    private void addParameterAttributes(Span span, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            String paramName = parameters[i].getName();
            Object paramValue = args[i];
            if (paramValue != null) {
                span.setAttribute("param." + paramName, truncate(paramValue.toString(), 500));
            }
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}
