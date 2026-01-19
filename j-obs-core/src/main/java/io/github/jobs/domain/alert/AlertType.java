package io.github.jobs.domain.alert;

/**
 * Types of alerts that can be configured.
 */
public enum AlertType {
    /**
     * Alert based on log patterns or error rates.
     */
    LOG("Log Alert", "Trigger based on log level, pattern, or error count"),

    /**
     * Alert based on trace metrics.
     */
    TRACE("Trace Alert", "Trigger based on trace duration, error rate, or span count"),

    /**
     * Alert based on health check status.
     */
    HEALTH("Health Alert", "Trigger when a component becomes unhealthy"),

    /**
     * Alert based on metric thresholds.
     */
    METRIC("Metric Alert", "Trigger when a metric crosses a threshold");

    private final String displayName;
    private final String description;

    AlertType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String icon() {
        return switch (this) {
            case LOG -> "📝";
            case TRACE -> "🔗";
            case HEALTH -> "💚";
            case METRIC -> "📊";
        };
    }
}
