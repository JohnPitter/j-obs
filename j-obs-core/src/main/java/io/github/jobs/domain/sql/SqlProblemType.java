package io.github.jobs.domain.sql;

/**
 * Types of SQL problems that can be detected.
 */
public enum SqlProblemType {

    /**
     * N+1 query problem: multiple similar queries executed in a loop.
     * Example: 1 query to get orders + N queries to get order items.
     */
    N_PLUS_ONE("N+1 Query", "critical", "Multiple similar queries executed instead of a single JOIN"),

    /**
     * Query execution time exceeds configured threshold.
     */
    SLOW_QUERY("Slow Query", "warning", "Query execution time exceeds threshold"),

    /**
     * Very slow query that significantly impacts performance.
     */
    VERY_SLOW_QUERY("Very Slow Query", "critical", "Query execution time is critically high"),

    /**
     * Query uses SELECT * instead of specific columns.
     */
    SELECT_STAR("SELECT *", "info", "Query selects all columns instead of specific ones"),

    /**
     * Query returns a large number of rows.
     */
    LARGE_RESULT_SET("Large Result Set", "warning", "Query returns too many rows"),

    /**
     * SELECT query without LIMIT clause on large table.
     */
    MISSING_LIMIT("Missing LIMIT", "warning", "SELECT without LIMIT on potentially large dataset"),

    /**
     * Cartesian product due to missing JOIN condition.
     */
    CARTESIAN_JOIN("Cartesian Join", "critical", "JOIN without proper condition creates cartesian product"),

    /**
     * Query pattern suggests missing index.
     */
    MISSING_INDEX("Missing Index", "warning", "Frequently executed query may benefit from an index");

    private final String displayName;
    private final String severity;
    private final String description;

    SqlProblemType(String displayName, String severity, String description) {
        this.displayName = displayName;
        this.severity = severity;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String severity() {
        return severity;
    }

    public String description() {
        return description;
    }

    public boolean isCritical() {
        return "critical".equals(severity);
    }

    public boolean isWarning() {
        return "warning".equals(severity);
    }

    public String cssClass() {
        return switch (severity) {
            case "critical" -> "text-red-500";
            case "warning" -> "text-amber-500";
            default -> "text-blue-500";
        };
    }

    public String bgCssClass() {
        return switch (severity) {
            case "critical" -> "bg-red-950/50 border-red-900/50";
            case "warning" -> "bg-amber-950/50 border-amber-900/50";
            default -> "bg-blue-950/50 border-blue-900/50";
        };
    }
}
