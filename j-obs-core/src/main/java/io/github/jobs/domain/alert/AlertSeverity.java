package io.github.jobs.domain.alert;

/**
 * Severity levels for alerts.
 */
public enum AlertSeverity {
    INFO("Info", "text-blue-600 dark:text-blue-400", "bg-blue-100 dark:bg-blue-900", "ℹ️"),
    WARNING("Warning", "text-yellow-600 dark:text-yellow-400", "bg-yellow-100 dark:bg-yellow-900", "⚠️"),
    CRITICAL("Critical", "text-red-600 dark:text-red-400", "bg-red-100 dark:bg-red-900", "🔴");

    private final String displayName;
    private final String textCssClass;
    private final String bgCssClass;
    private final String emoji;

    AlertSeverity(String displayName, String textCssClass, String bgCssClass, String emoji) {
        this.displayName = displayName;
        this.textCssClass = textCssClass;
        this.bgCssClass = bgCssClass;
        this.emoji = emoji;
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

    public String emoji() {
        return emoji;
    }
}
