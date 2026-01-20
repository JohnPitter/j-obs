package io.github.jobs.application;

import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for storing and querying distributed traces.
 * <p>
 * This is the main abstraction for trace storage in J-Obs. The default implementation
 * {@code InMemoryTraceRepository} uses a concurrent map with TTL-based cleanup.
 * <p>
 * Key features:
 * <ul>
 *   <li>Add spans which are automatically grouped by trace ID</li>
 *   <li>Query traces with flexible filtering via {@link TraceQuery}</li>
 *   <li>Get statistics about stored traces via {@link #stats()}</li>
 *   <li>Automatic cleanup of old traces based on retention period</li>
 * </ul>
 * <p>
 * Thread-safety: Implementations must be thread-safe as spans can be added
 * concurrently from multiple threads.
 *
 * @see Span
 * @see Trace
 * @see TraceQuery
 * @see io.github.jobs.infrastructure.InMemoryTraceRepository
 */
public interface TraceRepository {

    /**
     * Adds a span to the repository.
     * The span will be grouped with other spans sharing the same trace ID.
     *
     * @param span the span to add
     */
    void addSpan(Span span);

    /**
     * Retrieves a trace by its ID.
     *
     * @param traceId the trace ID
     * @return the trace if found
     */
    Optional<Trace> findByTraceId(String traceId);

    /**
     * Queries traces based on the given criteria.
     *
     * @param query the query parameters
     * @return list of matching traces
     */
    List<Trace> query(TraceQuery query);

    /**
     * Returns the most recent traces.
     *
     * @param limit maximum number of traces to return
     * @return list of recent traces
     */
    default List<Trace> recent(int limit) {
        return query(TraceQuery.recent(limit));
    }

    /**
     * Returns all traces with errors.
     *
     * @return list of traces with errors
     */
    default List<Trace> errors() {
        return query(TraceQuery.errors());
    }

    /**
     * Returns the total count of traces in the repository.
     *
     * @return trace count
     */
    long count();

    /**
     * Returns the count of traces matching the query.
     *
     * @param query the query parameters
     * @return matching trace count
     */
    long count(TraceQuery query);

    /**
     * Clears all traces from the repository.
     */
    void clear();

    /**
     * Returns statistics about the stored traces.
     *
     * @return trace statistics
     */
    TraceStats stats();

    /**
     * Statistics about stored traces.
     */
    record TraceStats(
        long totalTraces,
        long totalSpans,
        long errorTraces,
        double avgDurationMs,
        double p50DurationMs,
        double p95DurationMs,
        double p99DurationMs
    ) {
        public static TraceStats empty() {
            return new TraceStats(0, 0, 0, 0, 0, 0, 0);
        }
    }
}
