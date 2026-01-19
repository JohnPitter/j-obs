package io.github.jobs.spring.metric;

import io.github.jobs.application.MetricRepository;
import io.github.jobs.domain.metric.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MetricRepository implementation backed by Micrometer MeterRegistry.
 */
public class MicrometerMetricRepository implements MetricRepository {

    private final MeterRegistry meterRegistry;
    private final Map<String, List<MetricSnapshot.DataPoint>> history = new ConcurrentHashMap<>();
    private final int maxHistoryPoints;
    private volatile Instant lastRefresh = Instant.MIN;

    public MicrometerMetricRepository(MeterRegistry meterRegistry) {
        this(meterRegistry, 360); // 1 hour at 10s intervals
    }

    public MicrometerMetricRepository(MeterRegistry meterRegistry, int maxHistoryPoints) {
        this.meterRegistry = meterRegistry;
        this.maxHistoryPoints = maxHistoryPoints;
    }

    @Override
    public List<Metric> query(MetricQuery query) {
        return meterRegistry.getMeters().stream()
                .map(this::toMetric)
                .filter(m -> m.matches(query))
                .sorted(Comparator.comparing(Metric::name))
                .skip(query.offset())
                .limit(query.limit())
                .toList();
    }

    @Override
    public Optional<Metric> findById(String metricId) {
        return meterRegistry.getMeters().stream()
                .map(this::toMetric)
                .filter(m -> m.id().equals(metricId))
                .findFirst();
    }

    @Override
    public Optional<Metric> findByNameAndTags(String name, Map<String, String> tags) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(name))
                .filter(meter -> matchesTags(meter.getId().getTags(), tags))
                .map(this::toMetric)
                .findFirst();
    }

    private boolean matchesTags(List<Tag> meterTags, Map<String, String> queryTags) {
        if (queryTags == null || queryTags.isEmpty()) {
            return true;
        }
        Map<String, String> tagMap = meterTags.stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        return queryTags.entrySet().stream()
                .allMatch(e -> Objects.equals(tagMap.get(e.getKey()), e.getValue()));
    }

    @Override
    public List<String> getMetricNames() {
        return meterRegistry.getMeters().stream()
                .map(m -> m.getId().getName())
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getCategories() {
        return meterRegistry.getMeters().stream()
                .map(m -> m.getId().getName())
                .map(name -> {
                    int dot = name.indexOf('.');
                    return dot >= 0 ? name.substring(0, dot) : name;
                })
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getTagKeys() {
        return meterRegistry.getMeters().stream()
                .flatMap(m -> m.getId().getTags().stream())
                .map(Tag::getKey)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getTagValues(String tagKey) {
        return meterRegistry.getMeters().stream()
                .flatMap(m -> m.getId().getTags().stream())
                .filter(tag -> tag.getKey().equals(tagKey))
                .map(Tag::getValue)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public long count(MetricQuery query) {
        return meterRegistry.getMeters().stream()
                .map(this::toMetric)
                .filter(m -> m.matches(query))
                .count();
    }

    @Override
    public MetricSnapshot getSnapshot(String metricId, Duration duration) {
        Optional<Metric> metric = findById(metricId);
        if (metric.isEmpty()) {
            return null;
        }

        List<MetricSnapshot.DataPoint> points = history.getOrDefault(metricId, new ArrayList<>());
        Instant cutoff = Instant.now().minus(duration);

        List<MetricSnapshot.DataPoint> filteredPoints = points.stream()
                .filter(p -> p.timestamp().isAfter(cutoff))
                .toList();

        return MetricSnapshot.builder()
                .metricId(metricId)
                .metricName(metric.get().name())
                .type(metric.get().type())
                .tags(metric.get().tags())
                .dataPoints(filteredPoints)
                .build();
    }

    @Override
    public List<MetricSnapshot> getSnapshots(List<String> metricIds, Duration duration) {
        return metricIds.stream()
                .map(id -> getSnapshot(id, duration))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public MetricStats stats() {
        Map<MetricType, Long> typeCounts = meterRegistry.getMeters().stream()
                .collect(Collectors.groupingBy(
                        m -> toMetricType(m),
                        Collectors.counting()
                ));

        int uniqueCategories = getCategories().size();
        int uniqueTagKeys = getTagKeys().size();

        return new MetricStats(
                meterRegistry.getMeters().size(),
                typeCounts.getOrDefault(MetricType.COUNTER, 0L),
                typeCounts.getOrDefault(MetricType.GAUGE, 0L),
                typeCounts.getOrDefault(MetricType.TIMER, 0L),
                typeCounts.getOrDefault(MetricType.DISTRIBUTION_SUMMARY, 0L),
                typeCounts.getOrDefault(MetricType.LONG_TASK_TIMER, 0L) +
                        typeCounts.getOrDefault(MetricType.UNKNOWN, 0L),
                uniqueCategories,
                uniqueTagKeys
        );
    }

    @Override
    public void refresh() {
        Instant now = Instant.now();

        for (Meter meter : meterRegistry.getMeters()) {
            Metric metric = toMetric(meter);
            String id = metric.id();

            if (metric.currentValue() != null) {
                history.computeIfAbsent(id, k -> new ArrayList<>())
                        .add(new MetricSnapshot.DataPoint(now, metric.value()));

                // Trim history
                List<MetricSnapshot.DataPoint> points = history.get(id);
                if (points.size() > maxHistoryPoints) {
                    history.put(id, new ArrayList<>(points.subList(
                            points.size() - maxHistoryPoints, points.size())));
                }
            }
        }

        lastRefresh = now;
    }

    private Metric toMetric(Meter meter) {
        Meter.Id id = meter.getId();
        MetricType type = toMetricType(meter);
        MetricValue value = extractValue(meter);

        Map<String, String> tags = id.getTags().stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));

        return Metric.builder()
                .name(id.getName())
                .description(id.getDescription())
                .type(type)
                .baseUnit(id.getBaseUnit())
                .tags(tags)
                .currentValue(value)
                .build();
    }

    private MetricType toMetricType(Meter meter) {
        if (meter instanceof Counter) {
            return MetricType.COUNTER;
        } else if (meter instanceof Gauge) {
            return MetricType.GAUGE;
        } else if (meter instanceof io.micrometer.core.instrument.Timer) {
            return MetricType.TIMER;
        } else if (meter instanceof DistributionSummary) {
            return MetricType.DISTRIBUTION_SUMMARY;
        } else if (meter instanceof LongTaskTimer) {
            return MetricType.LONG_TASK_TIMER;
        }
        return MetricType.UNKNOWN;
    }

    private MetricValue extractValue(Meter meter) {
        MetricValue.Builder builder = MetricValue.builder()
                .timestamp(Instant.now());

        if (meter instanceof Counter counter) {
            builder.value(counter.count());
        } else if (meter instanceof Gauge gauge) {
            builder.value(gauge.value());
        } else if (meter instanceof io.micrometer.core.instrument.Timer timer) {
            builder.value(timer.mean(TimeUnit.MILLISECONDS))
                    .unit("ms")
                    .count(timer.count())
                    .total(timer.totalTime(TimeUnit.MILLISECONDS))
                    .mean(timer.mean(TimeUnit.MILLISECONDS))
                    .max(timer.max(TimeUnit.MILLISECONDS));

            // Add percentiles if available
            HistogramSnapshot snapshot = timer.takeSnapshot();
            for (ValueAtPercentile vap : snapshot.percentileValues()) {
                String key = "p" + (int) (vap.percentile() * 100);
                builder.percentile(key, vap.value(TimeUnit.MILLISECONDS));
            }
        } else if (meter instanceof DistributionSummary summary) {
            builder.value(summary.mean())
                    .count(summary.count())
                    .total(summary.totalAmount())
                    .mean(summary.mean())
                    .max(summary.max());

            HistogramSnapshot snapshot = summary.takeSnapshot();
            for (ValueAtPercentile vap : snapshot.percentileValues()) {
                String key = "p" + (int) (vap.percentile() * 100);
                builder.percentile(key, vap.value());
            }
        } else if (meter instanceof LongTaskTimer ltt) {
            builder.value(ltt.activeTasks())
                    .statistic("activeTasks", (double) ltt.activeTasks())
                    .statistic("duration", ltt.duration(TimeUnit.MILLISECONDS));
        } else {
            // Try to get a value from measurements
            meter.measure().forEach(m -> {
                if (m.getStatistic() == Statistic.VALUE ||
                    m.getStatistic() == Statistic.COUNT) {
                    builder.value(m.getValue());
                }
            });
        }

        return builder.build();
    }
}
