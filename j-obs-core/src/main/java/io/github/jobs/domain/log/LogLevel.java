package io.github.jobs.domain.log;

/**
 * Log level enumeration matching SLF4J/Logback severity levels.
 * <p>
 * Levels are ordered by severity from least to most severe:
 * {@link #TRACE} &lt; {@link #DEBUG} &lt; {@link #INFO} &lt; {@link #WARN} &lt; {@link #ERROR}
 * <p>
 * Each level includes CSS classes for styling in the J-Obs dashboard UI.
 *
 * @see LogEntry
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

    /**
     * Returns the numeric severity value (higher = more severe).
     *
     * @return the severity value (0-4)
     */
    public int severity() {
        return severity;
    }

    /**
     * Returns the display name for UI rendering.
     *
     * @return lowercase level name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the Tailwind CSS class for text color styling.
     *
     * @return CSS class string
     */
    public String textCssClass() {
        return textCssClass;
    }

    /**
     * Returns the Tailwind CSS class for background color styling.
     *
     * @return CSS class string with dark mode support
     */
    public String bgCssClass() {
        return bgCssClass;
    }

    /**
     * Checks if this level is at least as severe as another level.
     *
     * @param other the level to compare against
     * @return true if this level's severity is greater than or equal to other's
     */
    public boolean isAtLeast(LogLevel other) {
        return this.severity >= other.severity;
    }

    /**
     * Checks if this is the ERROR level.
     *
     * @return true if this is ERROR
     */
    public boolean isError() {
        return this == ERROR;
    }

    /**
     * Parses a log level from string, case-insensitive.
     * Returns {@link #INFO} if the string is null, empty, or invalid.
     *
     * @param level the level string to parse
     * @return the corresponding LogLevel, or INFO if invalid
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
