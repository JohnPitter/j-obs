package io.github.jobs.domain.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single span in a distributed trace.
 * A span represents a single operation within a trace.
 */
public final class Span {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String name;
    private final SpanKind kind;
    private final Instant startTime;
    private final Instant endTime;
    private final SpanStatus status;
    private final String statusMessage;
    private final String serviceName;
    private final Map<String, String> attributes;
    private final List<SpanEvent> events;

    private Span(Builder builder) {
        this.traceId = Objects.requireNonNull(builder.traceId, "traceId is required");
        this.spanId = Objects.requireNonNull(builder.spanId, "spanId is required");
        this.parentSpanId = builder.parentSpanId;
        this.name = Objects.requireNonNull(builder.name, "name is required");
        this.kind = builder.kind != null ? builder.kind : SpanKind.INTERNAL;
        this.startTime = Objects.requireNonNull(builder.startTime, "startTime is required");
        this.endTime = builder.endTime;
        this.status = builder.status != null ? builder.status : SpanStatus.UNSET;
        this.statusMessage = builder.statusMessage;
        this.serviceName = builder.serviceName;
        this.attributes = builder.attributes != null ?
            Collections.unmodifiableMap(builder.attributes) : Collections.emptyMap();
        this.events = builder.events != null ?
            Collections.unmodifiableList(builder.events) : Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    public boolean hasParent() {
        return parentSpanId != null && !parentSpanId.isEmpty();
    }

    public boolean isRoot() {
        return !hasParent();
    }

    public String name() {
        return name;
    }

    public SpanKind kind() {
        return kind;
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public boolean isComplete() {
        return endTime != null;
    }

    public Duration duration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }

    public long durationMs() {
        return duration().toMillis();
    }

    public SpanStatus status() {
        return status;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public boolean hasError() {
        return status.isError();
    }

    public String serviceName() {
        return serviceName;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    public String attribute(String key) {
        return attributes.get(key);
    }

    public List<SpanEvent> events() {
        return events;
    }

    /**
     * Returns the HTTP method if this is an HTTP span.
     */
    public String httpMethod() {
        return attributes.getOrDefault("http.method",
               attributes.get("http.request.method"));
    }

    /**
     * Returns the HTTP URL if this is an HTTP span.
     */
    public String httpUrl() {
        return attributes.getOrDefault("http.url",
               attributes.getOrDefault("url.full",
               attributes.get("http.target")));
    }

    /**
     * Returns the HTTP status code if this is an HTTP span.
     */
    public Integer httpStatusCode() {
        String status = attributes.getOrDefault("http.status_code",
                       attributes.get("http.response.status_code"));
        if (status != null) {
            try {
                return Integer.parseInt(status);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the database system if this is a database span.
     */
    public String dbSystem() {
        return attributes.get("db.system");
    }

    /**
     * Returns the database statement if this is a database span.
     */
    public String dbStatement() {
        return attributes.get("db.statement");
    }

    /**
     * Returns the database operation if this is a database span.
     */
    public String dbOperation() {
        return attributes.get("db.operation");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Span span = (Span) o;
        return Objects.equals(traceId, span.traceId) &&
               Objects.equals(spanId, span.spanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId);
    }

    @Override
    public String toString() {
        return "Span{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + durationMs() + "ms" +
                ", status=" + status +
                '}';
    }

    public static final class Builder {
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String name;
        private SpanKind kind;
        private Instant startTime;
        private Instant endTime;
        private SpanStatus status;
        private String statusMessage;
        private String serviceName;
        private Map<String, String> attributes;
        private List<SpanEvent> events;

        private Builder() {}

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(SpanKind kind) {
            this.kind = kind;
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

        public Builder status(SpanStatus status) {
            this.status = status;
            return this;
        }

        public Builder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (this.attributes == null) {
                this.attributes = new java.util.HashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }

        public Builder events(List<SpanEvent> events) {
            this.events = events;
            return this;
        }

        public Span build() {
            return new Span(this);
        }
    }
}
