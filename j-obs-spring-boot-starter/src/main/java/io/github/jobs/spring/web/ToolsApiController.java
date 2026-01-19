package io.github.jobs.spring.web;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Trace;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for developer tools.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/tools")
public class ToolsApiController {

    private final TraceRepository traceRepository;

    public ToolsApiController(TraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    // ==================== Service Map ====================

    @GetMapping(value = "/service-map", produces = MediaType.APPLICATION_JSON_VALUE)
    public ServiceMapResponse getServiceMap() {
        // Build service map from traces
        Map<String, ServiceNode> nodes = new LinkedHashMap<>();
        Map<String, ConnectionStats> connections = new HashMap<>();

        // Add self service as root
        String selfService = System.getProperty("spring.application.name", "application");
        nodes.put(selfService, new ServiceNode(selfService, "Application", "application"));

        // Analyze traces for dependencies
        List<Trace> recentTraces = traceRepository.recent(1000);

        for (Trace trace : recentTraces) {
            for (var span : trace.spans()) {
                String namespace = span.attributes().get("code.namespace");
                String function = span.attributes().get("code.function");

                if (namespace == null) continue;

                // Detect Database operations
                if (namespace.contains("Repository") ||
                    (function != null && (function.equals("save") || function.equals("findById") ||
                     function.equals("findAll") || function.equals("updateStatus") || function.equals("delete")))) {
                    addOrUpdateNode(nodes, "database", "Database", "database", span.durationMs());
                    addConnection(connections, selfService, "database", span.durationMs());
                }

                // Detect Payment Service
                if (namespace.contains("PaymentService") || span.attributes().containsKey("payment.provider")) {
                    String provider = span.attributes().getOrDefault("payment.provider", "gateway");
                    addOrUpdateNode(nodes, "payment-" + provider, "Payment Gateway (" + provider + ")", "payment", span.durationMs());
                    addConnection(connections, selfService, "payment-" + provider, span.durationMs());
                }

                // Detect Notification Service
                if (namespace.contains("NotificationService") || span.attributes().containsKey("notification.channel")) {
                    String channel = span.attributes().get("notification.channel");
                    if (channel != null) {
                        addOrUpdateNode(nodes, "notification-" + channel, capitalize(channel) + " Notifications", "notification", span.durationMs());
                        addConnection(connections, selfService, "notification-" + channel, span.durationMs());
                    }
                }

                // Detect Inventory Service
                if (namespace.contains("InventoryService")) {
                    addOrUpdateNode(nodes, "inventory", "Inventory Service", "service", span.durationMs());
                    addConnection(connections, selfService, "inventory", span.durationMs());
                }

                // Detect external HTTP calls
                if (span.attributes().containsKey("http.url")) {
                    String url = span.attributes().get("http.url");
                    String host = extractHost(url);
                    addOrUpdateNode(nodes, "http-" + host, host, "http", span.durationMs());
                    addConnection(connections, selfService, "http-" + host, span.durationMs());
                }

                // Detect messaging
                if (span.attributes().containsKey("messaging.system")) {
                    String system = span.attributes().get("messaging.system");
                    addOrUpdateNode(nodes, "mq-" + system, system, "messaging", span.durationMs());
                    addConnection(connections, selfService, "mq-" + system, span.durationMs());
                }

                // Detect cache operations
                if (namespace.contains("Cache") || span.name().toLowerCase().contains("cache")) {
                    addOrUpdateNode(nodes, "cache", "Cache", "cache", span.durationMs());
                    addConnection(connections, selfService, "cache", span.durationMs());
                }
            }
        }

        // Build response
        List<ServiceInfo> services = nodes.values().stream()
                .map(node -> new ServiceInfo(
                        node.id,
                        node.name,
                        node.type,
                        true,
                        node.requestCount > 0 ? node.totalLatency / node.requestCount : 0,
                        recentTraces.size() > 0 ? (double) node.requestCount / recentTraces.size() : 0
                ))
                .collect(Collectors.toList());

        List<ServiceConnection> connectionList = connections.values().stream()
                .map(conn -> new ServiceConnection(
                        conn.from,
                        conn.to,
                        conn.requestCount,
                        conn.requestCount > 0 ? conn.totalLatency / conn.requestCount : 0,
                        conn.errorCount
                ))
                .collect(Collectors.toList());

        return new ServiceMapResponse(services, connectionList);
    }

    private void addOrUpdateNode(Map<String, ServiceNode> nodes, String id, String name, String type, long latencyMs) {
        nodes.compute(id, (k, existing) -> {
            if (existing == null) {
                return new ServiceNode(id, name, type, 1, latencyMs);
            }
            existing.requestCount++;
            existing.totalLatency += latencyMs;
            return existing;
        });
    }

    private void addConnection(Map<String, ConnectionStats> connections, String from, String to, long latencyMs) {
        String key = from + "->" + to;
        connections.compute(key, (k, existing) -> {
            if (existing == null) {
                return new ConnectionStats(from, to, 1, latencyMs, 0);
            }
            existing.requestCount++;
            existing.totalLatency += latencyMs;
            return existing;
        });
    }

    private String extractHost(String url) {
        try {
            URL u = new URL(url);
            return u.getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    // Helper classes for building service map
    private static class ServiceNode {
        String id;
        String name;
        String type;
        int requestCount;
        long totalLatency;

        ServiceNode(String id, String name, String type) {
            this(id, name, type, 0, 0);
        }

        ServiceNode(String id, String name, String type, int requestCount, long totalLatency) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.requestCount = requestCount;
            this.totalLatency = totalLatency;
        }
    }

    private static class ConnectionStats {
        String from;
        String to;
        int requestCount;
        long totalLatency;
        int errorCount;

        ConnectionStats(String from, String to, int requestCount, long totalLatency, int errorCount) {
            this.from = from;
            this.to = to;
            this.requestCount = requestCount;
            this.totalLatency = totalLatency;
            this.errorCount = errorCount;
        }
    }

    // ==================== Profiling ====================

    @GetMapping(value = "/profiling/memory", produces = MediaType.APPLICATION_JSON_VALUE)
    public MemoryStatsResponse getMemoryStats() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        long usedMb = heapUsage.getUsed() / (1024 * 1024);
        long maxMb = heapUsage.getMax() / (1024 * 1024);
        int percent = maxMb > 0 ? (int) ((usedMb * 100) / maxMb) : 0;

        // Get GC stats
        long totalGcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionTime() > 0) {
                totalGcTime += gc.getCollectionTime();
            }
        }

        return new MemoryStatsResponse(
                usedMb + " MB / " + maxMb + " MB",
                percent,
                totalGcTime + "ms total"
        );
    }

    @PostMapping(value = "/profiling/gc", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> triggerGc() {
        System.gc();
        return Map.of("status", "requested", "timestamp", Instant.now().toString());
    }

    @PostMapping(value = "/profiling/heap-dump", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> triggerHeapDump() {
        // Note: Actual heap dump requires special permissions and HotSpotDiagnosticMXBean
        // This is a placeholder that returns a message
        return Map.of(
                "status", "not_available",
                "message", "Heap dump requires HotSpotDiagnosticMXBean access. Use jcmd or jmap instead."
        );
    }

    @GetMapping(value = "/profiling/threads", produces = MediaType.APPLICATION_JSON_VALUE)
    public ThreadDumpResponse getThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadBean.dumpAllThreads(false, false);

        List<ThreadInfoDto> threads = Arrays.stream(threadInfos)
                .map(info -> new ThreadInfoDto(
                        info.getThreadId(),
                        info.getThreadName(),
                        info.getThreadState().name()
                ))
                .sorted(Comparator.comparing(ThreadInfoDto::name))
                .collect(Collectors.toList());

        return new ThreadDumpResponse(threads);
    }

    @PostMapping(value = "/profiling/cpu/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> startCpuProfile(@RequestBody(required = false) Map<String, Object> params) {
        // CPU profiling would require async-profiler or similar
        // This is a placeholder
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        return Map.of(
                "sessionId", sessionId,
                "status", "started",
                "message", "CPU profiling requires async-profiler integration"
        );
    }

    @GetMapping(value = "/profiling/cpu/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getCpuProfileResult(@PathVariable String sessionId) {
        // Placeholder response
        return Map.of(
                "sessionId", sessionId,
                "status", "completed",
                "topMethods", List.of(
                        Map.of("name", "java.lang.Thread.sleep", "percent", 15.2),
                        Map.of("name", "sun.nio.ch.SocketDispatcher.read", "percent", 12.5),
                        Map.of("name", "java.util.HashMap.get", "percent", 8.3)
                ),
                "message", "Sample data - integrate async-profiler for real profiling"
        );
    }

    // ==================== SLOs ====================

    @GetMapping(value = "/slos", produces = MediaType.APPLICATION_JSON_VALUE)
    public SloListResponse getSlos() {
        // Return configured SLOs - for now return sample data
        List<SloDto> slos = List.of(
                new SloDto(
                        "api-availability",
                        "API Availability",
                        "API must be available 99.9% of the time",
                        99.9,
                        99.95,
                        78.0,
                        "23.4h",
                        "healthy"
                ),
                new SloDto(
                        "api-latency",
                        "API Latency (p99)",
                        "99% of requests must complete under 200ms",
                        99.0,
                        99.5,
                        95.0,
                        "28.5h",
                        "healthy"
                )
        );
        return new SloListResponse(slos);
    }

    // ==================== Anomaly Detection ====================

    @GetMapping(value = "/anomalies", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnomalyListResponse getAnomalies(@RequestParam(defaultValue = "24h") String range) {
        // For now return empty - anomaly detection requires historical data analysis
        List<AnomalyDto> anomalies = new ArrayList<>();

        // Check for basic anomalies based on current metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        int heapPercent = heapUsage.getMax() > 0 ?
                (int) ((heapUsage.getUsed() * 100) / heapUsage.getMax()) : 0;

        if (heapPercent > 85) {
            anomalies.add(new AnomalyDto(
                    "heap-" + System.currentTimeMillis(),
                    "High Memory Usage",
                    "Memory usage is above 85%",
                    "Memory Spike",
                    "warning",
                    Instant.now().toString(),
                    "< 70%",
                    heapPercent + "%",
                    "+" + (heapPercent - 70) + "%",
                    List.of("Memory leak", "Increased load", "Missing GC")
            ));
        }

        return new AnomalyListResponse(anomalies);
    }

    // ==================== SQL Analyzer ====================

    @PostMapping(value = "/sql/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public SqlAnalysisResponse analyzeSql() {
        List<SqlProblemDto> problems = new ArrayList<>();

        // Analyze traces for SQL patterns (limit to recent 1000)
        traceRepository.recent(1000).forEach(trace -> {
            trace.spans().stream()
                    .filter(span -> span.attributes().containsKey("db.statement"))
                    .forEach(span -> {
                        String statement = span.attributes().get("db.statement");
                        long durationMs = span.durationMs();

                        // Check for slow queries
                        if (durationMs > 1000) {
                            problems.add(new SqlProblemDto(
                                    "slow-" + span.spanId(),
                                    "Slow Query",
                                    "warning",
                                    trace.name(),
                                    truncateSql(statement),
                                    durationMs + "ms",
                                    "1",
                                    "Consider adding an index or optimizing the query"
                            ));
                        }

                        // Check for SELECT *
                        if (statement != null && statement.toUpperCase().contains("SELECT *")) {
                            problems.add(new SqlProblemDto(
                                    "select-star-" + span.spanId(),
                                    "SELECT *",
                                    "warning",
                                    trace.name(),
                                    truncateSql(statement),
                                    durationMs + "ms",
                                    "1",
                                    "Select only the columns you need"
                            ));
                        }
                    });
        });

        return new SqlAnalysisResponse(problems);
    }

    private String truncateSql(String sql) {
        if (sql == null) return "";
        return sql.length() > 200 ? sql.substring(0, 200) + "..." : sql;
    }

    // ==================== DTOs ====================

    public record ServiceMapResponse(List<ServiceInfo> services, List<ServiceConnection> connections) {}

    public record ServiceInfo(
            String id,
            String name,
            String type,
            boolean healthy,
            long avgLatencyMs,
            double requestsPerTrace
    ) {}

    public record ServiceConnection(
            String from,
            String to,
            int requestCount,
            long avgLatencyMs,
            int errorCount
    ) {}

    public record MemoryStatsResponse(String heapUsed, int heapPercent, String gcPauses) {}

    public record ThreadDumpResponse(List<ThreadInfoDto> threads) {}

    public record ThreadInfoDto(long id, String name, String state) {}

    public record SloListResponse(List<SloDto> slos) {}

    public record SloDto(
            String id,
            String name,
            String description,
            double target,
            double current,
            double errorBudget,
            String errorBudgetRemaining,
            String status
    ) {}

    public record AnomalyListResponse(List<AnomalyDto> anomalies) {}

    public record AnomalyDto(
            String id,
            String title,
            String description,
            String type,
            String severity,
            String timestamp,
            String normalValue,
            String actualValue,
            String deviation,
            List<String> possibleCauses
    ) {}

    public record SqlAnalysisResponse(List<SqlProblemDto> problems) {}

    public record SqlProblemDto(
            String id,
            String type,
            String severity,
            String endpoint,
            String query,
            String duration,
            String executions,
            String suggestion
    ) {}
}
