package io.github.jobs.domain.log;

/**
 * Log level enumeration matching SLF4J/Logback levels.
 */
public enum LogLevel {
    TRACE(0, "trace", "text-gray-400", "bg-gray-100 dark:bg-gray-800"),
    DEBUG(1, "debug", "text-blue-500", "bg-blue-100 dark:bg-blue-900"),
    INFO(2, "info", "text-green-500", "bg-green-100 dark:bg-green-900"),
    WARN(3, "warn", "text-yellow-500", "bg-yellow-100 dark:bg-yellow-900"),
    ERROR(4, "error", "text-red-500", "bg-red-100 dark:bg-red-900");

    private final int severity;
    private final String displayName;
    private final String textCssClass;
    private final String bgCssClass;

    LogLevel(int severity, String displayName, String textCssClass, String bgCssClass) {
        this.severity = severity;
        this.displayName = displayName;
        this.textCssClass = textCssClass;
        this.bgCssClass = bgCssClass;
    }

    public int severity() {
        return severity;
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

    public boolean isAtLeast(LogLevel other) {
        return this.severity >= other.severity;
    }

    public boolean isError() {
        return this == ERROR;
    }

    /**
     * Parse a log level from string, case-insensitive.
     */
    public static LogLevel fromString(String level) {
        if (level == null || level.isEmpty()) {
            return INFO;
        }
        try {
            return valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}
