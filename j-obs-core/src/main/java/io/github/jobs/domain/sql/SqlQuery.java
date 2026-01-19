package io.github.jobs.domain.sql;

import java.time.Instant;

/**
 * Represents a captured SQL query with execution metadata.
 */
public record SqlQuery(
        String spanId,
        String traceId,
        String statement,
        String normalizedStatement,
        String operation,
        String dbSystem,
        String dbName,
        long durationMs,
        Instant executedAt,
        String endpoint
) {

    /**
     * Normalizes a SQL statement by replacing literal values with placeholders.
     * This allows grouping similar queries together.
     *
     * @param sql the original SQL statement
     * @return the normalized statement
     */
    public static String normalize(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        return sql
                // Replace string literals
                .replaceAll("'[^']*'", "?")
                // Replace numeric literals
                .replaceAll("\\b\\d+\\b", "?")
                // Replace IN lists
                .replaceAll("IN\\s*\\([^)]+\\)", "IN (?)")
                // Normalize whitespace
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }

    /**
     * Extracts the operation type from a SQL statement.
     *
     * @param sql the SQL statement
     * @return the operation (SELECT, INSERT, UPDATE, DELETE, etc.)
     */
    public static String extractOperation(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "UNKNOWN";
        }

        String trimmed = sql.trim().toUpperCase();
        if (trimmed.startsWith("SELECT")) return "SELECT";
        if (trimmed.startsWith("INSERT")) return "INSERT";
        if (trimmed.startsWith("UPDATE")) return "UPDATE";
        if (trimmed.startsWith("DELETE")) return "DELETE";
        if (trimmed.startsWith("CREATE")) return "CREATE";
        if (trimmed.startsWith("ALTER")) return "ALTER";
        if (trimmed.startsWith("DROP")) return "DROP";
        if (trimmed.startsWith("TRUNCATE")) return "TRUNCATE";
        if (trimmed.startsWith("CALL")) return "CALL";
        if (trimmed.startsWith("EXEC")) return "EXEC";
        return "OTHER";
    }

    /**
     * Extracts the table name from a simple SQL statement.
     *
     * @param sql the SQL statement
     * @return the table name, or null if not found
     */
    public static String extractTable(String sql) {
        if (sql == null || sql.isEmpty()) {
            return null;
        }

        String upper = sql.trim().toUpperCase();

        // SELECT ... FROM table
        int fromIdx = upper.indexOf(" FROM ");
        if (fromIdx > 0) {
            String afterFrom = sql.substring(fromIdx + 6).trim();
            return extractFirstWord(afterFrom);
        }

        // INSERT INTO table
        int intoIdx = upper.indexOf(" INTO ");
        if (intoIdx > 0) {
            String afterInto = sql.substring(intoIdx + 6).trim();
            return extractFirstWord(afterInto);
        }

        // UPDATE table
        if (upper.startsWith("UPDATE ")) {
            String afterUpdate = sql.substring(7).trim();
            return extractFirstWord(afterUpdate);
        }

        // DELETE FROM table
        int deleteFromIdx = upper.indexOf("DELETE FROM ");
        if (deleteFromIdx >= 0) {
            String afterDelete = sql.substring(deleteFromIdx + 12).trim();
            return extractFirstWord(afterDelete);
        }

        return null;
    }

    private static String extractFirstWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        // Split on whitespace, parenthesis, or semicolon
        String[] parts = text.split("[\\s(;]+", 2);
        return parts.length > 0 ? parts[0] : null;
    }

    /**
     * Checks if this query uses SELECT *.
     */
    public boolean usesSelectStar() {
        if (statement == null) return false;
        return statement.toUpperCase().contains("SELECT *");
    }

    /**
     * Checks if this is a SELECT query without LIMIT.
     */
    public boolean missingLimit() {
        if (statement == null || !"SELECT".equals(operation)) return false;
        String upper = statement.toUpperCase();
        return !upper.contains(" LIMIT ") && !upper.contains(" TOP ") && !upper.contains(" FETCH ");
    }

    /**
     * Calculates similarity between this query and another.
     * Uses normalized statements for comparison.
     *
     * @param other the other query
     * @return similarity score between 0.0 and 1.0
     */
    public double similarityTo(SqlQuery other) {
        if (other == null || normalizedStatement == null || other.normalizedStatement == null) {
            return 0.0;
        }

        if (normalizedStatement.equals(other.normalizedStatement)) {
            return 1.0;
        }

        // Simple Jaccard similarity on words
        String[] words1 = normalizedStatement.split("\\s+");
        String[] words2 = other.normalizedStatement.split("\\s+");

        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
}
