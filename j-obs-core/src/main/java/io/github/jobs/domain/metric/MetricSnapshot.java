package io.github.jobs.domain.metric;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Represents a time-series snapshot of metric values.
 */
public record MetricSnapshot(
        String metricId,
        String metricName,
        MetricType type,
        Map<String, String> tags,
        List<DataPoint> dataPoints
) {
    public MetricSnapshot {
        Objects.requireNonNull(metricId, "metricId cannot be null");
        Objects.requireNonNull(metricName, "metricName cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        tags = tags != null ? Map.copyOf(tags) : Map.of();
        dataPoints = dataPoints != null ? List.copyOf(dataPoints) : List.of();
    }

    /**
     * Creates a snapshot from a metric with a single data point.
     */
    public static MetricSnapshot fromMetric(Metric metric) {
        List<DataPoint> points = new ArrayList<>();
        if (metric.currentValue() != null) {
            points.add(new DataPoint(
                    metric.currentValue().timestamp(),
                    metric.currentValue().value()
            ));
        }
        return new MetricSnapshot(
                metric.id(),
                metric.name(),
                metric.type(),
                metric.tags(),
                points
        );
    }

    /**
     * Creates a new snapshot using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the latest data point.
     */
    public Optional<DataPoint> latest() {
        if (dataPoints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dataPoints.get(dataPoints.size() - 1));
    }

    /**
     * Gets the earliest data point.
     */
    public Optional<DataPoint> earliest() {
        if (dataPoints.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(dataPoints.get(0));
    }

    /**
     * Gets the current value.
     */
    public double currentValue() {
        return latest().map(DataPoint::value).orElse(0.0);
    }

    /**
     * Gets the minimum value in the snapshot.
     */
    public double min() {
        return dataPoints.stream()
                .mapToDouble(DataPoint::value)
                .min()
                .orElse(0.0);
    }

    /**
     * Gets the maximum value in the snapshot.
     */
    public double max() {
        return dataPoints.stream()
                .mapToDouble(DataPoint::value)
                .max()
                .orElse(0.0);
    }

    /**
     * Gets the average value in the snapshot.
     */
    public double average() {
        return dataPoints.stream()
                .mapToDouble(DataPoint::value)
                .average()
                .orElse(0.0);
    }

    /**
     * Gets the time range of this snapshot.
     */
    public Optional<Duration> timeRange() {
        if (dataPoints.size() < 2) {
            return Optional.empty();
        }
        Instant start = dataPoints.get(0).timestamp();
        Instant end = dataPoints.get(dataPoints.size() - 1).timestamp();
        return Optional.of(Duration.between(start, end));
    }

    /**
     * A single data point in the time series.
     */
    public record DataPoint(Instant timestamp, double value) {
        public DataPoint {
            Objects.requireNonNull(timestamp, "timestamp cannot be null");
        }

        public static DataPoint of(Instant timestamp, double value) {
            return new DataPoint(timestamp, value);
        }

        public static DataPoint now(double value) {
            return new DataPoint(Instant.now(), value);
        }

        /**
         * Formats the value for display.
         */
        public String formattedValue() {
            if (value == (long) value) {
                return String.format("%d", (long) value);
            }
            if (Math.abs(value) < 0.01) {
                return String.format("%.4f", value);
            }
            return String.format("%.2f", value);
        }
    }

    public static class Builder {
        private String metricId;
        private String metricName;
        private MetricType type = MetricType.UNKNOWN;
        private final Map<String, String> tags = new LinkedHashMap<>();
        private final List<DataPoint> dataPoints = new ArrayList<>();

        public Builder metricId(String metricId) {
            this.metricId = metricId;
            return this;
        }

        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        public Builder type(MetricType type) {
            this.type = type;
            return this;
        }

        public Builder tag(String key, String value) {
            if (key != null && value != null) {
                this.tags.put(key, value);
            }
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            if (tags != null) {
                this.tags.putAll(tags);
            }
            return this;
        }

        public Builder addDataPoint(Instant timestamp, double value) {
            this.dataPoints.add(new DataPoint(timestamp, value));
            return this;
        }

        public Builder addDataPoint(DataPoint point) {
            this.dataPoints.add(point);
            return this;
        }

        public Builder dataPoints(List<DataPoint> points) {
            if (points != null) {
                this.dataPoints.addAll(points);
            }
            return this;
        }

        public MetricSnapshot build() {
            if (metricId == null && metricName != null) {
                metricId = metricName;
            }
            return new MetricSnapshot(metricId, metricName, type, tags, dataPoints);
        }
    }
}
