package io.github.jobs.spring.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.time.Instant;
import java.util.Map;

/**
 * Logback appender that captures log events and stores them in the LogRepository.
 * <p>
 * Thread-safe: logRepository is volatile to ensure visibility when set from Spring context.
 * <p>
 * Performance optimizations:
 * <ul>
 *   <li>Uses LogEntryFactory with pooled builders to reduce GC pressure</li>
 *   <li>Fast ID generation using atomic counter instead of UUID</li>
 *   <li>Early return for J-Obs internal logs to avoid circular logging</li>
 * </ul>
 */
public class JObsLogAppender extends AppenderBase<ILoggingEvent> {

    private volatile LogRepository logRepository;
    private final LogEntryFactory logEntryFactory = new LogEntryFactory();

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Local reference to avoid null check race condition
        LogRepository repo = this.logRepository;
        if (repo == null) {
            return;
        }

        // Skip logs from J-Obs library itself to avoid circular logging
        // Allow sample application logs (io.github.jobs.sample) and user applications
        String loggerName = event.getLoggerName();
        if (loggerName.startsWith("io.github.jobs.spring") ||
            loggerName.startsWith("io.github.jobs.application") ||
            loggerName.startsWith("io.github.jobs.domain") ||
            loggerName.startsWith("io.github.jobs.infrastructure")) {
            return;
        }

        // Use factory with pooled builders for better performance
        LogEntry entry = logEntryFactory.create(
                Instant.ofEpochMilli(event.getTimeStamp()),
                convertLevel(event.getLevel()),
                event.getLoggerName(),
                event.getFormattedMessage(),
                event.getThreadName(),
                extractTraceId(event),
                extractSpanId(event),
                formatThrowable(event.getThrowableProxy()),
                Map.copyOf(event.getMDCPropertyMap())
        );

        repo.add(entry);
    }

    private LogLevel convertLevel(Level level) {
        if (level == null) {
            return LogLevel.INFO;
        }
        return switch (level.toInt()) {
            case Level.ERROR_INT -> LogLevel.ERROR;
            case Level.WARN_INT -> LogLevel.WARN;
            case Level.INFO_INT -> LogLevel.INFO;
            case Level.DEBUG_INT -> LogLevel.DEBUG;
            case Level.TRACE_INT -> LogLevel.TRACE;
            default -> LogLevel.INFO;
        };
    }

    private String extractTraceId(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        // Try common trace ID keys from MDC
        String traceId = mdc.get("traceId");
        if (traceId == null) {
            traceId = mdc.get("trace_id");
        }
        if (traceId == null) {
            traceId = mdc.get("X-B3-TraceId");
        }
        // If not in MDC, try to get from OpenTelemetry current span
        if (traceId == null) {
            traceId = extractTraceIdFromOtel();
        }
        return traceId;
    }

    private String extractSpanId(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        // Try common span ID keys from MDC
        String spanId = mdc.get("spanId");
        if (spanId == null) {
            spanId = mdc.get("span_id");
        }
        if (spanId == null) {
            spanId = mdc.get("X-B3-SpanId");
        }
        // If not in MDC, try to get from OpenTelemetry current span
        if (spanId == null) {
            spanId = extractSpanIdFromOtel();
        }
        return spanId;
    }

    private String extractTraceIdFromOtel() {
        try {
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                return spanContext.getTraceId();
            }
        } catch (NoClassDefFoundError | Exception e) {
            // OpenTelemetry not available or error, ignore
        }
        return null;
    }

    private String extractSpanIdFromOtel() {
        try {
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                return spanContext.getSpanId();
            }
        } catch (NoClassDefFoundError | Exception e) {
            // OpenTelemetry not available or error, ignore
        }
        return null;
    }

    private String formatThrowable(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        return ThrowableProxyUtil.asString(throwableProxy);
    }
}
