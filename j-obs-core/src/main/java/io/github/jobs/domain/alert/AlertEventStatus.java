package io.github.jobs.domain.alert;

/**
 * Status of an alert event.
 */
public enum AlertEventStatus {
    FIRING("Firing", "text-red-600 dark:text-red-400", "bg-red-100 dark:bg-red-900"),
    ACKNOWLEDGED("Acknowledged", "text-yellow-600 dark:text-yellow-400", "bg-yellow-100 dark:bg-yellow-900"),
    RESOLVED("Resolved", "text-green-600 dark:text-green-400", "bg-green-100 dark:bg-green-900");

    private final String displayName;
    private final String textCssClass;
    private final String bgCssClass;

    AlertEventStatus(String displayName, String textCssClass, String bgCssClass) {
        this.displayName = displayName;
        this.textCssClass = textCssClass;
        this.bgCssClass = bgCssClass;
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
}
