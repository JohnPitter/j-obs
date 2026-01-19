package io.github.jobs.spring.metric;

import io.github.jobs.application.MetricRepository;
import io.github.jobs.domain.metric.Metric;
import io.github.jobs.domain.metric.MetricQuery;
import io.github.jobs.domain.metric.MetricSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Caching decorator for MetricRepository.
 * Caches expensive operations like stats(), getMetricNames(), etc.
 */
public class CachedMetricRepository implements MetricRepository {

    private final MetricRepository delegate;
    private final Duration cacheDuration;

    // Cache storage
    private volatile CacheEntry<MetricStats> statsCache;
    private volatile CacheEntry<List<String>> metricNamesCache;
    private volatile CacheEntry<List<String>> categoriesCache;
    private volatile CacheEntry<List<String>> tagKeysCache;
    private final Map<String, CacheEntry<List<String>>> tagValuesCache = new ConcurrentHashMap<>();
    private final Map<QueryKey, CacheEntry<List<Metric>>> queryCache = new ConcurrentHashMap<>();
    private final Map<QueryKey, CacheEntry<Long>> countCache = new ConcurrentHashMap<>();

    // Lock for cache invalidation
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final int MAX_QUERY_CACHE_SIZE = 100;

    public CachedMetricRepository(MetricRepository delegate) {
        this(delegate, Duration.ofSeconds(10));
    }

    public CachedMetricRepository(MetricRepository delegate, Duration cacheDuration) {
        this.delegate = delegate;
        this.cacheDuration = cacheDuration;
    }

    @Override
    public List<Metric> query(MetricQuery query) {
        QueryKey key = new QueryKey(query);

        lock.readLock().lock();
        try {
            CacheEntry<List<Metric>> cached = queryCache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.value();
            }
        } finally {
            lock.readLock().unlock();
        }

        List<Metric> result = delegate.query(query);

        lock.writeLock().lock();
        try {
            // Limit cache size
            if (queryCache.size() >= MAX_QUERY_CACHE_SIZE) {
                cleanExpiredEntries();
            }
            queryCache.put(key, new CacheEntry<>(result, cacheDuration));
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }

    @Override
    public Optional<Metric> findById(String metricId) {
        return delegate.findById(metricId);
    }

    @Override
    public Optional<Metric> findByNameAndTags(String name, Map<String, String> tags) {
        return delegate.findByNameAndTags(name, tags);
    }

    @Override
    public List<String> getMetricNames() {
        CacheEntry<List<String>> cached = metricNamesCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        List<String> result = delegate.getMetricNames();
        metricNamesCache = new CacheEntry<>(result, cacheDuration);
        return result;
    }

    @Override
    public List<String> getCategories() {
        CacheEntry<List<String>> cached = categoriesCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        List<String> result = delegate.getCategories();
        categoriesCache = new CacheEntry<>(result, cacheDuration);
        return result;
    }

    @Override
    public List<String> getTagKeys() {
        CacheEntry<List<String>> cached = tagKeysCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        List<String> result = delegate.getTagKeys();
        tagKeysCache = new CacheEntry<>(result, cacheDuration);
        return result;
    }

    @Override
    public List<String> getTagValues(String tagKey) {
        CacheEntry<List<String>> cached = tagValuesCache.get(tagKey);
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        List<String> result = delegate.getTagValues(tagKey);
        tagValuesCache.put(tagKey, new CacheEntry<>(result, cacheDuration));
        return result;
    }

    @Override
    public long count(MetricQuery query) {
        QueryKey key = new QueryKey(query);

        lock.readLock().lock();
        try {
            CacheEntry<Long> cached = countCache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.value();
            }
        } finally {
            lock.readLock().unlock();
        }

        long result = delegate.count(query);

        lock.writeLock().lock();
        try {
            if (countCache.size() >= MAX_QUERY_CACHE_SIZE) {
                cleanExpiredEntries();
            }
            countCache.put(key, new CacheEntry<>(result, cacheDuration));
        } finally {
            lock.writeLock().unlock();
        }

        return result;
    }

    @Override
    public MetricSnapshot getSnapshot(String metricId, Duration duration) {
        return delegate.getSnapshot(metricId, duration);
    }

    @Override
    public List<MetricSnapshot> getSnapshots(List<String> metricIds, Duration duration) {
        return delegate.getSnapshots(metricIds, duration);
    }

    @Override
    public MetricStats stats() {
        CacheEntry<MetricStats> cached = statsCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value();
        }

        MetricStats result = delegate.stats();
        statsCache = new CacheEntry<>(result, cacheDuration);
        return result;
    }

    @Override
    public void refresh() {
        delegate.refresh();
        invalidateAllCaches();
    }

    /**
     * Invalidates all caches.
     */
    public void invalidateAllCaches() {
        lock.writeLock().lock();
        try {
            statsCache = null;
            metricNamesCache = null;
            categoriesCache = null;
            tagKeysCache = null;
            tagValuesCache.clear();
            queryCache.clear();
            countCache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void cleanExpiredEntries() {
        queryCache.entrySet().removeIf(e -> e.getValue().isExpired());
        countCache.entrySet().removeIf(e -> e.getValue().isExpired());
        tagValuesCache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /**
     * Cache entry with expiration.
     */
    private record CacheEntry<T>(T value, Instant expiresAt) {
        CacheEntry(T value, Duration ttl) {
            this(value, Instant.now().plus(ttl));
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Key for query cache.
     */
    private record QueryKey(
            String namePattern,
            String category,
            String type,
            Map<String, String> tags,
            int offset,
            int limit
    ) {
        QueryKey(MetricQuery query) {
            this(
                    query.namePattern(),
                    query.category(),
                    query.type() != null ? query.type().name() : null,
                    query.tags(),
                    query.offset(),
                    query.limit()
            );
        }
    }
}
