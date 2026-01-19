package io.github.jobs.spring.metric;

import io.github.jobs.application.MetricRepository;
import io.github.jobs.domain.metric.Metric;
import io.github.jobs.domain.metric.MetricQuery;
import io.github.jobs.domain.metric.MetricType;
import io.github.jobs.domain.metric.MetricValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CachedMetricRepository.
 */
@ExtendWith(MockitoExtension.class)
class CachedMetricRepositoryTest {

    @Mock
    private MetricRepository delegate;

    private CachedMetricRepository cachedRepository;

    @BeforeEach
    void setUp() {
        cachedRepository = new CachedMetricRepository(delegate, Duration.ofMillis(100));
    }

    private Metric createMetric(String name, MetricType type) {
        MetricValue value = new MetricValue(Instant.now(), 100.0, null, null, null);
        return new Metric(name, null, type, null, Map.of(), value);
    }

    private MetricRepository.MetricStats createStats(long total, long counters, long gauges, long timers, long other) {
        return new MetricRepository.MetricStats(total, counters, gauges, timers, 0, other, 2, 3);
    }

    // ==================== stats ====================

    @Test
    void stats_shouldCacheResult() {
        MetricRepository.MetricStats stats = createStats(10, 5, 3, 2, 0);
        when(delegate.stats()).thenReturn(stats);

        // First call
        MetricRepository.MetricStats result1 = cachedRepository.stats();
        // Second call (should use cache)
        MetricRepository.MetricStats result2 = cachedRepository.stats();

        assertThat(result1).isEqualTo(stats);
        assertThat(result2).isEqualTo(stats);

        // Delegate should only be called once
        verify(delegate, times(1)).stats();
    }

    @Test
    void stats_shouldRefetchAfterCacheExpires() throws InterruptedException {
        MetricRepository.MetricStats stats1 = createStats(10, 5, 3, 2, 0);
        MetricRepository.MetricStats stats2 = createStats(20, 10, 6, 4, 0);
        when(delegate.stats()).thenReturn(stats1).thenReturn(stats2);

        // First call
        MetricRepository.MetricStats result1 = cachedRepository.stats();

        // Wait for cache to expire
        Thread.sleep(150);

        // Second call (cache expired)
        MetricRepository.MetricStats result2 = cachedRepository.stats();

        assertThat(result1).isEqualTo(stats1);
        assertThat(result2).isEqualTo(stats2);

        verify(delegate, times(2)).stats();
    }

    // ==================== getMetricNames ====================

    @Test
    void getMetricNames_shouldCacheResult() {
        List<String> names = List.of("metric1", "metric2");
        when(delegate.getMetricNames()).thenReturn(names);

        List<String> result1 = cachedRepository.getMetricNames();
        List<String> result2 = cachedRepository.getMetricNames();

        assertThat(result1).isEqualTo(names);
        assertThat(result2).isEqualTo(names);

        verify(delegate, times(1)).getMetricNames();
    }

    // ==================== getCategories ====================

    @Test
    void getCategories_shouldCacheResult() {
        List<String> categories = List.of("http", "jvm");
        when(delegate.getCategories()).thenReturn(categories);

        List<String> result1 = cachedRepository.getCategories();
        List<String> result2 = cachedRepository.getCategories();

        assertThat(result1).isEqualTo(categories);
        assertThat(result2).isEqualTo(categories);

        verify(delegate, times(1)).getCategories();
    }

    // ==================== getTagKeys ====================

    @Test
    void getTagKeys_shouldCacheResult() {
        List<String> keys = List.of("method", "status");
        when(delegate.getTagKeys()).thenReturn(keys);

        List<String> result1 = cachedRepository.getTagKeys();
        List<String> result2 = cachedRepository.getTagKeys();

        assertThat(result1).isEqualTo(keys);
        assertThat(result2).isEqualTo(keys);

        verify(delegate, times(1)).getTagKeys();
    }

    // ==================== getTagValues ====================

    @Test
    void getTagValues_shouldCachePerTagKey() {
        List<String> methods = List.of("GET", "POST");
        List<String> statuses = List.of("200", "500");
        when(delegate.getTagValues("method")).thenReturn(methods);
        when(delegate.getTagValues("status")).thenReturn(statuses);

        // Call for "method" twice
        List<String> result1 = cachedRepository.getTagValues("method");
        List<String> result2 = cachedRepository.getTagValues("method");

        // Call for "status" once
        List<String> result3 = cachedRepository.getTagValues("status");

        assertThat(result1).isEqualTo(methods);
        assertThat(result2).isEqualTo(methods);
        assertThat(result3).isEqualTo(statuses);

        verify(delegate, times(1)).getTagValues("method");
        verify(delegate, times(1)).getTagValues("status");
    }

    // ==================== query ====================

    @Test
    void query_shouldCacheByQueryKey() {
        Metric metric = createMetric("http.requests", MetricType.COUNTER);
        List<Metric> metrics = List.of(metric);

        MetricQuery query = new MetricQuery(null, null, null, null, 0, 100);
        when(delegate.query(query)).thenReturn(metrics);

        List<Metric> result1 = cachedRepository.query(query);
        List<Metric> result2 = cachedRepository.query(query);

        assertThat(result1).isEqualTo(metrics);
        assertThat(result2).isEqualTo(metrics);

        verify(delegate, times(1)).query(query);
    }

    @Test
    void query_shouldUseDifferentCacheForDifferentQueries() {
        Metric metric1 = createMetric("http.requests", MetricType.COUNTER);
        Metric metric2 = createMetric("jvm.memory", MetricType.GAUGE);

        MetricQuery query1 = new MetricQuery("http.*", null, null, null, 0, 100);
        MetricQuery query2 = new MetricQuery("jvm.*", null, null, null, 0, 100);

        when(delegate.query(query1)).thenReturn(List.of(metric1));
        when(delegate.query(query2)).thenReturn(List.of(metric2));

        List<Metric> result1 = cachedRepository.query(query1);
        List<Metric> result2 = cachedRepository.query(query2);

        assertThat(result1).containsExactly(metric1);
        assertThat(result2).containsExactly(metric2);

        verify(delegate, times(1)).query(query1);
        verify(delegate, times(1)).query(query2);
    }

    // ==================== count ====================

    @Test
    void count_shouldCacheResult() {
        MetricQuery query = new MetricQuery(null, null, null, null, 0, 100);
        when(delegate.count(query)).thenReturn(42L);

        long result1 = cachedRepository.count(query);
        long result2 = cachedRepository.count(query);

        assertThat(result1).isEqualTo(42L);
        assertThat(result2).isEqualTo(42L);

        verify(delegate, times(1)).count(query);
    }

    // ==================== refresh ====================

    @Test
    void refresh_shouldInvalidateCaches() {
        List<String> names1 = List.of("metric1");
        List<String> names2 = List.of("metric1", "metric2");
        when(delegate.getMetricNames()).thenReturn(names1).thenReturn(names2);

        // First call
        List<String> result1 = cachedRepository.getMetricNames();

        // Refresh (should invalidate cache)
        cachedRepository.refresh();

        // Second call (should fetch new data)
        List<String> result2 = cachedRepository.getMetricNames();

        assertThat(result1).isEqualTo(names1);
        assertThat(result2).isEqualTo(names2);

        verify(delegate, times(2)).getMetricNames();
        verify(delegate, times(1)).refresh();
    }

    // ==================== invalidateAllCaches ====================

    @Test
    void invalidateAllCaches_shouldClearAllCaches() {
        MetricRepository.MetricStats stats1 = createStats(10, 5, 3, 2, 0);
        MetricRepository.MetricStats stats2 = createStats(20, 10, 6, 4, 0);
        when(delegate.stats()).thenReturn(stats1).thenReturn(stats2);

        // First call
        cachedRepository.stats();

        // Invalidate
        cachedRepository.invalidateAllCaches();

        // Second call
        MetricRepository.MetricStats result = cachedRepository.stats();

        assertThat(result).isEqualTo(stats2);
        verify(delegate, times(2)).stats();
    }

    // ==================== Non-cached operations ====================

    @Test
    void findById_shouldNotCache() {
        Metric metric = createMetric("http.requests", MetricType.COUNTER);
        when(delegate.findById("id1")).thenReturn(java.util.Optional.of(metric));

        cachedRepository.findById("id1");
        cachedRepository.findById("id1");

        // Should call delegate twice (no caching)
        verify(delegate, times(2)).findById("id1");
    }

    @Test
    void getSnapshot_shouldNotCache() {
        when(delegate.getSnapshot(eq("id1"), any())).thenReturn(null);

        cachedRepository.getSnapshot("id1", Duration.ofMinutes(5));
        cachedRepository.getSnapshot("id1", Duration.ofMinutes(5));

        verify(delegate, times(2)).getSnapshot(eq("id1"), any());
    }
}
