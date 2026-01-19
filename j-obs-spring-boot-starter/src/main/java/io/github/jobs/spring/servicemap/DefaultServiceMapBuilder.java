package io.github.jobs.spring.servicemap;

import io.github.jobs.application.ServiceMapBuilder;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.servicemap.ServiceConnection;
import io.github.jobs.domain.servicemap.ServiceConnection.ConnectionStats;
import io.github.jobs.domain.servicemap.ServiceConnection.ConnectionType;
import io.github.jobs.domain.servicemap.ServiceMap;
import io.github.jobs.domain.servicemap.ServiceNode;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceHealth;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceStats;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceType;
import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.SpanKind;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ServiceMapBuilder that extracts service dependencies from traces.
 */
public class DefaultServiceMapBuilder implements ServiceMapBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultServiceMapBuilder.class);

    private final TraceRepository traceRepository;
    private final Config config;

    private volatile ServiceMap cachedMap;
    private volatile Instant cacheTime;

    public DefaultServiceMapBuilder(TraceRepository traceRepository, Config config) {
        this.traceRepository = traceRepository;
        this.config = config != null ? config : Config.defaults();
    }

    @Override
    public ServiceMap build(Duration window) {
        log.debug("Building service map for window: {}", window);

        Instant since = Instant.now().minus(window);
        List<Trace> traces = traceRepository.query(TraceQuery.builder()
                .startTime(since)
                .limit(10000)
                .build());

        if (traces.isEmpty()) {
            log.debug("No traces found for service map");
            return ServiceMap.empty();
        }

        // Collect statistics per node and connection
        Map<String, NodeStatsAccumulator> nodeStats = new HashMap<>();
        Map<String, ConnectionStatsAccumulator> connectionStats = new HashMap<>();

        for (Trace trace : traces) {
            processTrace(trace, nodeStats, connectionStats, window);
        }

        // Build the service map
        ServiceMap.Builder builder = ServiceMap.builder()
                .tracesAnalyzed(traces.size());

        // Add nodes
        for (Map.Entry<String, NodeStatsAccumulator> entry : nodeStats.entrySet()) {
            NodeStatsAccumulator acc = entry.getValue();
            ServiceStats stats = acc.toStats(window);
            ServiceHealth health = stats.calculateHealth(config.errorThreshold(), config.latencyThreshold());

            builder.addNode(new ServiceNode(
                    entry.getKey(),
                    entry.getKey(),
                    acc.type,
                    health,
                    stats
            ));
        }

        // Add connections
        for (Map.Entry<String, ConnectionStatsAccumulator> entry : connectionStats.entrySet()) {
            ConnectionStatsAccumulator acc = entry.getValue();
            ConnectionStats stats = acc.toStats(window);

            builder.addConnection(new ServiceConnection(
                    entry.getKey(),
                    acc.sourceId,
                    acc.targetId,
                    acc.type,
                    stats
            ));
        }

        ServiceMap map = builder.build();
        cachedMap = map;
        cacheTime = Instant.now();

        log.debug("Built service map with {} nodes and {} connections from {} traces",
                map.nodeCount(), map.connectionCount(), traces.size());

        return map;
    }

    @Override
    public ServiceMap getCached() {
        if (cachedMap != null && cacheTime != null) {
            if (Duration.between(cacheTime, Instant.now()).compareTo(config.cacheExpiration()) < 0) {
                return cachedMap;
            }
        }
        return build();
    }

    @Override
    public ServiceMap refresh() {
        cachedMap = null;
        cacheTime = null;
        return build();
    }

    private void processTrace(Trace trace, Map<String, NodeStatsAccumulator> nodeStats,
                              Map<String, ConnectionStatsAccumulator> connectionStats, Duration window) {
        for (Span span : trace.spans()) {
            String serviceName = getServiceName(span);
            if (serviceName == null) {
                continue;
            }

            // Track the service as a node
            ServiceType nodeType = inferNodeType(span);
            nodeStats.computeIfAbsent(serviceName, k -> new NodeStatsAccumulator(nodeType))
                    .record(span);

            // If this is a client span, we have a connection
            if (span.kind() == SpanKind.CLIENT) {
                String targetService = getTargetService(span);
                if (targetService != null && !targetService.equals(serviceName)) {
                    String connectionId = serviceName + "->" + targetService;
                    ConnectionType connType = inferConnectionType(span);

                    // Ensure target node exists
                    ServiceType targetType = inferTargetNodeType(span);
                    nodeStats.computeIfAbsent(targetService, k -> new NodeStatsAccumulator(targetType));

                    connectionStats.computeIfAbsent(connectionId,
                            k -> new ConnectionStatsAccumulator(serviceName, targetService, connType))
                            .record(span);
                }
            }
        }
    }

    private String getServiceName(Span span) {
        if (span.serviceName() != null && !span.serviceName().isEmpty()) {
            return span.serviceName();
        }
        // Try to infer from attributes
        String service = span.attribute("service.name");
        if (service != null) {
            return service;
        }
        return span.attribute("service");
    }

    private String getTargetService(Span span) {
        // Check for peer.service attribute
        String peerService = span.attribute("peer.service");
        if (peerService != null) {
            return peerService;
        }

        // Check for database
        String dbSystem = span.dbSystem();
        if (dbSystem != null) {
            String dbName = span.attribute("db.name");
            return dbName != null ? dbSystem + ":" + dbName : dbSystem;
        }

        // Check for messaging system
        String messagingSystem = span.attribute("messaging.system");
        if (messagingSystem != null) {
            String destination = span.attribute("messaging.destination");
            return destination != null ? messagingSystem + ":" + destination : messagingSystem;
        }

        // Try to extract from URL
        String httpHost = span.attribute("http.host");
        if (httpHost != null) {
            return httpHost;
        }

        String netPeerName = span.attribute("net.peer.name");
        if (netPeerName != null) {
            return netPeerName;
        }

        // Try to extract from URL path
        String url = span.httpUrl();
        if (url != null) {
            return extractHostFromUrl(url);
        }

        return null;
    }

    private String extractHostFromUrl(String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                String withoutProtocol = url.substring(url.indexOf("://") + 3);
                int slashIndex = withoutProtocol.indexOf('/');
                return slashIndex > 0 ? withoutProtocol.substring(0, slashIndex) : withoutProtocol;
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    private ServiceType inferNodeType(Span span) {
        if (span.dbSystem() != null) {
            return ServiceType.DATABASE;
        }
        if (span.attribute("messaging.system") != null) {
            return ServiceType.QUEUE;
        }
        String cacheType = span.attribute("cache.type");
        if (cacheType != null || "redis".equalsIgnoreCase(span.attribute("db.system"))) {
            return ServiceType.CACHE;
        }
        return ServiceType.SERVICE;
    }

    private ServiceType inferTargetNodeType(Span span) {
        if (span.dbSystem() != null) {
            String dbSystem = span.dbSystem().toLowerCase();
            if (dbSystem.contains("redis") || dbSystem.contains("memcache")) {
                return ServiceType.CACHE;
            }
            return ServiceType.DATABASE;
        }
        if (span.attribute("messaging.system") != null) {
            return ServiceType.QUEUE;
        }
        return ServiceType.SERVICE;
    }

    private ConnectionType inferConnectionType(Span span) {
        if (span.dbSystem() != null) {
            String dbSystem = span.dbSystem().toLowerCase();
            if (dbSystem.contains("redis") || dbSystem.contains("memcache")) {
                return ConnectionType.CACHE;
            }
            return ConnectionType.DATABASE;
        }
        if (span.attribute("messaging.system") != null) {
            return ConnectionType.QUEUE;
        }
        if (span.attribute("rpc.system") != null) {
            String rpcSystem = span.attribute("rpc.system").toLowerCase();
            if (rpcSystem.contains("grpc")) {
                return ConnectionType.GRPC;
            }
        }
        return ConnectionType.HTTP;
    }

    /**
     * Accumulator for node statistics.
     */
    private static class NodeStatsAccumulator {
        final ServiceType type;
        long totalRequests = 0;
        long totalErrors = 0;
        long totalDurationMs = 0;
        long[] durations = new long[1000];
        int durationIndex = 0;

        NodeStatsAccumulator(ServiceType type) {
            this.type = type;
        }

        void record(Span span) {
            totalRequests++;
            if (span.hasError()) {
                totalErrors++;
            }
            long duration = span.durationMs();
            totalDurationMs += duration;
            if (durationIndex < durations.length) {
                durations[durationIndex++] = duration;
            }
        }

        ServiceStats toStats(Duration window) {
            double windowSeconds = window.toSeconds();
            double rps = windowSeconds > 0 ? totalRequests / windowSeconds : 0;
            double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
            double avgLatency = totalRequests > 0 ? (double) totalDurationMs / totalRequests : 0;
            double p99Latency = calculateP99();

            return new ServiceStats(rps, errorRate, avgLatency, p99Latency, totalRequests, totalErrors);
        }

        private double calculateP99() {
            if (durationIndex == 0) {
                return 0;
            }
            long[] sorted = Arrays.copyOf(durations, durationIndex);
            Arrays.sort(sorted);
            int p99Index = (int) Math.ceil(sorted.length * 0.99) - 1;
            return sorted[Math.max(0, Math.min(p99Index, sorted.length - 1))];
        }
    }

    /**
     * Accumulator for connection statistics.
     */
    private static class ConnectionStatsAccumulator {
        final String sourceId;
        final String targetId;
        final ConnectionType type;
        long totalRequests = 0;
        long totalErrors = 0;
        long totalDurationMs = 0;
        long[] durations = new long[1000];
        int durationIndex = 0;

        ConnectionStatsAccumulator(String sourceId, String targetId, ConnectionType type) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.type = type;
        }

        void record(Span span) {
            totalRequests++;
            if (span.hasError()) {
                totalErrors++;
            }
            long duration = span.durationMs();
            totalDurationMs += duration;
            if (durationIndex < durations.length) {
                durations[durationIndex++] = duration;
            }
        }

        ConnectionStats toStats(Duration window) {
            double windowSeconds = window.toSeconds();
            double rps = windowSeconds > 0 ? totalRequests / windowSeconds : 0;
            double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
            double avgLatency = totalRequests > 0 ? (double) totalDurationMs / totalRequests : 0;
            double p99Latency = calculateP99();

            return new ConnectionStats(rps, errorRate, avgLatency, p99Latency, totalRequests, totalErrors);
        }

        private double calculateP99() {
            if (durationIndex == 0) {
                return 0;
            }
            long[] sorted = Arrays.copyOf(durations, durationIndex);
            Arrays.sort(sorted);
            int p99Index = (int) Math.ceil(sorted.length * 0.99) - 1;
            return sorted[Math.max(0, Math.min(p99Index, sorted.length - 1))];
        }
    }
}
