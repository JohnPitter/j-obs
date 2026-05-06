package io.github.jobs.spring.log;

import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.spring.security.LogSanitizer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Log4j2 appender that captures log events and stores them in the LogRepository.
 * Mirror of JObsLogAppender for Log4j2 support.
 */
public class JObsLog4j2Appender extends AbstractAppender {

    private volatile LogRepository logRepository;
    private volatile LogEntryFactory logEntryFactory;
    private volatile LogSanitizer logSanitizer;

    public JObsLog4j2Appender(String name) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
    }

    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void setLogEntryFactory(LogEntryFactory logEntryFactory) {
        this.logEntryFactory = logEntryFactory;
    }

    public void setLogSanitizer(LogSanitizer logSanitizer) {
        this.logSanitizer = logSanitizer;
    }

    @Override
    public void append(LogEvent event) {
        LogRepository repo = this.logRepository;
        if (repo == null) {
            return;
        }

        // Skip logs from J-Obs library itself to avoid circular logging
        String loggerName = event.getLoggerName();
        if (loggerName.startsWith("io.github.jobs.spring") ||
            loggerName.startsWith("io.github.jobs.application") ||
            loggerName.startsWith("io.github.jobs.domain") ||
            loggerName.startsWith("io.github.jobs.infrastructure")) {
            return;
        }

        LogEntryFactory factory = this.logEntryFactory;
        if (factory == null) {
            synchronized (this) {
                factory = this.logEntryFactory;
                if (factory == null) {
                    factory = new LogEntryFactory();
                    this.logEntryFactory = factory;
                }
            }
        }

        LogSanitizer sanitizer = this.logSanitizer;
        if (sanitizer == null) {
            synchronized (this) {
                sanitizer = this.logSanitizer;
                if (sanitizer == null) {
                    sanitizer = new LogSanitizer();
                    this.logSanitizer = sanitizer;
                }
            }
        }

        String message = event.getMessage() != null ? event.getMessage().getFormattedMessage() : "";
        String sanitizedMessage = sanitizer.sanitize(message);
        String sanitizedStackTrace = sanitizer.sanitizeStackTrace(formatThrowable(event));
        Map<String, String> mdcMap = event.getContextData() != null
                ? event.getContextData().toMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())))
                : Map.of();
        Map<String, String> sanitizedMdc = sanitizer.sanitizeMdc(mdcMap);

        LogEntry entry = factory.create(
                Instant.ofEpochMilli(event.getTimeMillis()),
                convertLevel(event.getLevel()),
                loggerName,
                sanitizedMessage,
                event.getThreadName(),
                extractTraceId(mdcMap),
                extractSpanId(mdcMap),
                sanitizedStackTrace,
                sanitizedMdc != null ? Map.copyOf(sanitizedMdc) : Map.of()
        );

        repo.add(entry);
    }

    private LogLevel convertLevel(Level level) {
        if (level == null) {
            return LogLevel.INFO;
        }
        if (level.isMoreSpecificThan(Level.ERROR)) return LogLevel.ERROR;
        if (level.isMoreSpecificThan(Level.WARN))  return LogLevel.WARN;
        if (level.isMoreSpecificThan(Level.INFO))  return LogLevel.INFO;
        if (level.isMoreSpecificThan(Level.DEBUG)) return LogLevel.DEBUG;
        return LogLevel.TRACE;
    }

    private String extractTraceId(Map<String, String> mdc) {
        String traceId = mdc.get("traceId");
        if (traceId == null) traceId = mdc.get("trace_id");
        if (traceId == null) traceId = mdc.get("X-B3-TraceId");
        if (traceId == null) {
            SpanContext ctx = getOtelSpanContext();
            if (ctx != null) traceId = ctx.getTraceId();
        }
        return traceId;
    }

    private String extractSpanId(Map<String, String> mdc) {
        String spanId = mdc.get("spanId");
        if (spanId == null) spanId = mdc.get("span_id");
        if (spanId == null) spanId = mdc.get("X-B3-SpanId");
        if (spanId == null) {
            SpanContext ctx = getOtelSpanContext();
            if (ctx != null) spanId = ctx.getSpanId();
        }
        return spanId;
    }

    private SpanContext getOtelSpanContext() {
        try {
            SpanContext ctx = Span.current().getSpanContext();
            return ctx.isValid() ? ctx : null;
        } catch (NoClassDefFoundError | Exception e) {
            return null;
        }
    }

    private String formatThrowable(LogEvent event) {
        if (event.getThrown() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Throwable t = event.getThrown();
        sb.append(t.toString());
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("\n\tat ").append(el);
        }
        return sb.toString();
    }
}
