package io.github.jobs.infrastructure;

import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.domain.log.LogQuery;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * In-memory implementation of LogRepository using a circular buffer.
 * Thread-safe with support for real-time subscriptions.
 */
public class InMemoryLogRepository implements LogRepository {

    private static final int DEFAULT_MAX_ENTRIES = 10000;

    private final LogEntry[] buffer;
    private final int maxEntries;
    private int head = 0;
    private int size = 0;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<SubscriptionImpl> subscribers = new CopyOnWriteArrayList<>();

    public InMemoryLogRepository() {
        this(DEFAULT_MAX_ENTRIES);
    }

    public InMemoryLogRepository(int maxEntries) {
        this.maxEntries = maxEntries;
        this.buffer = new LogEntry[maxEntries];
    }

    @Override
    public void add(LogEntry entry) {
        lock.writeLock().lock();
        try {
            buffer[head] = entry;
            head = (head + 1) % maxEntries;
            if (size < maxEntries) {
                size++;
            }
        } finally {
            lock.writeLock().unlock();
        }

        // Notify subscribers outside the lock
        notifySubscribers(entry);
    }

    private void notifySubscribers(LogEntry entry) {
        for (SubscriptionImpl subscription : subscribers) {
            if (subscription.isActive()) {
                try {
                    subscription.consumer.accept(entry);
                } catch (Exception e) {
                    // Log error but don't fail - subscriber issue shouldn't affect main flow
                }
            }
        }
    }

    @Override
    public List<LogEntry> query(LogQuery query) {
        lock.readLock().lock();
        try {
            // Efficient pagination: iterate directly in reverse order (newest first)
            // without creating intermediate collections
            List<LogEntry> result = new ArrayList<>(Math.min(query.limit(), size));
            int skipped = 0;
            int collected = 0;
            int offset = query.offset();
            int limit = query.limit();

            // Iterate in reverse order (newest first) directly on buffer
            for (int i = 0; i < size && collected < limit; i++) {
                // Calculate index for reverse iteration (newest first)
                // head points to next write position, so head-1 is most recent
                int index = (head - 1 - i + maxEntries) % maxEntries;
                LogEntry entry = buffer[index];

                if (entry != null && query.matches(entry)) {
                    if (skipped < offset) {
                        skipped++;
                    } else {
                        result.add(entry);
                        collected++;
                    }
                }
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long count() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long count(LogQuery query) {
        lock.readLock().lock();
        try {
            // Efficient count: iterate directly without creating intermediate collections
            long matchCount = 0;
            int start = (size < maxEntries) ? 0 : head;

            for (int i = 0; i < size; i++) {
                int index = (start + i) % maxEntries;
                LogEntry entry = buffer[index];
                if (entry != null && query.matches(entry)) {
                    matchCount++;
                }
            }

            return matchCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            Arrays.fill(buffer, null);
            head = 0;
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public LogStats stats() {
        lock.readLock().lock();
        try {
            // Single-pass calculation for O(n) performance
            long errorCount = 0;
            long warnCount = 0;
            long infoCount = 0;
            long debugCount = 0;
            long traceCount = 0;
            Set<String> uniqueLoggers = new HashSet<>();
            Set<String> uniqueThreads = new HashSet<>();

            // Iterate directly over the buffer without creating intermediate list
            int start = (size < maxEntries) ? 0 : head;
            for (int i = 0; i < size; i++) {
                int index = (start + i) % maxEntries;
                LogEntry entry = buffer[index];
                if (entry == null) continue;

                // Count by level
                switch (entry.level()) {
                    case ERROR -> errorCount++;
                    case WARN -> warnCount++;
                    case INFO -> infoCount++;
                    case DEBUG -> debugCount++;
                    case TRACE -> traceCount++;
                }

                // Collect unique loggers and threads
                if (entry.loggerName() != null) {
                    uniqueLoggers.add(entry.loggerName());
                }
                if (entry.threadName() != null) {
                    uniqueThreads.add(entry.threadName());
                }
            }

            return new LogStats(
                    size,
                    errorCount,
                    warnCount,
                    infoCount,
                    debugCount,
                    traceCount,
                    uniqueLoggers.size(),
                    uniqueThreads.size()
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Subscription subscribe(Consumer<LogEntry> subscriber) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        subscribers.add(subscription);
        return subscription;
    }

    /**
     * Returns the current buffer capacity.
     */
    public int capacity() {
        return maxEntries;
    }

    private class SubscriptionImpl implements Subscription {
        private final Consumer<LogEntry> consumer;
        private volatile boolean active = true;

        SubscriptionImpl(Consumer<LogEntry> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void unsubscribe() {
            active = false;
            subscribers.remove(this);
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }
}
