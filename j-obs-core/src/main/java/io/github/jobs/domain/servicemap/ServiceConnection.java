package io.github.jobs.domain.servicemap;

import java.util.Objects;

/**
 * Represents a connection between two services in the service map.
 */
public record ServiceConnection(
        String id,
        String sourceId,
        String targetId,
        ConnectionType type,
        ConnectionStats stats
) {
    public ServiceConnection {
        Objects.requireNonNull(sourceId, "sourceId cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");
        if (id == null) {
            id = sourceId + "->" + targetId;
        }
        if (type == null) {
            type = ConnectionType.HTTP;
        }
        if (stats == null) {
            stats = ConnectionStats.empty();
        }
    }

    /**
     * Creates a simple connection between two services.
     */
    public static ServiceConnection of(String sourceId, String targetId) {
        return new ServiceConnection(null, sourceId, targetId, ConnectionType.HTTP, ConnectionStats.empty());
    }

    /**
     * Creates a connection with type.
     */
    public static ServiceConnection of(String sourceId, String targetId, ConnectionType type) {
        return new ServiceConnection(null, sourceId, targetId, type, ConnectionStats.empty());
    }

    /**
     * Types of connections between services.
     */
    public enum ConnectionType {
        HTTP("HTTP", "solid", "#3B82F6"),
        GRPC("gRPC", "solid", "#10B981"),
        DATABASE("Database", "dashed", "#F59E0B"),
        CACHE("Cache", "dotted", "#8B5CF6"),
        QUEUE("Queue", "dashed", "#EC4899"),
        WEBSOCKET("WebSocket", "solid", "#06B6D4");

        private final String displayName;
        private final String lineStyle;
        private final String color;

        ConnectionType(String displayName, String lineStyle, String color) {
            this.displayName = displayName;
            this.lineStyle = lineStyle;
            this.color = color;
        }

        public String displayName() {
            return displayName;
        }

        public String lineStyle() {
            return lineStyle;
        }

        public String color() {
            return color;
        }
    }

    /**
     * Statistics for a connection.
     */
    public record ConnectionStats(
            double requestsPerSecond,
            double errorRate,
            double avgLatencyMs,
            double p99LatencyMs,
            long totalRequests,
            long totalErrors
    ) {
        public static ConnectionStats empty() {
            return new ConnectionStats(0, 0, 0, 0, 0, 0);
        }

        /**
         * Determines connection health based on error rate.
         */
        public ServiceNode.ServiceHealth calculateHealth(double errorThreshold) {
            if (errorRate > errorThreshold) {
                return ServiceNode.ServiceHealth.UNHEALTHY;
            }
            if (errorRate > errorThreshold / 2) {
                return ServiceNode.ServiceHealth.DEGRADED;
            }
            if (totalRequests == 0) {
                return ServiceNode.ServiceHealth.UNKNOWN;
            }
            return ServiceNode.ServiceHealth.HEALTHY;
        }

        /**
         * Calculates line thickness based on request volume.
         */
        public int lineThickness(double maxRequestsPerSecond) {
            if (maxRequestsPerSecond <= 0 || requestsPerSecond <= 0) {
                return 1;
            }
            double ratio = requestsPerSecond / maxRequestsPerSecond;
            return Math.max(1, Math.min(8, (int) (ratio * 8)));
        }
    }
}
