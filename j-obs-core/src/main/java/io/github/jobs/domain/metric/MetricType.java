package io.github.jobs.domain.metric;

/**
 * Types of metrics supported by J-Obs.
 */
public enum MetricType {
    /**
     * A counter that only increments (e.g., request count).
     */
    COUNTER("Counter", "Monotonically increasing value"),

    /**
     * A gauge that can go up or down (e.g., current memory usage).
     */
    GAUGE("Gauge", "Point-in-time value"),

    /**
     * A timer that measures duration (e.g., request latency).
     */
    TIMER("Timer", "Duration measurement with count and percentiles"),

    /**
     * A distribution summary (e.g., request size distribution).
     */
    DISTRIBUTION_SUMMARY("Distribution", "Value distribution with percentiles"),

    /**
     * A long task timer for tracking in-progress operations.
     */
    LONG_TASK_TIMER("Long Task Timer", "Duration of ongoing tasks"),

    /**
     * Unknown metric type.
     */
    UNKNOWN("Unknown", "Unknown metric type");

    private final String displayName;
    private final String description;

    MetricType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /**
     * Determines if this metric type represents a rate-based metric.
     */
    public boolean isRate() {
        return this == COUNTER;
    }

    /**
     * Determines if this metric type has percentiles.
     */
    public boolean hasPercentiles() {
        return this == TIMER || this == DISTRIBUTION_SUMMARY;
    }

    /**
     * Determines if this metric type has histogram data.
     */
    public boolean hasHistogram() {
        return this == TIMER || this == DISTRIBUTION_SUMMARY;
    }

    public static MetricType fromMicrometerType(String meterType) {
        if (meterType == null) {
            return UNKNOWN;
        }
        return switch (meterType.toUpperCase()) {
            case "COUNTER" -> COUNTER;
            case "GAUGE" -> GAUGE;
            case "TIMER" -> TIMER;
            case "DISTRIBUTION_SUMMARY" -> DISTRIBUTION_SUMMARY;
            case "LONG_TASK_TIMER" -> LONG_TASK_TIMER;
            default -> UNKNOWN;
        };
    }
}
