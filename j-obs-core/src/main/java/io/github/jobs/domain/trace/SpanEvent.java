package io.github.jobs.domain.trace;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an event that occurred during a span's lifetime.
 * Events are used to record notable occurrences within a span.
 */
public final class SpanEvent {

    private final String name;
    private final Instant timestamp;
    private final Map<String, String> attributes;

    private SpanEvent(String name, Instant timestamp, Map<String, String> attributes) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
        this.attributes = attributes != null ?
            Collections.unmodifiableMap(attributes) : Collections.emptyMap();
    }

    public static SpanEvent of(String name, Instant timestamp) {
        return new SpanEvent(name, timestamp, null);
    }

    public static SpanEvent of(String name, Instant timestamp, Map<String, String> attributes) {
        return new SpanEvent(name, timestamp, attributes);
    }

    public String name() {
        return name;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Returns true if this event represents an exception.
     */
    public boolean isException() {
        return "exception".equals(name) || attributes.containsKey("exception.type");
    }

    /**
     * Returns the exception type if this is an exception event.
     */
    public String exceptionType() {
        return attributes.get("exception.type");
    }

    /**
     * Returns the exception message if this is an exception event.
     */
    public String exceptionMessage() {
        return attributes.get("exception.message");
    }

    /**
     * Returns the exception stacktrace if this is an exception event.
     */
    public String exceptionStacktrace() {
        return attributes.get("exception.stacktrace");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanEvent spanEvent = (SpanEvent) o;
        return Objects.equals(name, spanEvent.name) &&
               Objects.equals(timestamp, spanEvent.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, timestamp);
    }

    @Override
    public String toString() {
        return "SpanEvent{" +
                "name='" + name + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
