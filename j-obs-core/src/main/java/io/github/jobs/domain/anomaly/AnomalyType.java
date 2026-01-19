package io.github.jobs.domain.anomaly;

/**
 * Types of anomalies that can be detected.
 */
public enum AnomalyType {

    LATENCY_SPIKE("Latency Spike", "critical", "text-red-600", "bg-red-100"),
    ERROR_RATE_SPIKE("Error Rate Spike", "critical", "text-red-600", "bg-red-100"),
    TRAFFIC_SPIKE("Traffic Spike", "warning", "text-amber-600", "bg-amber-100"),
    TRAFFIC_DROP("Traffic Drop", "warning", "text-amber-600", "bg-amber-100"),
    SLOW_DEPENDENCY("Slow Dependency", "warning", "text-amber-600", "bg-amber-100"),
    MEMORY_SPIKE("Memory Spike", "warning", "text-amber-600", "bg-amber-100"),
    CPU_SPIKE("CPU Spike", "warning", "text-amber-600", "bg-amber-100");

    private final String displayName;
    private final String severity;
    private final String cssClass;
    private final String bgCssClass;

    AnomalyType(String displayName, String severity, String cssClass, String bgCssClass) {
        this.displayName = displayName;
        this.severity = severity;
        this.cssClass = cssClass;
        this.bgCssClass = bgCssClass;
    }

    public String displayName() {
        return displayName;
    }

    public String severity() {
        return severity;
    }

    public String cssClass() {
        return cssClass;
    }

    public String bgCssClass() {
        return bgCssClass;
    }

    public boolean isCritical() {
        return "critical".equals(severity);
    }

    public boolean isWarning() {
        return "warning".equals(severity);
    }

    /**
     * Returns the appropriate emoji for this anomaly type.
     */
    public String emoji() {
        return isCritical() ? "\uD83D\uDD34" : "\uD83D\uDFE1"; // Red or Yellow circle
    }

    /**
     * Returns description of what this anomaly type means.
     */
    public String description() {
        return switch (this) {
            case LATENCY_SPIKE -> "Response times have increased significantly above the baseline";
            case ERROR_RATE_SPIKE -> "Error rate has increased significantly above normal levels";
            case TRAFFIC_SPIKE -> "Request volume has increased significantly above normal levels";
            case TRAFFIC_DROP -> "Request volume has dropped significantly below normal levels";
            case SLOW_DEPENDENCY -> "An external dependency is responding slower than usual";
            case MEMORY_SPIKE -> "Memory usage has increased significantly above normal levels";
            case CPU_SPIKE -> "CPU usage has increased significantly above normal levels";
        };
    }
}
