package io.github.jobs.domain.anomaly;

import java.util.Objects;

/**
 * Represents a possible cause for a detected anomaly.
 */
public record PossibleCause(
        String description,
        CauseType type,
        Confidence confidence,
        String details
) {

    public PossibleCause {
        Objects.requireNonNull(description, "description is required");
        if (type == null) type = CauseType.UNKNOWN;
        if (confidence == null) confidence = Confidence.LOW;
    }

    public PossibleCause(String description, Confidence confidence) {
        this(description, CauseType.UNKNOWN, confidence, null);
    }

    public PossibleCause(String description, CauseType type, Confidence confidence) {
        this(description, type, confidence, null);
    }

    /**
     * Types of possible causes.
     */
    public enum CauseType {
        DEPENDENCY_DEGRADATION("Dependency Degradation"),
        RECENT_DEPLOY("Recent Deploy"),
        SLOW_QUERIES("Slow Database Queries"),
        TRAFFIC_CHANGE("Traffic Change"),
        RESOURCE_EXHAUSTION("Resource Exhaustion"),
        EXTERNAL_SERVICE("External Service Issue"),
        CONFIGURATION_CHANGE("Configuration Change"),
        UNKNOWN("Unknown");

        private final String displayName;

        CauseType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * Confidence level for the identified cause.
     */
    public enum Confidence {
        HIGH("High", "text-green-600", 0.8),
        MEDIUM("Medium", "text-amber-600", 0.5),
        LOW("Low", "text-gray-600", 0.2);

        private final String displayName;
        private final String cssClass;
        private final double threshold;

        Confidence(String displayName, String cssClass, double threshold) {
            this.displayName = displayName;
            this.cssClass = cssClass;
            this.threshold = threshold;
        }

        public String displayName() {
            return displayName;
        }

        public String cssClass() {
            return cssClass;
        }

        public double threshold() {
            return threshold;
        }

        public static Confidence fromScore(double score) {
            if (score >= HIGH.threshold) return HIGH;
            if (score >= MEDIUM.threshold) return MEDIUM;
            return LOW;
        }
    }

    public boolean isHighConfidence() {
        return confidence == Confidence.HIGH;
    }

    public boolean isLikelyCause() {
        return confidence == Confidence.HIGH || confidence == Confidence.MEDIUM;
    }
}
