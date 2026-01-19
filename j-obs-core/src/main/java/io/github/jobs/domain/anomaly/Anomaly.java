package io.github.jobs.domain.anomaly;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a detected anomaly in the system.
 */
public final class Anomaly {

    private final String id;
    private final AnomalyType type;
    private final String metric;
    private final String endpoint;
    private final String service;
    private final double baselineValue;
    private final double currentValue;
    private final double deviation;
    private final double percentageChange;
    private final Instant detectedAt;
    private final Instant resolvedAt;
    private final List<PossibleCause> possibleCauses;
    private final AnomalyStatus status;

    private Anomaly(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.type = Objects.requireNonNull(builder.type, "type is required");
        this.metric = builder.metric;
        this.endpoint = builder.endpoint;
        this.service = builder.service;
        this.baselineValue = builder.baselineValue;
        this.currentValue = builder.currentValue;
        this.deviation = builder.deviation;
        this.percentageChange = builder.percentageChange;
        this.detectedAt = builder.detectedAt != null ? builder.detectedAt : Instant.now();
        this.resolvedAt = builder.resolvedAt;
        this.possibleCauses = builder.possibleCauses != null ?
                List.copyOf(builder.possibleCauses) : List.of();
        this.status = builder.status != null ? builder.status : AnomalyStatus.ACTIVE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public AnomalyType type() {
        return type;
    }

    public String metric() {
        return metric;
    }

    public String endpoint() {
        return endpoint;
    }

    public String service() {
        return service;
    }

    public double baselineValue() {
        return baselineValue;
    }

    public double currentValue() {
        return currentValue;
    }

    public double deviation() {
        return deviation;
    }

    public double percentageChange() {
        return percentageChange;
    }

    public Instant detectedAt() {
        return detectedAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public List<PossibleCause> possibleCauses() {
        return possibleCauses;
    }

    public AnomalyStatus status() {
        return status;
    }

    public boolean isActive() {
        return status == AnomalyStatus.ACTIVE;
    }

    public boolean isResolved() {
        return status == AnomalyStatus.RESOLVED;
    }

    public boolean isCritical() {
        return type.isCritical();
    }

    /**
     * Returns a formatted string describing the anomaly.
     */
    public String summary() {
        String change = percentageChange >= 0 ? "+" : "";
        return String.format("%s: %.2f -> %.2f (%s%.1f%%)",
                type.displayName(),
                baselineValue,
                currentValue,
                change,
                percentageChange);
    }

    /**
     * Returns a human-readable duration since detection.
     */
    public String durationSinceDetection() {
        long seconds = Instant.now().getEpochSecond() - detectedAt.getEpochSecond();
        if (seconds < 60) return seconds + "s ago";
        if (seconds < 3600) return (seconds / 60) + "m ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Anomaly anomaly = (Anomaly) o;
        return Objects.equals(id, anomaly.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Anomaly{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", currentValue=" + currentValue +
                ", percentageChange=" + percentageChange + "%" +
                ", status=" + status +
                '}';
    }

    public static final class Builder {
        private String id;
        private AnomalyType type;
        private String metric;
        private String endpoint;
        private String service;
        private double baselineValue;
        private double currentValue;
        private double deviation;
        private double percentageChange;
        private Instant detectedAt;
        private Instant resolvedAt;
        private List<PossibleCause> possibleCauses;
        private AnomalyStatus status;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(AnomalyType type) {
            this.type = type;
            return this;
        }

        public Builder metric(String metric) {
            this.metric = metric;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder baselineValue(double baselineValue) {
            this.baselineValue = baselineValue;
            return this;
        }

        public Builder currentValue(double currentValue) {
            this.currentValue = currentValue;
            return this;
        }

        public Builder deviation(double deviation) {
            this.deviation = deviation;
            return this;
        }

        public Builder percentageChange(double percentageChange) {
            this.percentageChange = percentageChange;
            return this;
        }

        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder possibleCauses(List<PossibleCause> possibleCauses) {
            this.possibleCauses = possibleCauses;
            return this;
        }

        public Builder status(AnomalyStatus status) {
            this.status = status;
            return this;
        }

        public Anomaly build() {
            return new Anomaly(this);
        }
    }
}
