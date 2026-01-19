package io.github.jobs.spring.web;

import io.github.jobs.application.MetricRepository;
import io.github.jobs.application.MetricRepository.MetricStats;
import io.github.jobs.domain.metric.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for metric data.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/metrics")
public class MetricApiController {

    private final MetricRepository metricRepository;

    public MetricApiController(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public MetricsResponse getMetrics(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Map<String, String> tags,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        MetricQuery.Builder queryBuilder = MetricQuery.builder()
                .limit(limit)
                .offset(offset);

        if (name != null && !name.isEmpty()) {
            queryBuilder.namePattern(name);
        }
        if (type != null && !type.isEmpty()) {
            queryBuilder.type(MetricType.fromMicrometerType(type));
        }
        if (category != null && !category.isEmpty()) {
            queryBuilder.category(category);
        }
        // Filter out non-tag query params
        if (tags != null) {
            tags.entrySet().stream()
                    .filter(e -> !List.of("name", "type", "category", "limit", "offset", "duration").contains(e.getKey()))
                    .forEach(e -> queryBuilder.tag(e.getKey(), e.getValue()));
        }

        MetricQuery query = queryBuilder.build();
        List<Metric> metrics = metricRepository.query(query);
        long total = metricRepository.count(query);

        return new MetricsResponse(
                metrics.stream().map(MetricDto::from).toList(),
                total,
                limit,
                offset
        );
    }

    @GetMapping(value = "/{metricId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MetricDetailDto getMetric(@PathVariable String metricId) {
        return metricRepository.findById(metricId)
                .map(m -> MetricDetailDto.from(m, metricRepository.getSnapshot(metricId, Duration.ofHours(1))))
                .orElse(null);
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public MetricStats getStats() {
        return metricRepository.stats();
    }

    @GetMapping(value = "/quick-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuickStatsDto getQuickStats() {
        // Find JVM memory metrics
        Double jvmMemoryUsed = findMetricValue("jvm.memory.used");
        Double jvmMemoryMax = findMetricValue("jvm.memory.max");
        Double threadCount = findMetricValue("jvm.threads.live");
        Double cpuUsage = findMetricValue("process.cpu.usage");
        if (cpuUsage == null) {
            cpuUsage = findMetricValue("system.cpu.usage");
        }
        Double uptime = findMetricValue("process.uptime");
        Double gcCount = findMetricValue("jvm.gc.pause");

        return new QuickStatsDto(
                jvmMemoryUsed != null ? jvmMemoryUsed.longValue() : null,
                jvmMemoryMax != null ? jvmMemoryMax.longValue() : null,
                threadCount != null ? threadCount.intValue() : null,
                cpuUsage,
                uptime,
                gcCount != null ? gcCount.longValue() : 0L
        );
    }

    private Double findMetricValue(String metricName) {
        MetricQuery query = MetricQuery.builder()
                .namePattern(metricName)
                .limit(1)
                .build();
        List<Metric> metrics = metricRepository.query(query);
        if (metrics.isEmpty()) {
            return null;
        }
        // Sum all values for this metric type (e.g., all memory pools)
        return metricRepository.query(MetricQuery.builder().namePattern(metricName).limit(100).build())
                .stream()
                .mapToDouble(Metric::value)
                .sum();
    }

    @GetMapping(value = "/names", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getMetricNames() {
        return metricRepository.getMetricNames();
    }

    @GetMapping(value = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getCategories() {
        return metricRepository.getCategories();
    }

    @GetMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getTagKeys() {
        return metricRepository.getTagKeys();
    }

    @GetMapping(value = "/tags/{key}/values", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getTagValues(@PathVariable String key) {
        return metricRepository.getTagValues(key);
    }

    @GetMapping(value = "/snapshot", produces = MediaType.APPLICATION_JSON_VALUE)
    public SnapshotResponse getSnapshot(
            @RequestParam String metricId,
            @RequestParam(defaultValue = "3600") long durationSeconds
    ) {
        MetricSnapshot snapshot = metricRepository.getSnapshot(metricId, Duration.ofSeconds(durationSeconds));
        if (snapshot == null) {
            return null;
        }
        return SnapshotResponse.from(snapshot);
    }

    @PostMapping(value = "/refresh")
    public void refreshMetrics() {
        metricRepository.refresh();
    }

    // DTOs

    public record MetricsResponse(
            List<MetricDto> metrics,
            long total,
            int limit,
            int offset
    ) {}

    public record MetricDto(
            String id,
            String name,
            String displayName,
            String shortName,
            String category,
            String type,
            String typeDisplayName,
            String description,
            String baseUnit,
            Map<String, String> tags,
            double value,
            String formattedValue,
            String cssClass
    ) {
        public static MetricDto from(Metric metric) {
            return new MetricDto(
                    metric.id(),
                    metric.name(),
                    metric.displayName(),
                    metric.shortName(),
                    metric.category(),
                    metric.type().name(),
                    metric.type().displayName(),
                    metric.description(),
                    metric.baseUnit(),
                    metric.tags(),
                    metric.value(),
                    metric.formattedValue(),
                    metric.cssClass()
            );
        }
    }

    public record MetricDetailDto(
            String id,
            String name,
            String displayName,
            String type,
            String typeDisplayName,
            String description,
            String baseUnit,
            Map<String, String> tags,
            MetricValueDto currentValue,
            List<DataPointDto> history
    ) {
        public static MetricDetailDto from(Metric metric, MetricSnapshot snapshot) {
            List<DataPointDto> history = snapshot != null
                    ? snapshot.dataPoints().stream().map(DataPointDto::from).toList()
                    : List.of();

            return new MetricDetailDto(
                    metric.id(),
                    metric.name(),
                    metric.displayName(),
                    metric.type().name(),
                    metric.type().displayName(),
                    metric.description(),
                    metric.baseUnit(),
                    metric.tags(),
                    MetricValueDto.from(metric.currentValue()),
                    history
            );
        }
    }

    public record MetricValueDto(
            Instant timestamp,
            double value,
            String formattedValue,
            String unit,
            Map<String, Double> percentiles,
            Map<String, Double> statistics
    ) {
        public static MetricValueDto from(MetricValue value) {
            if (value == null) {
                return null;
            }
            return new MetricValueDto(
                    value.timestamp(),
                    value.value(),
                    value.formattedValue(),
                    value.unit(),
                    value.percentiles(),
                    value.statistics()
            );
        }
    }

    public record DataPointDto(
            Instant timestamp,
            double value
    ) {
        public static DataPointDto from(MetricSnapshot.DataPoint point) {
            return new DataPointDto(point.timestamp(), point.value());
        }
    }

    public record SnapshotResponse(
            String metricId,
            String metricName,
            String type,
            Map<String, String> tags,
            List<DataPointDto> dataPoints,
            double min,
            double max,
            double average,
            double current
    ) {
        public static SnapshotResponse from(MetricSnapshot snapshot) {
            return new SnapshotResponse(
                    snapshot.metricId(),
                    snapshot.metricName(),
                    snapshot.type().name(),
                    snapshot.tags(),
                    snapshot.dataPoints().stream().map(DataPointDto::from).toList(),
                    snapshot.min(),
                    snapshot.max(),
                    snapshot.average(),
                    snapshot.currentValue()
            );
        }
    }

    public record QuickStatsDto(
            Long jvmMemoryUsed,
            Long jvmMemoryMax,
            Integer threadCount,
            Double cpuUsage,
            Double uptime,
            Long gcCount
    ) {}
}
