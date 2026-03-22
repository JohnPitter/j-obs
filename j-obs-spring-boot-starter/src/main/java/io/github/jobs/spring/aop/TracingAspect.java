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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect that automatically creates OpenTelemetry spans for methods
 * annotated with @Traced or @Observable, or methods in annotated classes.
 */
@Aspect
public class TracingAspect {

    private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

    private final Tracer tracer;
    private final Map<Method, TracedMethodMetadata> metadataCache = new ConcurrentHashMap<>();

    /**
     * Cached metadata extracted from @Traced / @Observable annotations to avoid
     * repeated reflection on every method invocation.
     */
    private record TracedMethodMetadata(
            Traced annotation,
            String spanName,
            String[] parameterNames
    ) {}

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
     * Wrapped in error boundary so J-Obs instrumentation failures never crash the host application.
     */
    @Around("(tracedMethod() || tracedClass() || observableMethod() || observableClass()) && execution(* *(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Look up cached metadata or compute on first call
        TracedMethodMetadata metadata;
        try {
            metadata = metadataCache.computeIfAbsent(method,
                    m -> buildMetadata(m, targetClass));
        } catch (Exception e) {
            log.debug("Failed to resolve tracing metadata, proceeding without tracing", e);
            return joinPoint.proceed();
        }

        if (metadata == null) {
            return joinPoint.proceed();
        }

        Traced traced = metadata.annotation();

        // Create span using cached span name (error boundary)
        Span span;
        try {
            span = tracer.spanBuilder(metadata.spanName())
                    .setAttribute("code.function", method.getName())
                    .setAttribute("code.namespace", targetClass.getName())
                    .startSpan();

            // Add static attributes (from cached annotation)
            for (String attr : traced.attributes()) {
                String[] parts = attr.split("=", 2);
                if (parts.length == 2) {
                    span.setAttribute(parts[0], parts[1]);
                }
            }

            // Add parameter attributes using cached parameter names
            if (traced.includeParameters()) {
                addParameterAttributes(span, metadata.parameterNames(), joinPoint.getArgs());
            }
        } catch (Exception e) {
            log.debug("Failed to create trace span, proceeding without tracing", e);
            return joinPoint.proceed();
        }

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();

            // Record result (error boundary for span operations only)
            try {
                if (traced.includeResult() && result != null) {
                    span.setAttribute("result", truncate(result.toString(), 1000));
                }
                span.setStatus(StatusCode.OK);
            } catch (Exception e) {
                log.debug("Failed to record span result", e);
            }

            return result;

        } catch (Throwable e) {
            try {
                if (traced.recordException()) {
                    span.recordException(e);
                }
                span.setStatus(StatusCode.ERROR, e.getMessage());
            } catch (Exception spanError) {
                log.debug("Failed to record span exception", spanError);
            }
            throw e;

        } finally {
            try {
                span.end();
            } catch (Exception e) {
                log.debug("Failed to end trace span", e);
            }
        }
    }

    /**
     * Builds and caches method metadata from annotations and reflection.
     * Called once per unique method, then cached for subsequent invocations.
     */
    private TracedMethodMetadata buildMetadata(Method method, Class<?> targetClass) {
        Traced traced = getTracedAnnotation(method, targetClass);
        if (traced == null) {
            return null;
        }

        String spanName = traced.name().isEmpty()
                ? targetClass.getSimpleName() + "." + method.getName()
                : traced.name();

        String[] parameterNames = null;
        if (traced.includeParameters()) {
            Parameter[] parameters = method.getParameters();
            parameterNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                parameterNames[i] = parameters[i].getName();
            }
        }

        return new TracedMethodMetadata(traced, spanName, parameterNames);
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

    private void addParameterAttributes(Span span, String[] parameterNames, Object[] args) {
        if (parameterNames == null) return;
        for (int i = 0; i < parameterNames.length && i < args.length; i++) {
            Object paramValue = args[i];
            if (paramValue != null) {
                span.setAttribute("param." + parameterNames[i], truncate(paramValue.toString(), 500));
            }
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }
}
