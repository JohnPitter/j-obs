package io.github.jobs.domain.slo;

/**
 * Status of a Service Level Objective.
 */
public enum SloStatus {
    /**
     * SLO is being met with healthy error budget.
     */
    HEALTHY("Healthy", "text-green-400", "bg-green-500/20", "#22c55e"),

    /**
     * SLO is being met but error budget is running low.
     */
    AT_RISK("At Risk", "text-amber-400", "bg-amber-500/20", "#f59e0b"),

    /**
     * SLO has been breached (error budget exhausted).
     */
    BREACHED("Breached", "text-red-400", "bg-red-500/20", "#ef4444"),

    /**
     * Not enough data to determine SLO status.
     */
    NO_DATA("No Data", "text-slate-400", "bg-slate-500/20", "#94a3b8");

    private final String displayName;
    private final String textCssClass;
    private final String bgCssClass;
    private final String color;

    SloStatus(String displayName, String textCssClass, String bgCssClass, String color) {
        this.displayName = displayName;
        this.textCssClass = textCssClass;
        this.bgCssClass = bgCssClass;
        this.color = color;
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

    public String color() {
        return color;
    }

    /**
     * Checks if the SLO needs attention.
     *
     * @return true if at risk or breached
     */
    public boolean needsAttention() {
        return this == AT_RISK || this == BREACHED;
    }

    /**
     * Checks if the SLO is healthy.
     *
     * @return true if healthy
     */
    public boolean isHealthy() {
        return this == HEALTHY;
    }
}
