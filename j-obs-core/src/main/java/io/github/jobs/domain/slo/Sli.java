package io.github.jobs.domain.slo;

import java.util.Objects;

/**
 * Service Level Indicator (SLI) configuration.
 * An SLI is a quantitative measure of some aspect of the level of service being provided.
 */
public record Sli(
        SliType type,
        String metric,
        String goodCondition,
        String totalCondition,
        Double threshold,
        Integer percentile
) {
    public Sli {
        Objects.requireNonNull(type, "SLI type cannot be null");
        Objects.requireNonNull(metric, "SLI metric cannot be null");
    }

    /**
     * Creates an availability SLI.
     *
     * @param metric         the metric name
     * @param goodCondition  condition for good events (e.g., "status < 500")
     * @param totalCondition condition for total events (e.g., "status >= 0")
     * @return the availability SLI
     */
    public static Sli availability(String metric, String goodCondition, String totalCondition) {
        return new Sli(SliType.AVAILABILITY, metric, goodCondition, totalCondition, null, null);
    }

    /**
     * Creates a latency SLI.
     *
     * @param metric     the metric name
     * @param threshold  latency threshold in milliseconds
     * @param percentile percentile to measure (e.g., 99 for p99)
     * @return the latency SLI
     */
    public static Sli latency(String metric, double threshold, int percentile) {
        return new Sli(SliType.LATENCY, metric, null, null, threshold, percentile);
    }

    /**
     * Creates an error rate SLI.
     *
     * @param metric         the metric name
     * @param goodCondition  condition for good events
     * @param totalCondition condition for total events
     * @return the error rate SLI
     */
    public static Sli errorRate(String metric, String goodCondition, String totalCondition) {
        return new Sli(SliType.ERROR_RATE, metric, goodCondition, totalCondition, null, null);
    }

    /**
     * Creates a throughput SLI.
     *
     * @param metric    the metric name
     * @param threshold minimum throughput threshold
     * @return the throughput SLI
     */
    public static Sli throughput(String metric, double threshold) {
        return new Sli(SliType.THROUGHPUT, metric, null, null, threshold, null);
    }

    /**
     * Checks if this SLI is ratio-based (availability, error rate).
     *
     * @return true if ratio-based
     */
    public boolean isRatioBased() {
        return type == SliType.AVAILABILITY || type == SliType.ERROR_RATE;
    }

    /**
     * Checks if this SLI is threshold-based (latency, throughput).
     *
     * @return true if threshold-based
     */
    public boolean isThresholdBased() {
        return type == SliType.LATENCY || type == SliType.THROUGHPUT;
    }
}
