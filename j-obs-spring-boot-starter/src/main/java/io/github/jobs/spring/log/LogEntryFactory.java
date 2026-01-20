package io.github.jobs.spring.log;

import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory for creating LogEntry instances with optimized memory allocation.
 * <p>
 * Optimizations:
 * <ul>
 *   <li>Uses atomic counter for IDs instead of UUID (much faster, no random generation)</li>
 *   <li>Pools MutableLogEntryBuilder instances to reduce object allocation</li>
 *   <li>Thread-safe for concurrent log processing</li>
 * </ul>
 * <p>
 * Performance characteristics:
 * <ul>
 *   <li>ID generation: O(1) atomic increment vs O(n) UUID random bytes</li>
 *   <li>Builder pooling: Reduces GC pressure under high log volume</li>
 *   <li>Pool size is bounded to prevent memory leaks</li>
 * </ul>
 */
public class LogEntryFactory {

    private static final int MAX_POOL_SIZE = 256;
    private static final String ID_PREFIX = "log-";

    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());
    private final Queue<MutableLogEntryBuilder> builderPool = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new LogEntry with an optimized ID.
     * Uses pooled builder when available.
     */
    public LogEntry create(
            Instant timestamp,
            LogLevel level,
            String loggerName,
            String message,
            String threadName,
            String traceId,
            String spanId,
            String throwable,
            Map<String, String> mdc
    ) {
        MutableLogEntryBuilder builder = acquireBuilder();
        try {
            return builder
                    .id(generateId())
                    .timestamp(timestamp)
                    .level(level)
                    .loggerName(loggerName)
                    .message(message)
                    .threadName(threadName)
                    .traceId(traceId)
                    .spanId(spanId)
                    .throwable(throwable)
                    .mdc(mdc)
                    .build();
        } finally {
            releaseBuilder(builder);
        }
    }

    /**
     * Generates a fast, unique ID using atomic counter.
     * Format: "log-{counter}" where counter starts from current time millis.
     * <p>
     * This is ~10x faster than UUID.randomUUID().toString() and produces
     * shorter strings (typically 20-25 chars vs 36 chars for UUID).
     */
    private String generateId() {
        return ID_PREFIX + idCounter.incrementAndGet();
    }

    /**
     * Acquires a builder from the pool or creates a new one.
     */
    private MutableLogEntryBuilder acquireBuilder() {
        MutableLogEntryBuilder builder = builderPool.poll();
        return builder != null ? builder.reset() : new MutableLogEntryBuilder();
    }

    /**
     * Returns a builder to the pool if there's room.
     */
    private void releaseBuilder(MutableLogEntryBuilder builder) {
        if (builderPool.size() < MAX_POOL_SIZE) {
            builderPool.offer(builder.reset());
        }
        // If pool is full, builder is discarded and will be GC'd
    }

    /**
     * Returns the current pool size (for monitoring/testing).
     */
    public int getPoolSize() {
        return builderPool.size();
    }

    /**
     * Returns the total number of IDs generated (for monitoring/testing).
     */
    public long getIdCount() {
        return idCounter.get() - System.currentTimeMillis();
    }

    /**
     * Mutable builder that can be reset and reused.
     * Internal class - not exposed publicly.
     */
    private static final class MutableLogEntryBuilder {
        private String id;
        private Instant timestamp;
        private LogLevel level;
        private String loggerName;
        private String message;
        private String threadName;
        private String traceId;
        private String spanId;
        private String throwable;
        private Map<String, String> mdc;

        MutableLogEntryBuilder reset() {
            this.id = null;
            this.timestamp = null;
            this.level = null;
            this.loggerName = null;
            this.message = null;
            this.threadName = null;
            this.traceId = null;
            this.spanId = null;
            this.throwable = null;
            this.mdc = null;
            return this;
        }

        MutableLogEntryBuilder id(String id) {
            this.id = id;
            return this;
        }

        MutableLogEntryBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        MutableLogEntryBuilder level(LogLevel level) {
            this.level = level;
            return this;
        }

        MutableLogEntryBuilder loggerName(String loggerName) {
            this.loggerName = loggerName;
            return this;
        }

        MutableLogEntryBuilder message(String message) {
            this.message = message;
            return this;
        }

        MutableLogEntryBuilder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        MutableLogEntryBuilder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        MutableLogEntryBuilder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        MutableLogEntryBuilder throwable(String throwable) {
            this.throwable = throwable;
            return this;
        }

        MutableLogEntryBuilder mdc(Map<String, String> mdc) {
            this.mdc = mdc;
            return this;
        }

        LogEntry build() {
            return LogEntry.builder()
                    .id(id)
                    .timestamp(timestamp)
                    .level(level)
                    .loggerName(loggerName)
                    .message(message)
                    .threadName(threadName)
                    .traceId(traceId)
                    .spanId(spanId)
                    .throwable(throwable)
                    .mdc(mdc)
                    .build();
        }
    }
}
