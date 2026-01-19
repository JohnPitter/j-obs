package io.github.jobs.domain.health;

/**
 * Health status levels for components and overall system health.
 */
public enum HealthStatus {
    /**
     * Component is functioning normally.
     */
    UP("Up", "text-green-600 dark:text-green-400", "bg-green-100 dark:bg-green-900", 0),

    /**
     * Component is functioning but with degraded performance.
     */
    DEGRADED("Degraded", "text-yellow-600 dark:text-yellow-400", "bg-yellow-100 dark:bg-yellow-900", 1),

    /**
     * Component is not functioning.
     */
    DOWN("Down", "text-red-600 dark:text-red-400", "bg-red-100 dark:bg-red-900", 4),

    /**
     * Component has been taken out of service.
     */
    OUT_OF_SERVICE("Out of Service", "text-orange-600 dark:text-orange-400", "bg-orange-100 dark:bg-orange-900", 3),

    /**
     * Component status is unknown.
     */
    UNKNOWN("Unknown", "text-gray-600 dark:text-gray-400", "bg-gray-100 dark:bg-gray-700", 2);

    private final String displayName;
    private final String textCssClass;
    private final String bgCssClass;
    private final int severity;

    HealthStatus(String displayName, String textCssClass, String bgCssClass, int severity) {
        this.displayName = displayName;
        this.textCssClass = textCssClass;
        this.bgCssClass = bgCssClass;
        this.severity = severity;
    }

    public String displayName() {
        return displayName;
    }

    public String textCssClass() {
        return textCssClass;
    }

    public String bgCssClass() {
        return bgCssClass;
    }

    public int severity() {
        return severity;
    }

    /**
     * Checks if this status indicates the component is healthy.
     */
    public boolean isHealthy() {
        return this == UP;
    }

    /**
     * Checks if this status indicates a problem.
     */
    public boolean hasProblems() {
        return this == DOWN || this == OUT_OF_SERVICE;
    }

    /**
     * Gets an emoji representation of the status.
     */
    public String emoji() {
        return switch (this) {
            case UP -> "✅";
            case DEGRADED -> "⚠️";
            case DOWN -> "❌";
            case OUT_OF_SERVICE -> "🔶";
            case UNKNOWN -> "❓";
        };
    }

    /**
     * Combines two statuses, returning the worse one.
     */
    public HealthStatus combine(HealthStatus other) {
        if (other == null) {
            return this;
        }
        return this.severity >= other.severity ? this : other;
    }

    /**
     * Parses a status from a string (case-insensitive).
     */
    public static HealthStatus fromString(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
