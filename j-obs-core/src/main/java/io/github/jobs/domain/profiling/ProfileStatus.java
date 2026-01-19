package io.github.jobs.domain.profiling;

/**
 * Status of a profiling session.
 */
public enum ProfileStatus {
    /**
     * Profiling session is starting.
     */
    STARTING("Starting"),

    /**
     * Profiling session is running.
     */
    RUNNING("Running"),

    /**
     * Profiling session is stopping.
     */
    STOPPING("Stopping"),

    /**
     * Profiling session completed successfully.
     */
    COMPLETED("Completed"),

    /**
     * Profiling session failed.
     */
    FAILED("Failed"),

    /**
     * Profiling session was cancelled.
     */
    CANCELLED("Cancelled");

    private final String displayName;

    ProfileStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == STARTING || this == RUNNING || this == STOPPING;
    }
}
