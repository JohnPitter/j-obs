package io.github.jobs.domain.alert;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the condition that triggers an alert.
 */
public record AlertCondition(
        String metric,
        ComparisonOperator operator,
        double threshold,
        Duration window,
        Map<String, String> filters
) {
    public AlertCondition {
        Objects.requireNonNull(metric, "metric cannot be null");
        Objects.requireNonNull(operator, "operator cannot be null");
        if (window == null) {
            window = Duration.ofMinutes(5);
        }
        if (filters == null) {
            filters = Map.of();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple condition.
     */
    public static AlertCondition of(String metric, ComparisonOperator operator, double threshold) {
        return new AlertCondition(metric, operator, threshold, Duration.ofMinutes(5), Map.of());
    }

    /**
     * Evaluates the condition against a value.
     */
    public boolean evaluate(double value) {
        return operator.compare(value, threshold);
    }

    public String describe() {
        return metric + " " + operator.symbol() + " " + threshold;
    }

    public static class Builder {
        private String metric;
        private ComparisonOperator operator;
        private double threshold;
        private Duration window = Duration.ofMinutes(5);
        private Map<String, String> filters = Map.of();

        public Builder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public Builder operator(ComparisonOperator operator) {
            this.operator = operator;
            return this;
        }

        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder window(Duration window) {
            this.window = window;
            return this;
        }

        public Builder filters(Map<String, String> filters) {
            this.filters = filters;
            return this;
        }

        public AlertCondition build() {
            return new AlertCondition(metric, operator, threshold, window, filters);
        }
    }

    /**
     * Comparison operators for conditions.
     */
    public enum ComparisonOperator {
        GREATER_THAN(">", "greater than") {
            @Override
            public boolean compare(double value, double threshold) {
                return value > threshold;
            }
        },
        GREATER_THAN_OR_EQUAL(">=", "greater than or equal to") {
            @Override
            public boolean compare(double value, double threshold) {
                return value >= threshold;
            }
        },
        LESS_THAN("<", "less than") {
            @Override
            public boolean compare(double value, double threshold) {
                return value < threshold;
            }
        },
        LESS_THAN_OR_EQUAL("<=", "less than or equal to") {
            @Override
            public boolean compare(double value, double threshold) {
                return value <= threshold;
            }
        },
        EQUALS("==", "equals") {
            @Override
            public boolean compare(double value, double threshold) {
                return Math.abs(value - threshold) < 0.0001;
            }
        },
        NOT_EQUALS("!=", "not equals") {
            @Override
            public boolean compare(double value, double threshold) {
                return Math.abs(value - threshold) >= 0.0001;
            }
        };

        private final String symbol;
        private final String description;

        ComparisonOperator(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        public String symbol() {
            return symbol;
        }

        public String description() {
            return description;
        }

        public abstract boolean compare(double value, double threshold);
    }
}
