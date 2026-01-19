package io.github.jobs.domain.anomaly;

/**
 * Status of a detected anomaly.
 */
public enum AnomalyStatus {

    /**
     * Anomaly is currently active/ongoing.
     */
    ACTIVE("Active", "text-red-600", "bg-red-100"),

    /**
     * Anomaly has been acknowledged by user.
     */
    ACKNOWLEDGED("Acknowledged", "text-amber-600", "bg-amber-100"),

    /**
     * Anomaly has been resolved (values returned to normal).
     */
    RESOLVED("Resolved", "text-green-600", "bg-green-100"),

    /**
     * Anomaly was ignored/dismissed by user.
     */
    IGNORED("Ignored", "text-gray-600", "bg-gray-100");

    private final String displayName;
    private final String cssClass;
    private final String bgCssClass;

    AnomalyStatus(String displayName, String cssClass, String bgCssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.bgCssClass = bgCssClass;
    }

    public String displayName() {
        return displayName;
    }

    public String cssClass() {
        return cssClass;
    }

    public String bgCssClass() {
        return bgCssClass;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean needsAttention() {
        return this == ACTIVE || this == ACKNOWLEDGED;
    }
}
