package io.github.jobs.domain.log;

import java.time.Instant;

/**
 * Query parameters for filtering logs.
 */
public final class LogQuery {

    private final LogLevel minLevel;
    private final String loggerName;
    private final String messagePattern;
    private final String traceId;
    private final String threadName;
    private final Instant startTime;
    private final Instant endTime;
    private final int limit;
    private final int offset;

    private LogQuery(Builder builder) {
        this.minLevel = builder.minLevel;
        this.loggerName = builder.loggerName;
        this.messagePattern = builder.messagePattern;
        this.traceId = builder.traceId;
        this.threadName = builder.threadName;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.limit = builder.limit > 0 ? builder.limit : 100;
        this.offset = Math.max(builder.offset, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LogQuery all() {
        return builder().build();
    }

    public static LogQuery recent(int limit) {
        return builder().limit(limit).build();
    }

    public static LogQuery errors() {
        return builder().minLevel(LogLevel.ERROR).build();
    }

    public static LogQuery warnings() {
        return builder().minLevel(LogLevel.WARN).build();
    }

    public static LogQuery byTraceId(String traceId) {
        return builder().traceId(traceId).build();
    }

    public LogLevel minLevel() {
        return minLevel;
    }

    public String loggerName() {
        return loggerName;
    }

    public String messagePattern() {
        return messagePattern;
    }

    public String traceId() {
        return traceId;
    }

    public String threadName() {
        return threadName;
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public int limit() {
        return limit;
    }

    public int offset() {
        return offset;
    }

    public boolean hasFilters() {
        return minLevel != null || loggerName != null || messagePattern != null ||
               traceId != null || threadName != null || startTime != null || endTime != null;
    }

    /**
     * Tests if a log entry matches this query.
     */
    public boolean matches(LogEntry entry) {
        return entry.matches(this);
    }

    @Override
    public String toString() {
        return "LogQuery{" +
                "minLevel=" + minLevel +
                ", loggerName='" + loggerName + '\'' +
                ", messagePattern='" + messagePattern + '\'' +
                ", limit=" + limit +
                '}';
    }

    public static final class Builder {
        private LogLevel minLevel;
        private String loggerName;
        private String messagePattern;
        private String traceId;
        private String threadName;
        private Instant startTime;
        private Instant endTime;
        private int limit = 100;
        private int offset = 0;

        private Builder() {}

        public Builder minLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
            return this;
        }

        public Builder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        public Builder messagePattern(String messagePattern) {
            this.messagePattern = messagePattern;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public LogQuery build() {
            return new LogQuery(this);
        }
    }
}
