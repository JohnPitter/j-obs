package io.github.jobs.domain.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Query parameters for filtering traces.
 */
public final class TraceQuery {

    private final String serviceName;
    private final String spanName;
    private final SpanKind spanKind;
    private final SpanStatus status;
    private final Duration minDuration;
    private final Duration maxDuration;
    private final Instant startTime;
    private final Instant endTime;
    private final String traceId;
    private final int limit;
    private final int offset;

    private TraceQuery(Builder builder) {
        this.serviceName = builder.serviceName;
        this.spanName = builder.spanName;
        this.spanKind = builder.spanKind;
        this.status = builder.status;
        this.minDuration = builder.minDuration;
        this.maxDuration = builder.maxDuration;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.traceId = builder.traceId;
        this.limit = builder.limit > 0 ? builder.limit : 100;
        this.offset = Math.max(builder.offset, 0);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TraceQuery all() {
        return builder().build();
    }

    public static TraceQuery byTraceId(String traceId) {
        return builder().traceId(traceId).build();
    }

    public static TraceQuery recent(int limit) {
        return builder().limit(limit).build();
    }

    public static TraceQuery errors() {
        return builder().status(SpanStatus.ERROR).build();
    }

    public static TraceQuery slow(Duration minDuration) {
        return builder().minDuration(minDuration).build();
    }

    public String serviceName() {
        return serviceName;
    }

    public String spanName() {
        return spanName;
    }

    public SpanKind spanKind() {
        return spanKind;
    }

    public SpanStatus status() {
        return status;
    }

    public Duration minDuration() {
        return minDuration;
    }

    public Duration maxDuration() {
        return maxDuration;
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public String traceId() {
        return traceId;
    }

    public int limit() {
        return limit;
    }

    public int offset() {
        return offset;
    }

    public boolean hasFilters() {
        return serviceName != null || spanName != null || spanKind != null ||
               status != null || minDuration != null || maxDuration != null ||
               startTime != null || endTime != null || traceId != null;
    }

    /**
     * Tests if a trace matches this query.
     */
    public boolean matches(Trace trace) {
        if (traceId != null && !traceId.equals(trace.traceId())) {
            return false;
        }
        if (serviceName != null && !serviceName.equals(trace.serviceName())) {
            return false;
        }
        if (status != null && status != trace.status()) {
            return false;
        }
        if (minDuration != null && trace.duration().compareTo(minDuration) < 0) {
            return false;
        }
        if (maxDuration != null && trace.duration().compareTo(maxDuration) > 0) {
            return false;
        }
        if (startTime != null && trace.startTime().isBefore(startTime)) {
            return false;
        }
        if (endTime != null && trace.startTime().isAfter(endTime)) {
            return false;
        }
        if (spanName != null) {
            boolean hasMatchingSpan = trace.spans().stream()
                    .anyMatch(s -> s.name().contains(spanName));
            if (!hasMatchingSpan) return false;
        }
        if (spanKind != null) {
            boolean hasMatchingKind = trace.spans().stream()
                    .anyMatch(s -> s.kind() == spanKind);
            if (!hasMatchingKind) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TraceQuery{" +
                "serviceName='" + serviceName + '\'' +
                ", status=" + status +
                ", minDuration=" + minDuration +
                ", limit=" + limit +
                '}';
    }

    public static final class Builder {
        private String serviceName;
        private String spanName;
        private SpanKind spanKind;
        private SpanStatus status;
        private Duration minDuration;
        private Duration maxDuration;
        private Instant startTime;
        private Instant endTime;
        private String traceId;
        private int limit = 100;
        private int offset = 0;

        private Builder() {}

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder spanName(String spanName) {
            this.spanName = spanName;
            return this;
        }

        public Builder spanKind(SpanKind spanKind) {
            this.spanKind = spanKind;
            return this;
        }

        public Builder status(SpanStatus status) {
            this.status = status;
            return this;
        }

        public Builder minDuration(Duration minDuration) {
            this.minDuration = minDuration;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
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

        public Builder traceId(String traceId) {
            this.traceId = traceId;
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

        public TraceQuery build() {
            return new TraceQuery(this);
        }
    }
}
