package io.github.jobs.spring.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filter that automatically creates spans and metrics for all HTTP requests.
 */
public class HttpTracingFilter extends OncePerRequestFilter {

    private static final int MAX_TIMER_CACHE_SIZE = 1000;

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers;
    private Timer fallbackTimer;

    public HttpTracingFilter(Tracer tracer, MeterRegistry meterRegistry) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
        // LRU cache with bounded size
        this.timers = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Timer> eldest) {
                return size() > MAX_TIMER_CACHE_SIZE;
            }
        };
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip static resources and actuator endpoints
        String uri = request.getRequestURI();
        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String spanName = method + " " + normalizeUri(uri);

        // Create span
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", method)
                .setAttribute("http.url", request.getRequestURL().toString())
                .setAttribute("http.target", uri)
                .setAttribute("http.scheme", request.getScheme())
                .setAttribute("http.host", request.getServerName())
                .setAttribute("http.user_agent", request.getHeader("User-Agent"))
                .setAttribute("net.peer.ip", request.getRemoteAddr())
                .startSpan();

        // Add trace ID to MDC for log correlation
        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        // Get or create timer for this endpoint
        Timer timer = getOrCreateTimer(method, normalizeUri(uri));
        Timer.Sample sample = Timer.start(meterRegistry);

        try (Scope scope = span.makeCurrent()) {
            filterChain.doFilter(request, response);

            int status = response.getStatus();
            span.setAttribute("http.status_code", status);

            if (status >= 400) {
                span.setStatus(StatusCode.ERROR, "HTTP " + status);
            } else {
                span.setStatus(StatusCode.OK);
            }

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;

        } finally {
            span.end();
            sample.stop(timer);
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    private boolean shouldSkip(String uri) {
        return uri.startsWith("/actuator") ||
               uri.startsWith("/j-obs") ||
               uri.matches(".*\\.(css|js|ico|png|jpg|jpeg|gif|svg|woff|woff2|ttf|eot)$");
    }

    private String normalizeUri(String uri) {
        // Replace path parameters with placeholders
        // e.g., /api/orders/123 -> /api/orders/{id}
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{uuid}");
    }

    private Timer getOrCreateTimer(String method, String uri) {
        String key = method + ":" + uri;
        synchronized (timers) {
            Timer timer = timers.get(key);
            if (timer != null) {
                return timer;
            }
            // Create new timer
            // Use distinct metric name to avoid conflict with Spring Boot's http.server.requests
            // which has different tag keys (error, exception, method, outcome, status, uri)
            timer = Timer.builder("jobs.http.requests")
                    .description("HTTP server request time tracked by J-Obs")
                    .tag("method", method)
                    .tag("uri", uri)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
            timers.put(key, timer);
            return timer;
        }
    }

    private Timer getFallbackTimer() {
        if (fallbackTimer == null) {
            synchronized (this) {
                if (fallbackTimer == null) {
                    fallbackTimer = Timer.builder("jobs.http.requests")
                            .description("HTTP server request time tracked by J-Obs")
                            .tag("method", "UNKNOWN")
                            .tag("uri", "/other")
                            .publishPercentiles(0.5, 0.95, 0.99)
                            .register(meterRegistry);
                }
            }
        }
        return fallbackTimer;
    }
}
