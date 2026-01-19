package io.github.jobs.application;

import io.github.jobs.domain.metric.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port for metric storage and retrieval operations.
 */
public interface MetricRepository {

    /**
     * Gets all metrics matching the query.
     */
    List<Metric> query(MetricQuery query);

    /**
     * Gets a metric by its unique ID.
     */
    Optional<Metric> findById(String metricId);

    /**
     * Gets a metric by name and tags.
     */
    Optional<Metric> findByNameAndTags(String name, Map<String, String> tags);

    /**
     * Gets all unique metric names.
     */
    List<String> getMetricNames();

    /**
     * Gets all unique categories.
     */
    List<String> getCategories();

    /**
     * Gets all unique tag keys.
     */
    List<String> getTagKeys();

    /**
     * Gets all unique values for a tag key.
     */
    List<String> getTagValues(String tagKey);

    /**
     * Counts metrics matching the query.
     */
    long count(MetricQuery query);

    /**
     * Gets a time-series snapshot for a metric.
     */
    MetricSnapshot getSnapshot(String metricId, Duration duration);

    /**
     * Gets time-series snapshots for multiple metrics.
     */
    List<MetricSnapshot> getSnapshots(List<String> metricIds, Duration duration);

    /**
     * Gets aggregate statistics across all metrics.
     */
    MetricStats stats();

    /**
     * Refreshes the metrics from the underlying source.
     */
    void refresh();

    /**
     * Aggregate statistics about metrics.
     */
    record MetricStats(
            long totalMetrics,
            long counterCount,
            long gaugeCount,
            long timerCount,
            long distributionCount,
            long otherCount,
            int uniqueCategories,
            int uniqueTagKeys
    ) {
        public static MetricStats empty() {
            return new MetricStats(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
