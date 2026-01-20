package io.github.jobs.application;

import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.domain.log.LogQuery;

import java.util.List;
import java.util.function.Consumer;

/**
 * Repository interface for storing and querying log entries.
 * <p>
 * This is the main abstraction for log storage in J-Obs. The default implementation
 * {@code InMemoryLogRepository} uses a circular buffer for efficient memory usage.
 * <p>
 * Key features:
 * <ul>
 *   <li>Add and query log entries with flexible filtering</li>
 *   <li>Subscribe to real-time log updates via {@link #subscribe(Consumer)}</li>
 *   <li>Get statistics about stored logs via {@link #stats()}</li>
 *   <li>Pagination support via {@link LogQuery}</li>
 * </ul>
 * <p>
 * Thread-safety: Implementations must be thread-safe as log entries can be added
 * concurrently from multiple threads.
 *
 * @see LogEntry
 * @see LogQuery
 * @see io.github.jobs.infrastructure.InMemoryLogRepository
 */
public interface LogRepository {

    /**
     * Adds a log entry to the repository.
     *
     * @param entry the log entry to add
     */
    void add(LogEntry entry);

    /**
     * Queries log entries based on the given criteria.
     *
     * @param query the query parameters
     * @return list of matching log entries
     */
    List<LogEntry> query(LogQuery query);

    /**
     * Returns the most recent log entries.
     *
     * @param limit maximum number of entries to return
     * @return list of recent entries
     */
    default List<LogEntry> recent(int limit) {
        return query(LogQuery.recent(limit));
    }

    /**
     * Returns all error log entries.
     *
     * @return list of error entries
     */
    default List<LogEntry> errors() {
        return query(LogQuery.errors());
    }

    /**
     * Returns log entries associated with a trace.
     *
     * @param traceId the trace ID
     * @return list of entries for the trace
     */
    default List<LogEntry> byTraceId(String traceId) {
        return query(LogQuery.byTraceId(traceId));
    }

    /**
     * Returns the total count of log entries in the repository.
     *
     * @return entry count
     */
    long count();

    /**
     * Returns the count of log entries matching the query.
     *
     * @param query the query parameters
     * @return matching entry count
     */
    long count(LogQuery query);

    /**
     * Clears all log entries from the repository.
     */
    void clear();

    /**
     * Returns statistics about the stored logs.
     *
     * @return log statistics
     */
    LogStats stats();

    /**
     * Subscribes to new log entries.
     * The consumer will be called for each new log entry added.
     *
     * @param subscriber the consumer to notify
     * @return a subscription handle that can be used to unsubscribe
     */
    Subscription subscribe(Consumer<LogEntry> subscriber);

    /**
     * Subscription handle for log entry notifications.
     */
    interface Subscription {
        void unsubscribe();
        boolean isActive();
    }

    /**
     * Statistics about stored logs.
     */
    record LogStats(
        long totalEntries,
        long errorCount,
        long warnCount,
        long infoCount,
        long debugCount,
        long traceCount,
        int uniqueLoggers,
        int uniqueThreads
    ) {
        public static LogStats empty() {
            return new LogStats(0, 0, 0, 0, 0, 0, 0, 0);
        }

        public long countByLevel(LogLevel level) {
            return switch (level) {
                case ERROR -> errorCount;
                case WARN -> warnCount;
                case INFO -> infoCount;
                case DEBUG -> debugCount;
                case TRACE -> traceCount;
            };
        }
    }
}
