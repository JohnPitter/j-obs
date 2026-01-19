package io.github.jobs.domain.profiling;

/**
 * Types of profiling supported.
 */
public enum ProfileType {
    /**
     * CPU profiling - samples stack traces to identify hot methods.
     */
    CPU("CPU Profile", "Identifies CPU-intensive methods"),

    /**
     * Memory profiling - captures heap snapshots.
     */
    MEMORY("Memory Profile", "Analyzes heap usage and object allocations"),

    /**
     * Thread dump - captures current thread states.
     */
    THREAD("Thread Dump", "Shows current thread states and stack traces"),

    /**
     * Allocation profiling - tracks object allocations.
     */
    ALLOCATION("Allocation Profile", "Tracks object allocation rates");

    private final String displayName;
    private final String description;

    ProfileType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
