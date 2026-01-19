package io.github.jobs.domain.slo;

/**
 * Types of Service Level Indicators (SLIs).
 */
public enum SliType {
    /**
     * Availability - percentage of successful requests.
     */
    AVAILABILITY("Availability", "Percentage of successful requests", "%"),

    /**
     * Latency - response time percentile.
     */
    LATENCY("Latency", "Response time at specified percentile", "ms"),

    /**
     * Error rate - percentage of failed requests.
     */
    ERROR_RATE("Error Rate", "Percentage of failed requests", "%"),

    /**
     * Throughput - requests per second.
     */
    THROUGHPUT("Throughput", "Requests processed per second", "req/s");

    private final String displayName;
    private final String description;
    private final String unit;

    SliType(String displayName, String description, String unit) {
        this.displayName = displayName;
        this.description = description;
        this.unit = unit;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String unit() {
        return unit;
    }
}
