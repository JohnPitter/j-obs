package io.github.jobs.domain.metric;

import java.util.*;

/**
 * Represents a metric with its metadata and current value.
 */
public record Metric(
        String name,
        String description,
        MetricType type,
        String baseUnit,
        Map<String, String> tags,
        MetricValue currentValue
) {
    public Metric {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        tags = tags != null ? Map.copyOf(tags) : Map.of();
    }

    /**
     * Creates a new metric using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets a unique identifier for this metric (name + tags).
     */
    public String id() {
        if (tags.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name);
        tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(",").append(e.getKey()).append("=").append(e.getValue()));
        return sb.toString();
    }

    /**
     * Gets a display name for the metric.
     */
    public String displayName() {
        return name.replace("_", " ").replace(".", " ");
    }

    /**
     * Gets a short name (last part after last dot).
     */
    public String shortName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot >= 0 ? name.substring(lastDot + 1) : name;
    }

    /**
     * Gets the metric category (first part before first dot).
     */
    public String category() {
        int firstDot = name.indexOf('.');
        return firstDot >= 0 ? name.substring(0, firstDot) : name;
    }

    /**
     * Gets a tag value.
     */
    public String tag(String key) {
        return tags.get(key);
    }

    /**
     * Checks if the metric has a specific tag.
     */
    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }

    /**
     * Checks if the metric has a specific tag value.
     */
    public boolean hasTag(String key, String value) {
        return Objects.equals(tags.get(key), value);
    }

    /**
     * Checks if this metric matches a query.
     */
    public boolean matches(MetricQuery query) {
        if (query == null) {
            return true;
        }

        // Check name pattern
        if (query.namePattern() != null && !query.namePattern().isEmpty()) {
            if (!name.contains(query.namePattern())) {
                return false;
            }
        }

        // Check type
        if (query.type() != null && type != query.type()) {
            return false;
        }

        // Check required tags
        if (query.tags() != null) {
            for (Map.Entry<String, String> entry : query.tags().entrySet()) {
                if (!Objects.equals(tags.get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
        }

        // Check category
        if (query.category() != null && !query.category().isEmpty()) {
            if (!category().equals(query.category())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the current value as a double, or 0 if no value.
     */
    public double value() {
        return currentValue != null ? currentValue.value() : 0.0;
    }

    /**
     * Gets the formatted current value.
     */
    public String formattedValue() {
        if (currentValue == null) {
            return "N/A";
        }
        return currentValue.formattedValue();
    }

    /**
     * Creates a copy of this metric with a new value.
     */
    public Metric withValue(MetricValue newValue) {
        return new Metric(name, description, type, baseUnit, tags, newValue);
    }

    /**
     * Determines the CSS class for display based on the metric type.
     */
    public String cssClass() {
        return switch (type) {
            case COUNTER -> "text-blue-600 dark:text-blue-400";
            case GAUGE -> "text-green-600 dark:text-green-400";
            case TIMER -> "text-purple-600 dark:text-purple-400";
            case DISTRIBUTION_SUMMARY -> "text-orange-600 dark:text-orange-400";
            case LONG_TASK_TIMER -> "text-yellow-600 dark:text-yellow-400";
            case UNKNOWN -> "text-gray-600 dark:text-gray-400";
        };
    }

    public static class Builder {
        private String name;
        private String description;
        private MetricType type = MetricType.UNKNOWN;
        private String baseUnit;
        private final Map<String, String> tags = new LinkedHashMap<>();
        private MetricValue currentValue;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(MetricType type) {
            this.type = type;
            return this;
        }

        public Builder baseUnit(String baseUnit) {
            this.baseUnit = baseUnit;
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

        public Builder currentValue(MetricValue currentValue) {
            this.currentValue = currentValue;
            return this;
        }

        public Builder value(double value) {
            this.currentValue = MetricValue.of(value);
            return this;
        }

        public Metric build() {
            return new Metric(name, description, type, baseUnit, tags, currentValue);
        }
    }
}
