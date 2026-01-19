package io.github.jobs.domain.log;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single log entry.
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

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public LogLevel level() {
        return level;
    }

    public String loggerName() {
        return loggerName;
    }

    /**
     * Returns the short logger name (last segment after dot).
     */
    public String shortLoggerName() {
        if (loggerName == null) {
            return null;
        }
        int lastDot = loggerName.lastIndexOf('.');
        return lastDot >= 0 ? loggerName.substring(lastDot + 1) : loggerName;
    }

    public String message() {
        return message;
    }

    public String threadName() {
        return threadName;
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    public boolean hasTraceId() {
        return traceId != null && !traceId.isEmpty();
    }

    public String throwable() {
        return throwable;
    }

    public boolean hasThrowable() {
        return throwable != null && !throwable.isEmpty();
    }

    public Map<String, String> mdc() {
        return mdc;
    }

    public boolean hasError() {
        return level.isError() || hasThrowable();
    }

    /**
     * Checks if this log entry matches the given query.
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
