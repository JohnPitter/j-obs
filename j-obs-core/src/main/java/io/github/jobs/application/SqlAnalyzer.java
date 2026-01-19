package io.github.jobs.application;

import io.github.jobs.domain.sql.SqlIssue;
import io.github.jobs.domain.sql.SqlProblemType;
import io.github.jobs.domain.sql.SqlQuery;

import java.time.Duration;
import java.util.List;

/**
 * Analyzes SQL queries from traces to detect performance issues.
 * <p>
 * The analyzer processes trace data to identify common SQL anti-patterns such as:
 * <ul>
 *   <li>N+1 query problems</li>
 *   <li>Slow queries</li>
 *   <li>Missing indexes (based on query patterns)</li>
 *   <li>SELECT * usage</li>
 *   <li>Large result sets</li>
 * </ul>
 */
public interface SqlAnalyzer {

    /**
     * Analyzes recent traces within the specified time window.
     *
     * @param window the time window to analyze
     * @return list of detected SQL issues
     */
    List<SqlIssue> analyze(Duration window);

    /**
     * Analyzes a specific trace for SQL issues.
     *
     * @param traceId the trace ID to analyze
     * @return list of detected SQL issues
     */
    List<SqlIssue> analyzeTrace(String traceId);

    /**
     * Gets all detected issues, optionally filtered by severity.
     *
     * @param minSeverity minimum severity level (null for all)
     * @return list of SQL issues
     */
    List<SqlIssue> getIssues(SqlProblemType minSeverity);

    /**
     * Gets slow queries within the specified time window.
     *
     * @param threshold minimum duration to consider slow
     * @param window time window to analyze
     * @return list of slow queries
     */
    List<SqlQuery> getSlowQueries(Duration threshold, Duration window);

    /**
     * Gets the top N queries by execution count.
     *
     * @param limit maximum number of queries to return
     * @param window time window to analyze
     * @return list of frequently executed queries
     */
    List<QueryStats> getTopQueries(int limit, Duration window);

    /**
     * Gets statistics about SQL analysis.
     *
     * @return analysis statistics
     */
    SqlAnalysisStats getStats();

    /**
     * Clears all detected issues.
     */
    void clearIssues();

    /**
     * Statistics for a specific query pattern.
     */
    record QueryStats(
            String normalizedQuery,
            String operation,
            String dbSystem,
            long executionCount,
            double avgDurationMs,
            long minDurationMs,
            long maxDurationMs,
            long p50DurationMs,
            long p95DurationMs,
            long p99DurationMs
    ) {}

    /**
     * Overall statistics for SQL analysis.
     */
    record SqlAnalysisStats(
            long totalQueries,
            long totalIssues,
            long criticalIssues,
            long warningIssues,
            long nPlusOneDetected,
            long slowQueriesDetected,
            double avgQueryDurationMs
    ) {}
}
