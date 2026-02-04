package io.github.jobs.infrastructure;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.SpanStatus;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * In-memory implementation of TraceRepository with TTL support.
 * Uses a concurrent map for thread-safe access and periodic cleanup.
 * <p>
 * Implements {@link Closeable} for proper resource cleanup.
 */
public class InMemoryTraceRepository implements TraceRepository, Closeable {

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final int DEFAULT_MAX_TRACES = 10000;
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(1);

    private final Map<String, TraceEntry> traces = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int maxTraces;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService cleanupExecutor;
    private long insertionCounter = 0;

    public InMemoryTraceRepository() {
        this(DEFAULT_TTL, DEFAULT_MAX_TRACES);
    }

    public InMemoryTraceRepository(Duration ttl, int maxTraces) {
        this.ttl = ttl;
        this.maxTraces = maxTraces;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "j-obs-trace-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduleCleanup();
    }

    private void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanup,
            CLEANUP_INTERVAL.toMillis(),
            CLEANUP_INTERVAL.toMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void addSpan(Span span) {
        lock.writeLock().lock();
        try {
            TraceEntry entry = traces.computeIfAbsent(
                span.traceId(),
                id -> new TraceEntry(id, insertionCounter++)
            );
            entry.addSpan(span);
            entry.touch();

            // Evict oldest traces if limit exceeded
            if (traces.size() > maxTraces) {
                evictOldest();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Trace> findByTraceId(String traceId) {
        lock.readLock().lock();
        try {
            TraceEntry entry = traces.get(traceId);
            if (entry == null || entry.isExpired(ttl)) {
                return Optional.empty();
            }
            return Optional.of(entry.toTrace());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Trace> query(TraceQuery query) {
        lock.readLock().lock();
        try {
            return traces.values().stream()
                .filter(e -> !e.isExpired(ttl))
                .map(TraceEntry::toTrace)
                .filter(query::matches)
                .sorted(Comparator.comparing(Trace::startTime).reversed())
                .skip(query.offset())
                .limit(query.limit())
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long count() {
        lock.readLock().lock();
        try {
            return traces.values().stream()
                .filter(e -> !e.isExpired(ttl))
                .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long count(TraceQuery query) {
        lock.readLock().lock();
        try {
            return traces.values().stream()
                .filter(e -> !e.isExpired(ttl))
                .map(TraceEntry::toTrace)
                .filter(query::matches)
                .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            traces.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public TraceStats stats() {
        lock.readLock().lock();
        try {
            List<Trace> allTraces = traces.values().stream()
                .filter(e -> !e.isExpired(ttl))
                .map(TraceEntry::toTrace)
                .toList();

            if (allTraces.isEmpty()) {
                return TraceStats.empty();
            }

            long totalSpans = allTraces.stream()
                .mapToLong(Trace::spanCount)
                .sum();

            long errorTraces = allTraces.stream()
                .filter(Trace::hasError)
                .count();

            List<Long> durations = allTraces.stream()
                .map(Trace::durationMs)
                .sorted()
                .toList();

            double avgDuration = durations.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

            return new TraceStats(
                allTraces.size(),
                totalSpans,
                errorTraces,
                avgDuration,
                percentile(durations, 50),
                percentile(durations, 95),
                percentile(durations, 99)
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    private double percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private void cleanup() {
        lock.writeLock().lock();
        try {
            traces.entrySet().removeIf(e -> e.getValue().isExpired(ttl));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evictOldest() {
        // Find and remove the oldest 10% of traces (at least 1)
        int toRemove = Math.max(1, maxTraces / 10);

        // Use a bounded priority queue to find oldest entries in O(n log k) instead of O(n log n)
        // Compare by lastAccess first, then by insertionOrder for deterministic ordering
        PriorityQueue<Map.Entry<String, TraceEntry>> oldest = new PriorityQueue<>(
            toRemove + 1,
            (a, b) -> {
                int accessCompare = b.getValue().lastAccess.compareTo(a.getValue().lastAccess);
                if (accessCompare != 0) return accessCompare; // max heap by lastAccess
                return Long.compare(b.getValue().insertionOrder, a.getValue().insertionOrder); // max heap by insertion order
            }
        );

        for (Map.Entry<String, TraceEntry> entry : traces.entrySet()) {
            oldest.offer(entry);
            if (oldest.size() > toRemove) {
                oldest.poll(); // Remove the most recent (max)
            }
        }

        // Remove the oldest entries
        while (!oldest.isEmpty()) {
            traces.remove(oldest.poll().getKey());
        }
    }

    /**
     * Shuts down the cleanup executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        traces.clear();
    }

    /**
     * Implements Closeable for resource cleanup.
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * Internal entry for storing trace data.
     */
    private static class TraceEntry {
        private final String traceId;
        private final List<Span> spans = new ArrayList<>();
        private final long insertionOrder;
        private volatile Instant lastAccess;

        TraceEntry(String traceId, long insertionOrder) {
            this.traceId = traceId;
            this.insertionOrder = insertionOrder;
            this.lastAccess = Instant.now();
        }

        void addSpan(Span span) {
            spans.add(span);
        }

        void touch() {
            lastAccess = Instant.now();
        }

        boolean isExpired(Duration ttl) {
            return Instant.now().isAfter(lastAccess.plus(ttl));
        }

        Trace toTrace() {
            return Trace.of(traceId, new ArrayList<>(spans));
        }
    }
}
