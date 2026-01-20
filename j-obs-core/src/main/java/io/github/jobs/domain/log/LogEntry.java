package io.github.jobs.domain.log;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single log entry captured by J-Obs.
 * <p>
 * A log entry contains all the information about a logged event including:
 * <ul>
 *   <li>Timestamp when the log was generated</li>
 *   <li>Log level (ERROR, WARN, INFO, DEBUG, TRACE)</li>
 *   <li>Logger name (typically the class name)</li>
 *   <li>Log message</li>
 *   <li>Thread name where the log was generated</li>
 *   <li>Optional trace/span IDs for correlation with distributed traces</li>
 *   <li>Optional exception stack trace</li>
 *   <li>MDC (Mapped Diagnostic Context) key-value pairs</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe. Use {@link #builder()} to create instances.
 *
 * @see LogLevel
 * @see LogQuery
 */
public final class LogEntry {

    private final String id;
    private final Instant timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String message;
    private final String threadName;
    private final String traceId;
    private final String spanId;
    private final String throwable;
    private final Map<String, String> mdc;

    private LogEntry(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.level = builder.level != null ? builder.level : LogLevel.INFO;
        this.loggerName = builder.loggerName;
        this.message = Objects.requireNonNull(builder.message, "message is required");
        this.threadName = builder.threadName;
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.throwable = builder.throwable;
        this.mdc = builder.mdc != null ? Map.copyOf(builder.mdc) : Map.of();
    }

    /**
     * Creates a new builder for constructing LogEntry instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the unique identifier for this log entry.
     *
     * @return the log entry ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns the timestamp when this log entry was created.
     *
     * @return the timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns the log level of this entry.
     *
     * @return the log level
     */
    public LogLevel level() {
        return level;
    }

    /**
     * Returns the full logger name (typically the fully qualified class name).
     *
     * @return the logger name, may be null
     */
    public String loggerName() {
        return loggerName;
    }

    /**
     * Returns the short logger name (last segment after the last dot).
     * <p>
     * For example, "com.example.MyClass" returns "MyClass".
     *
     * @return the short logger name, or the full name if no dots, or null if logger name is null
     */
    public String shortLoggerName() {
        if (loggerName == null) {
            return null;
        }
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

    /**
     * Returns the log message.
     *
     * @return the log message
     */
    public String message() {
        return message;
    }

    /**
     * Returns the name of the thread that generated this log entry.
     *
     * @return the thread name, may be null
     */
    public String threadName() {
        return threadName;
    }

    /**
     * Returns the trace ID for distributed tracing correlation.
     *
     * @return the trace ID, may be null if not in a traced context
     */
    public String traceId() {
        return traceId;
    }

    /**
     * Returns the span ID for distributed tracing correlation.
     *
     * @return the span ID, may be null if not in a traced context
     */
    public String spanId() {
        return spanId;
    }

    /**
     * Checks if this log entry has an associated trace ID.
     *
     * @return true if trace ID is present and non-empty
     */
    public boolean hasTraceId() {
        return traceId != null && !traceId.isEmpty();
    }

    /**
     * Returns the exception stack trace if this log entry represents an error with exception.
     *
     * @return the exception stack trace as string, may be null
     */
    public String throwable() {
        return throwable;
    }

    /**
     * Checks if this log entry has an associated exception.
     *
     * @return true if an exception stack trace is present
     */
    public boolean hasThrowable() {
        return throwable != null && !throwable.isEmpty();
    }

    /**
     * Returns the MDC (Mapped Diagnostic Context) key-value pairs.
     *
     * @return an unmodifiable map of MDC entries, never null
     */
    public Map<String, String> mdc() {
        return mdc;
    }

    /**
     * Checks if this log entry represents an error condition.
     * <p>
     * A log entry is considered an error if its level is ERROR or if it has an exception.
     *
     * @return true if this is an error log entry
     */
    public boolean hasError() {
        return level.isError() || hasThrowable();
    }

    /**
     * Checks if this log entry matches the given query criteria.
     *
     * @param query the query to match against
     * @return true if this entry matches all query criteria
     */
    public boolean matches(LogQuery query) {
        if (query.minLevel() != null && !level.isAtLeast(query.minLevel())) {
            return false;
        }
        if (query.loggerName() != null && (loggerName == null || !loggerName.contains(query.loggerName()))) {
            return false;
        }
        if (query.messagePattern() != null && !message.toLowerCase().contains(query.messagePattern().toLowerCase())) {
            return false;
        }
        if (query.traceId() != null && !query.traceId().equals(traceId)) {
            return false;
        }
        if (query.threadName() != null && !query.threadName().equals(threadName)) {
            return false;
        }
        if (query.startTime() != null && timestamp.isBefore(query.startTime())) {
            return false;
        }
        if (query.endTime() != null && timestamp.isAfter(query.endTime())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(id, logEntry.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", level=" + level +
                ", logger='" + shortLoggerName() + '\'' +
                ", message='" + (message.length() > 50 ? message.substring(0, 50) + "..." : message) + '\'' +
                '}';
    }

    public static final class Builder {
        private String id;
        private Instant timestamp;
        private LogLevel level;
        private String loggerName;
        private String message;
        private String threadName;
        private String traceId;
        private String spanId;
        private String throwable;
        private Map<String, String> mdc;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(LogLevel level) {
            this.level = level;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder throwable(String throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder mdc(Map<String, String> mdc) {
            this.mdc = mdc;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
