package io.github.jobs.domain.metric;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a metric measurement at a point in time.
 */
public record MetricValue(
        Instant timestamp,
        double value,
        String unit,
        Map<String, Double> percentiles,
        Map<String, Double> statistics
) {
    public MetricValue {
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        percentiles = percentiles != null ? Map.copyOf(percentiles) : Map.of();
        statistics = statistics != null ? Map.copyOf(statistics) : Map.of();
    }

    /**
     * Creates a simple metric value with just a value.
     */
    public static MetricValue of(double value) {
        return new MetricValue(Instant.now(), value, null, null, null);
    }

    /**
     * Creates a metric value with timestamp.
     */
    public static MetricValue of(Instant timestamp, double value) {
        return new MetricValue(timestamp, value, null, null, null);
    }

    /**
     * Creates a metric value with unit.
     */
    public static MetricValue of(double value, String unit) {
        return new MetricValue(Instant.now(), value, unit, null, null);
    }

    /**
     * Creates a full metric value using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a percentile value.
     */
    public Double getPercentile(String key) {
        return percentiles.get(key);
    }

    /**
     * Gets a statistic value.
     */
    public Double getStatistic(String key) {
        return statistics.get(key);
    }

    /**
     * Gets the count if available.
     */
    public Double count() {
        return statistics.get("count");
    }

    /**
     * Gets the total/sum if available.
     */
    public Double total() {
        return statistics.get("total");
    }

    /**
     * Gets the mean if available.
     */
    public Double mean() {
        return statistics.get("mean");
    }

    /**
     * Gets the max if available.
     */
    public Double max() {
        return statistics.get("max");
    }

    /**
     * Checks if this value has percentile data.
     */
    public boolean hasPercentiles() {
        return !percentiles.isEmpty();
    }

    /**
     * Formats the value with its unit.
     */
    public String formattedValue() {
        if (unit == null || unit.isEmpty()) {
            return formatNumber(value);
        }
        return formatNumber(value) + " " + unit;
    }

    private String formatNumber(double val) {
        if (val == (long) val) {
            return String.format("%d", (long) val);
        }
        if (Math.abs(val) < 0.01) {
            return String.format("%.4f", val);
        }
        return String.format("%.2f", val);
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private double value;
        private String unit;
        private HashMap<String, Double> percentiles = new HashMap<>();
        private HashMap<String, Double> statistics = new HashMap<>();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder percentile(String key, Double value) {
            if (value != null) {
                this.percentiles.put(key, value);
            }
            return this;
        }

        public Builder percentiles(Map<String, Double> percentiles) {
            if (percentiles != null) {
                this.percentiles.putAll(percentiles);
            }
            return this;
        }

        public Builder statistic(String key, Double value) {
            if (value != null) {
                this.statistics.put(key, value);
            }
            return this;
        }

        public Builder statistics(Map<String, Double> statistics) {
            if (statistics != null) {
                this.statistics.putAll(statistics);
            }
            return this;
        }

        public Builder count(double count) {
            return statistic("count", count);
        }

        public Builder total(double total) {
            return statistic("total", total);
        }

        public Builder mean(double mean) {
            return statistic("mean", mean);
        }

        public Builder max(double max) {
            return statistic("max", max);
        }

        public MetricValue build() {
            return new MetricValue(timestamp, value, unit, percentiles, statistics);
        }
    }
}
