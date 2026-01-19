package io.github.jobs.domain.servicemap;

import java.util.Objects;

/**
 * Represents a service node in the service map.
 */
public record ServiceNode(
        String id,
        String name,
        ServiceType type,
        ServiceHealth health,
        ServiceStats stats
) {
    public ServiceNode {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (type == null) {
            type = ServiceType.SERVICE;
        }
        if (health == null) {
            health = ServiceHealth.UNKNOWN;
        }
        if (stats == null) {
            stats = ServiceStats.empty();
        }
    }

    /**
     * Creates a service node with just name.
     */
    public static ServiceNode of(String name) {
        return new ServiceNode(name, name, ServiceType.SERVICE, ServiceHealth.UNKNOWN, ServiceStats.empty());
    }

    /**
     * Creates a service node with name and type.
     */
    public static ServiceNode of(String name, ServiceType type) {
        return new ServiceNode(name, name, type, ServiceHealth.UNKNOWN, ServiceStats.empty());
    }

    /**
     * Types of service nodes.
     */
    public enum ServiceType {
        SERVICE("Service", "rectangle", "#3B82F6"),
        DATABASE("Database", "cylinder", "#10B981"),
        CACHE("Cache", "diamond", "#F59E0B"),
        QUEUE("Queue", "parallelogram", "#8B5CF6"),
        EXTERNAL("External", "cloud", "#6B7280"),
        CLIENT("Client", "ellipse", "#EC4899");

        private final String displayName;
        private final String shape;
        private final String color;

        ServiceType(String displayName, String shape, String color) {
            this.displayName = displayName;
            this.shape = shape;
            this.color = color;
        }

        public String displayName() {
            return displayName;
        }

        public String shape() {
            return shape;
        }

        public String color() {
            return color;
        }
    }

    /**
     * Health status of a service.
     */
    public enum ServiceHealth {
        HEALTHY("Healthy", "text-green-600", "bg-green-100", "#10B981"),
        DEGRADED("Degraded", "text-amber-600", "bg-amber-100", "#F59E0B"),
        UNHEALTHY("Unhealthy", "text-red-600", "bg-red-100", "#EF4444"),
        UNKNOWN("Unknown", "text-gray-600", "bg-gray-100", "#6B7280");

        private final String displayName;
        private final String textCssClass;
        private final String bgCssClass;
        private final String color;

        ServiceHealth(String displayName, String textCssClass, String bgCssClass, String color) {
            this.displayName = displayName;
            this.textCssClass = textCssClass;
            this.bgCssClass = bgCssClass;
            this.color = color;
        }

        public String displayName() {
            return displayName;
        }

        public String textCssClass() {
            return textCssClass;
        }

        public String bgCssClass() {
            return bgCssClass;
        }

        public String color() {
            return color;
        }

        public boolean isHealthy() {
            return this == HEALTHY;
        }

        public boolean needsAttention() {
            return this == DEGRADED || this == UNHEALTHY;
        }
    }

    /**
     * Statistics for a service node.
     */
    public record ServiceStats(
            double requestsPerSecond,
            double errorRate,
            double avgLatencyMs,
            double p99LatencyMs,
            long totalRequests,
            long totalErrors
    ) {
        public static ServiceStats empty() {
            return new ServiceStats(0, 0, 0, 0, 0, 0);
        }

        /**
         * Determines health based on error rate and latency.
         */
        public ServiceHealth calculateHealth(double errorThreshold, double latencyThreshold) {
            if (errorRate > errorThreshold) {
                return ServiceHealth.UNHEALTHY;
            }
            if (p99LatencyMs > latencyThreshold || errorRate > errorThreshold / 2) {
                return ServiceHealth.DEGRADED;
            }
            if (totalRequests == 0) {
                return ServiceHealth.UNKNOWN;
            }
            return ServiceHealth.HEALTHY;
        }
    }
}
