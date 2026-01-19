package io.github.jobs.domain.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Query parameters for filtering metrics.
 */
public record MetricQuery(
        String namePattern,
        MetricType type,
        String category,
        Map<String, String> tags,
        int limit,
        int offset
) {
    public MetricQuery {
        tags = tags != null ? Map.copyOf(tags) : Map.of();
        if (limit <= 0) {
            limit = 100;
        }
        if (offset < 0) {
            offset = 0;
        }
    }

    /**
     * Creates a new query using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a query that matches all metrics.
     */
    public static MetricQuery all() {
        return builder().build();
    }

    /**
     * Creates a query for a specific metric name pattern.
     */
    public static MetricQuery byName(String pattern) {
        return builder().namePattern(pattern).build();
    }

    /**
     * Creates a query for a specific metric type.
     */
    public static MetricQuery byType(MetricType type) {
        return builder().type(type).build();
    }

    /**
     * Creates a query for a specific category.
     */
    public static MetricQuery byCategory(String category) {
        return builder().category(category).build();
    }

    /**
     * Checks if this query has any filters.
     */
    public boolean hasFilters() {
        return (namePattern != null && !namePattern.isEmpty()) ||
               type != null ||
               (category != null && !category.isEmpty()) ||
               !tags.isEmpty();
    }

    /**
     * Checks if a metric matches this query.
     */
    public boolean matches(Metric metric) {
        return metric.matches(this);
    }

    public static class Builder {
        private String namePattern;
        private MetricType type;
        private String category;
        private HashMap<String, String> tags = new HashMap<>();
        private int limit = 100;
        private int offset = 0;

        public Builder namePattern(String namePattern) {
            this.namePattern = namePattern;
            return this;
        }

        public Builder type(MetricType type) {
            this.type = type;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
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

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public MetricQuery build() {
            return new MetricQuery(namePattern, type, category, tags, limit, offset);
        }
    }
}
