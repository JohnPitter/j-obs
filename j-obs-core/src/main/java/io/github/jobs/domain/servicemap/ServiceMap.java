package io.github.jobs.domain.servicemap;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the service dependency map built from trace data.
 */
public class ServiceMap {

    private final Map<String, ServiceNode> nodes;
    private final Map<String, ServiceConnection> connections;
    private final Instant generatedAt;
    private final long tracesAnalyzed;

    public ServiceMap(
            Collection<ServiceNode> nodes,
            Collection<ServiceConnection> connections,
            long tracesAnalyzed
    ) {
        Objects.requireNonNull(nodes, "nodes cannot be null");
        Objects.requireNonNull(connections, "connections cannot be null");

        this.nodes = nodes.stream()
                .collect(Collectors.toMap(ServiceNode::id, n -> n, (a, b) -> a));
        this.connections = connections.stream()
                .collect(Collectors.toMap(ServiceConnection::id, c -> c, (a, b) -> a));
        this.generatedAt = Instant.now();
        this.tracesAnalyzed = tracesAnalyzed;
    }

    /**
     * Creates an empty service map.
     */
    public static ServiceMap empty() {
        return new ServiceMap(List.of(), List.of(), 0);
    }

    /**
     * Creates a builder for constructing a service map.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns all nodes in the map.
     */
    public Collection<ServiceNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Returns all connections in the map.
     */
    public Collection<ServiceConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    /**
     * Finds a node by ID.
     */
    public Optional<ServiceNode> getNode(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    /**
     * Finds a connection by ID.
     */
    public Optional<ServiceConnection> getConnection(String id) {
        return Optional.ofNullable(connections.get(id));
    }

    /**
     * Returns connections from a specific source node.
     */
    public List<ServiceConnection> getOutgoingConnections(String nodeId) {
        return connections.values().stream()
                .filter(c -> c.sourceId().equals(nodeId))
                .toList();
    }

    /**
     * Returns connections to a specific target node.
     */
    public List<ServiceConnection> getIncomingConnections(String nodeId) {
        return connections.values().stream()
                .filter(c -> c.targetId().equals(nodeId))
                .toList();
    }

    /**
     * Returns when the map was generated.
     */
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Returns the number of traces analyzed to build this map.
     */
    public long getTracesAnalyzed() {
        return tracesAnalyzed;
    }

    /**
     * Returns the total number of nodes.
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Returns the total number of connections.
     */
    public int connectionCount() {
        return connections.size();
    }

    /**
     * Returns nodes that have issues (degraded or unhealthy).
     */
    public List<ServiceNode> getNodesWithIssues() {
        return nodes.values().stream()
                .filter(n -> n.health().needsAttention())
                .toList();
    }

    /**
     * Returns the maximum requests per second across all connections.
     */
    public double getMaxRequestsPerSecond() {
        return connections.values().stream()
                .mapToDouble(c -> c.stats().requestsPerSecond())
                .max()
                .orElse(0);
    }

    /**
     * Returns statistics about the service map.
     */
    public ServiceMapStats getStats() {
        long healthyNodes = nodes.values().stream()
                .filter(n -> n.health().isHealthy())
                .count();
        long degradedNodes = nodes.values().stream()
                .filter(n -> n.health() == ServiceNode.ServiceHealth.DEGRADED)
                .count();
        long unhealthyNodes = nodes.values().stream()
                .filter(n -> n.health() == ServiceNode.ServiceHealth.UNHEALTHY)
                .count();

        double totalRps = connections.values().stream()
                .mapToDouble(c -> c.stats().requestsPerSecond())
                .sum();
        double avgErrorRate = connections.values().stream()
                .mapToDouble(c -> c.stats().errorRate())
                .average()
                .orElse(0);

        return new ServiceMapStats(
                nodeCount(),
                connectionCount(),
                healthyNodes,
                degradedNodes,
                unhealthyNodes,
                totalRps,
                avgErrorRate,
                tracesAnalyzed,
                generatedAt
        );
    }

    /**
     * Statistics about the service map.
     */
    public record ServiceMapStats(
            int totalNodes,
            int totalConnections,
            long healthyNodes,
            long degradedNodes,
            long unhealthyNodes,
            double totalRequestsPerSecond,
            double averageErrorRate,
            long tracesAnalyzed,
            Instant generatedAt
    ) {}

    /**
     * Builder for constructing a service map.
     */
    public static class Builder {
        private final Map<String, ServiceNode> nodes = new HashMap<>();
        private final Map<String, ServiceConnection> connections = new HashMap<>();
        private long tracesAnalyzed = 0;

        public Builder addNode(ServiceNode node) {
            nodes.put(node.id(), node);
            return this;
        }

        public Builder addConnection(ServiceConnection connection) {
            connections.put(connection.id(), connection);
            return this;
        }

        public Builder tracesAnalyzed(long count) {
            this.tracesAnalyzed = count;
            return this;
        }

        public ServiceMap build() {
            return new ServiceMap(nodes.values(), connections.values(), tracesAnalyzed);
        }
    }
}
