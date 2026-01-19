package io.github.jobs.domain.trace;

/**
 * Represents the status of a span execution.
 * Based on OpenTelemetry StatusCode specification.
 */
public enum SpanStatus {
    /**
     * The default status, indicating the operation completed without error.
     */
    UNSET,

    /**
     * The operation completed successfully.
     */
    OK,

    /**
     * The operation contained an error.
     */
    ERROR;

    /**
     * Returns a user-friendly display name.
     */
    public String displayName() {
        return switch (this) {
            case UNSET -> "Success";
            case OK -> "OK";
            case ERROR -> "Error";
        };
    }

    /**
     * Returns the CSS class for styling.
     */
    public String cssClass() {
        return switch (this) {
            case UNSET, OK -> "text-green-500";
            case ERROR -> "text-red-500";
        };
    }

    /**
     * Returns the icon name for the status.
     */
    public String icon() {
        return switch (this) {
            case UNSET, OK -> "check-circle";
            case ERROR -> "x-circle";
        };
    }

    /**
     * Returns true if this status indicates an error.
     */
    public boolean isError() {
        return this == ERROR;
    }
}
