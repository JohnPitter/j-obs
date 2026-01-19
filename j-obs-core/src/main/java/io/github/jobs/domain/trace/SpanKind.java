package io.github.jobs.domain.trace;

/**
 * Represents the role of a span in a trace.
 * Based on OpenTelemetry SpanKind specification.
 */
public enum SpanKind {
    /**
     * Default span kind. Indicates that the span represents an internal operation.
     */
    INTERNAL,

    /**
     * Indicates that the span covers server-side handling of a synchronous RPC or HTTP request.
     */
    SERVER,

    /**
     * Indicates that the span describes a request to some remote service.
     */
    CLIENT,

    /**
     * Indicates that the span describes the initiator of an asynchronous request.
     */
    PRODUCER,

    /**
     * Indicates that the span describes a child of an asynchronous producer.
     */
    CONSUMER;

    /**
     * Returns a user-friendly display name.
     */
    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    /**
     * Returns the CSS class for styling.
     */
    public String cssClass() {
        return switch (this) {
            case SERVER -> "bg-blue-500";
            case CLIENT -> "bg-green-500";
            case PRODUCER -> "bg-purple-500";
            case CONSUMER -> "bg-orange-500";
            case INTERNAL -> "bg-gray-500";
        };
    }
}
