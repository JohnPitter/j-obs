package io.github.jobs.application;

import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storing and querying traces.
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
