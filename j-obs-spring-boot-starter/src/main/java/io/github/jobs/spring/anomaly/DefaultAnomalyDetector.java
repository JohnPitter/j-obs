package io.github.jobs.spring.anomaly;

import io.github.jobs.application.AnomalyDetector;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.anomaly.*;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of AnomalyDetector using statistical analysis.
 */
public class DefaultAnomalyDetector implements AnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(DefaultAnomalyDetector.class);

    private final TraceRepository traceRepository;
    private final AnomalyDetectionConfig config;
    private final Map<String, Anomaly> detectedAnomalies = new ConcurrentHashMap<>();
    private final Map<String, BaselineStats> baselineCache = new ConcurrentHashMap<>();

    private Instant lastDetectionRun;
    private Duration lastDetectionDuration = Duration.ZERO;

    public DefaultAnomalyDetector(TraceRepository traceRepository, AnomalyDetectionConfig config) {
        this.traceRepository = traceRepository;
        this.config = config;
    }

    @Override
    public List<Anomaly> detect(Duration window) {
        Instant start = Instant.now();
        List<Anomaly> newAnomalies = new ArrayList<>();

        try {
            // Get recent traces for analysis
            Instant since = Instant.now().minus(window);
            List<Trace> recentTraces = traceRepository.query(TraceQuery.builder()
                    .startTime(since)
                    .limit(1000)
                    .build());

            if (recentTraces.isEmpty()) {
                log.debug("No traces to analyze for anomalies");
                return newAnomalies;
            }

            // Calculate current metrics
            MetricSnapshot currentSnapshot = calculateMetrics(recentTraces);

            // Get baseline metrics
            BaselineStats latencyBaseline = getOrCalculateBaseline("latency_p99", window);
            BaselineStats errorRateBaseline = getOrCalculateBaseline("error_rate", window);
            BaselineStats trafficBaseline = getOrCalculateBaseline("request_count", window);

            // Detect latency anomalies
            if (latencyBaseline != null && latencyBaseline.sampleCount() >= config.getMinSamplesForBaseline()) {
                detectLatencyAnomaly(currentSnapshot, latencyBaseline, newAnomalies);
            }

            // Detect error rate anomalies
            if (errorRateBaseline != null && errorRateBaseline.sampleCount() >= config.getMinSamplesForBaseline()) {
                detectErrorRateAnomaly(currentSnapshot, errorRateBaseline, newAnomalies);
            }

            // Detect traffic anomalies
            if (trafficBaseline != null && trafficBaseline.sampleCount() >= config.getMinSamplesForBaseline()) {
                detectTrafficAnomaly(currentSnapshot, trafficBaseline, newAnomalies);
            }

            // Detect per-endpoint anomalies
            detectEndpointAnomalies(recentTraces, newAnomalies);

            // Store new anomalies
            for (Anomaly anomaly : newAnomalies) {
                detectedAnomalies.put(anomaly.id(), anomaly);
            }

            // Auto-resolve old anomalies that are no longer occurring
            autoResolveAnomalies(currentSnapshot);

        } catch (Exception e) {
            log.error("Error during anomaly detection", e);
        } finally {
            lastDetectionRun = Instant.now();
            lastDetectionDuration = Duration.between(start, lastDetectionRun);
        }

        return newAnomalies;
    }

    private MetricSnapshot calculateMetrics(List<Trace> traces) {
        if (traces.isEmpty()) {
            return new MetricSnapshot(0, 0, 0, 0, 0, Map.of());
        }

        long[] durations = traces.stream()
                .mapToLong(Trace::durationMs)
                .sorted()
                .toArray();

        long errorCount = traces.stream()
                .filter(Trace::hasError)
                .count();

        double avgLatency = Arrays.stream(durations).average().orElse(0);
        double p99Latency = percentile(durations, 99);
        double errorRate = (double) errorCount / traces.size() * 100;

        // Group by endpoint
        Map<String, List<Trace>> byEndpoint = traces.stream()
                .filter(t -> t.httpMethod() != null && t.httpUrl() != null)
                .collect(Collectors.groupingBy(t -> t.httpMethod() + " " + normalizeUrl(t.httpUrl())));

        Map<String, EndpointMetrics> endpointMetrics = new HashMap<>();
        for (Map.Entry<String, List<Trace>> entry : byEndpoint.entrySet()) {
            List<Trace> endpointTraces = entry.getValue();
            long[] epDurations = endpointTraces.stream()
                    .mapToLong(Trace::durationMs)
                    .sorted()
                    .toArray();
            long epErrors = endpointTraces.stream().filter(Trace::hasError).count();

            endpointMetrics.put(entry.getKey(), new EndpointMetrics(
                    entry.getKey(),
                    endpointTraces.size(),
                    Arrays.stream(epDurations).average().orElse(0),
                    percentile(epDurations, 99),
                    (double) epErrors / endpointTraces.size() * 100
            ));
        }

        return new MetricSnapshot(
                traces.size(),
                avgLatency,
                p99Latency,
                errorRate,
                errorCount,
                endpointMetrics
        );
    }

    private String normalizeUrl(String url) {
        if (url == null) return "/";
        // Remove query params and normalize IDs
        return url.replaceAll("\\?.*", "")
                .replaceAll("/\\d+", "/{id}")
                .replaceAll("/[a-f0-9-]{36}", "/{uuid}");
    }

    private BaselineStats getOrCalculateBaseline(String metric, Duration window) {
        BaselineStats cached = baselineCache.get(metric);
        if (cached != null && cached.calculatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(5)))) {
            return cached;
        }

        // Calculate baseline from historical data
        Instant baselineStart = Instant.now().minus(config.getBaselineWindow());
        Instant baselineEnd = Instant.now().minus(window); // Exclude recent window

        List<Trace> baselineTraces = traceRepository.query(TraceQuery.builder()
                .startTime(baselineStart)
                .endTime(baselineEnd)
                .limit(10000)
                .build());

        if (baselineTraces.size() < config.getMinSamplesForBaseline()) {
            log.debug("Not enough samples for baseline calculation: {} < {}",
                    baselineTraces.size(), config.getMinSamplesForBaseline());
            return null;
        }

        double[] values = extractMetricValues(baselineTraces, metric);
        if (values.length == 0) return null;

        double mean = Arrays.stream(values).average().orElse(0);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        BaselineStats baseline = new BaselineStats(
                metric,
                mean,
                stdDev,
                Arrays.stream(values).min().orElse(0),
                Arrays.stream(values).max().orElse(0),
                values.length,
                Instant.now()
        );

        baselineCache.put(metric, baseline);
        return baseline;
    }

    private double[] extractMetricValues(List<Trace> traces, String metric) {
        return switch (metric) {
            case "latency_p99" -> {
                // Calculate p99 for each time bucket (1 minute)
                Map<Long, List<Trace>> buckets = traces.stream()
                        .collect(Collectors.groupingBy(t -> t.startTime().toEpochMilli() / 60000));
                yield buckets.values().stream()
                        .mapToDouble(bucket -> {
                            long[] durations = bucket.stream()
                                    .mapToLong(Trace::durationMs)
                                    .sorted()
                                    .toArray();
                            return percentile(durations, 99);
                        })
                        .toArray();
            }
            case "error_rate" -> {
                Map<Long, List<Trace>> buckets = traces.stream()
                        .collect(Collectors.groupingBy(t -> t.startTime().toEpochMilli() / 60000));
                yield buckets.values().stream()
                        .mapToDouble(bucket -> {
                            long errors = bucket.stream().filter(Trace::hasError).count();
                            return (double) errors / bucket.size() * 100;
                        })
                        .toArray();
            }
            case "request_count" -> {
                Map<Long, List<Trace>> buckets = traces.stream()
                        .collect(Collectors.groupingBy(t -> t.startTime().toEpochMilli() / 60000));
                yield buckets.values().stream()
                        .mapToDouble(List::size)
                        .toArray();
            }
            default -> new double[0];
        };
    }

    private void detectLatencyAnomaly(MetricSnapshot snapshot, BaselineStats baseline, List<Anomaly> anomalies) {
        double zScore = baseline.zScore(snapshot.p99Latency);
        double percentChange = baseline.percentageChange(snapshot.p99Latency);

        if (zScore > config.getLatencyZScoreThreshold() &&
                percentChange > config.getLatencyMinIncreasePercent()) {

            String id = generateAnomalyId("latency", "global");

            // Don't duplicate if already detected and active
            if (hasActiveAnomaly(id)) return;

            Anomaly anomaly = Anomaly.builder()
                    .id(id)
                    .type(AnomalyType.LATENCY_SPIKE)
                    .metric("latency_p99")
                    .baselineValue(baseline.mean())
                    .currentValue(snapshot.p99Latency)
                    .deviation(zScore)
                    .percentageChange(percentChange)
                    .possibleCauses(findPossibleCauses(AnomalyType.LATENCY_SPIKE, snapshot))
                    .build();

            anomalies.add(anomaly);
            log.info("Detected latency spike: baseline={:.2f}ms, current={:.2f}ms, change={:.1f}%",
                    baseline.mean(), snapshot.p99Latency, percentChange);
        }
    }

    private void detectErrorRateAnomaly(MetricSnapshot snapshot, BaselineStats baseline, List<Anomaly> anomalies) {
        if (snapshot.errorRate < config.getErrorRateMinAbsolute()) {
            return; // Error rate too low to care about
        }

        double zScore = baseline.zScore(snapshot.errorRate);
        double percentChange = baseline.percentageChange(snapshot.errorRate);

        if (zScore > config.getErrorRateZScoreThreshold()) {

            String id = generateAnomalyId("error_rate", "global");

            if (hasActiveAnomaly(id)) return;

            Anomaly anomaly = Anomaly.builder()
                    .id(id)
                    .type(AnomalyType.ERROR_RATE_SPIKE)
                    .metric("error_rate")
                    .baselineValue(baseline.mean())
                    .currentValue(snapshot.errorRate)
                    .deviation(zScore)
                    .percentageChange(percentChange)
                    .possibleCauses(findPossibleCauses(AnomalyType.ERROR_RATE_SPIKE, snapshot))
                    .build();

            anomalies.add(anomaly);
            log.info("Detected error rate spike: baseline={:.2f}%, current={:.2f}%",
                    baseline.mean(), snapshot.errorRate);
        }
    }

    private void detectTrafficAnomaly(MetricSnapshot snapshot, BaselineStats baseline, List<Anomaly> anomalies) {
        double zScore = baseline.zScore(snapshot.requestCount);
        double percentChange = baseline.percentageChange(snapshot.requestCount);

        boolean isSpike = zScore > config.getTrafficZScoreThreshold();
        boolean isDrop = zScore < -config.getTrafficZScoreThreshold() && config.isAlertOnTrafficDecrease();

        if (isSpike || isDrop) {
            AnomalyType type = isSpike ? AnomalyType.TRAFFIC_SPIKE : AnomalyType.TRAFFIC_DROP;
            String id = generateAnomalyId("traffic", type.name().toLowerCase());

            if (hasActiveAnomaly(id)) return;

            Anomaly anomaly = Anomaly.builder()
                    .id(id)
                    .type(type)
                    .metric("request_count")
                    .baselineValue(baseline.mean())
                    .currentValue(snapshot.requestCount)
                    .deviation(zScore)
                    .percentageChange(percentChange)
                    .possibleCauses(findPossibleCauses(type, snapshot))
                    .build();

            anomalies.add(anomaly);
            log.info("Detected traffic {}: baseline={:.0f}, current={:.0f}, change={:.1f}%",
                    isSpike ? "spike" : "drop", baseline.mean(), (double) snapshot.requestCount, percentChange);
        }
    }

    private void detectEndpointAnomalies(List<Trace> traces, List<Anomaly> anomalies) {
        // Group by endpoint and detect per-endpoint anomalies
        Map<String, List<Trace>> byEndpoint = traces.stream()
                .filter(t -> t.httpMethod() != null && t.httpUrl() != null)
                .collect(Collectors.groupingBy(t -> t.httpMethod() + " " + normalizeUrl(t.httpUrl())));

        for (Map.Entry<String, List<Trace>> entry : byEndpoint.entrySet()) {
            String endpoint = entry.getKey();
            List<Trace> endpointTraces = entry.getValue();

            if (endpointTraces.size() < 10) continue; // Not enough samples

            // Check for slow endpoints
            long[] durations = endpointTraces.stream()
                    .mapToLong(Trace::durationMs)
                    .sorted()
                    .toArray();

            double p99 = percentile(durations, 99);
            if (p99 > 5000) { // More than 5 seconds
                String id = generateAnomalyId("slow_endpoint", endpoint);
                if (!hasActiveAnomaly(id)) {
                    Anomaly anomaly = Anomaly.builder()
                            .id(id)
                            .type(AnomalyType.LATENCY_SPIKE)
                            .metric("endpoint_latency_p99")
                            .endpoint(endpoint)
                            .baselineValue(1000) // Expected 1s
                            .currentValue(p99)
                            .percentageChange(((p99 - 1000) / 1000) * 100)
                            .build();
                    anomalies.add(anomaly);
                }
            }

            // Check for high error rates per endpoint
            long errors = endpointTraces.stream().filter(Trace::hasError).count();
            double errorRate = (double) errors / endpointTraces.size() * 100;
            if (errorRate > 10) { // More than 10% errors
                String id = generateAnomalyId("endpoint_errors", endpoint);
                if (!hasActiveAnomaly(id)) {
                    Anomaly anomaly = Anomaly.builder()
                            .id(id)
                            .type(AnomalyType.ERROR_RATE_SPIKE)
                            .metric("endpoint_error_rate")
                            .endpoint(endpoint)
                            .baselineValue(1) // Expected 1%
                            .currentValue(errorRate)
                            .percentageChange(((errorRate - 1) / 1) * 100)
                            .build();
                    anomalies.add(anomaly);
                }
            }
        }
    }

    private List<PossibleCause> findPossibleCauses(AnomalyType type, MetricSnapshot snapshot) {
        List<PossibleCause> causes = new ArrayList<>();

        switch (type) {
            case LATENCY_SPIKE -> {
                // Check for slow endpoints
                snapshot.endpointMetrics.values().stream()
                        .filter(e -> e.p99Latency > 2000)
                        .findFirst()
                        .ifPresent(e -> causes.add(new PossibleCause(
                                "Slow endpoint: " + e.endpoint + " (" + String.format("%.0f", e.p99Latency) + "ms)",
                                PossibleCause.CauseType.SLOW_QUERIES,
                                PossibleCause.Confidence.MEDIUM)));

                causes.add(new PossibleCause(
                        "Check database query performance",
                        PossibleCause.CauseType.SLOW_QUERIES,
                        PossibleCause.Confidence.MEDIUM));
            }
            case ERROR_RATE_SPIKE -> {
                causes.add(new PossibleCause(
                        "Check for recent deployments",
                        PossibleCause.CauseType.RECENT_DEPLOY,
                        PossibleCause.Confidence.MEDIUM));
                causes.add(new PossibleCause(
                        "Check external service health",
                        PossibleCause.CauseType.EXTERNAL_SERVICE,
                        PossibleCause.Confidence.MEDIUM));
            }
            case TRAFFIC_SPIKE -> {
                causes.add(new PossibleCause(
                        "Possible traffic surge from external source",
                        PossibleCause.CauseType.TRAFFIC_CHANGE,
                        PossibleCause.Confidence.MEDIUM));
            }
            case TRAFFIC_DROP -> {
                causes.add(new PossibleCause(
                        "Check load balancer and routing",
                        PossibleCause.CauseType.CONFIGURATION_CHANGE,
                        PossibleCause.Confidence.MEDIUM));
                causes.add(new PossibleCause(
                        "Check for client-side issues",
                        PossibleCause.CauseType.EXTERNAL_SERVICE,
                        PossibleCause.Confidence.LOW));
            }
            default -> {}
        }

        return causes;
    }

    private void autoResolveAnomalies(MetricSnapshot currentSnapshot) {
        for (Anomaly anomaly : detectedAnomalies.values()) {
            if (!anomaly.isActive()) continue;

            boolean shouldResolve = switch (anomaly.type()) {
                case LATENCY_SPIKE -> currentSnapshot.p99Latency < anomaly.baselineValue() * 1.5;
                case ERROR_RATE_SPIKE -> currentSnapshot.errorRate < Math.max(1, anomaly.baselineValue() * 1.5);
                case TRAFFIC_SPIKE, TRAFFIC_DROP -> {
                    double change = Math.abs(currentSnapshot.requestCount - anomaly.baselineValue()) / anomaly.baselineValue();
                    yield change < 0.5; // Within 50% of baseline
                }
                default -> false;
            };

            if (shouldResolve) {
                updateStatus(anomaly.id(), AnomalyStatus.RESOLVED);
            }
        }
    }

    private String generateAnomalyId(String type, String identifier) {
        return type + "-" + identifier.hashCode();
    }

    private boolean hasActiveAnomaly(String id) {
        Anomaly existing = detectedAnomalies.get(id);
        return existing != null && existing.isActive();
    }

    private double percentile(long[] sorted, int percentile) {
        if (sorted.length == 0) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    @Override
    public List<Anomaly> getAnomalies(AnomalyStatus status) {
        return detectedAnomalies.values().stream()
                .filter(a -> status == null || a.status() == status)
                .sorted(Comparator.comparing(Anomaly::detectedAt).reversed())
                .toList();
    }

    @Override
    public List<Anomaly> getAnomaliesByType(AnomalyType type) {
        return detectedAnomalies.values().stream()
                .filter(a -> a.type() == type)
                .sorted(Comparator.comparing(Anomaly::detectedAt).reversed())
                .toList();
    }

    @Override
    public Anomaly getAnomaly(String id) {
        return detectedAnomalies.get(id);
    }

    @Override
    public Anomaly updateStatus(String id, AnomalyStatus status) {
        Anomaly existing = detectedAnomalies.get(id);
        if (existing == null) return null;

        Anomaly.Builder builder = Anomaly.builder()
                .id(existing.id())
                .type(existing.type())
                .metric(existing.metric())
                .endpoint(existing.endpoint())
                .service(existing.service())
                .baselineValue(existing.baselineValue())
                .currentValue(existing.currentValue())
                .deviation(existing.deviation())
                .percentageChange(existing.percentageChange())
                .detectedAt(existing.detectedAt())
                .possibleCauses(existing.possibleCauses())
                .status(status);

        if (status == AnomalyStatus.RESOLVED) {
            builder.resolvedAt(Instant.now());
        }

        Anomaly updated = builder.build();
        detectedAnomalies.put(id, updated);
        return updated;
    }

    @Override
    public void clearResolved() {
        detectedAnomalies.entrySet().removeIf(e ->
                e.getValue().status() == AnomalyStatus.RESOLVED ||
                        e.getValue().status() == AnomalyStatus.IGNORED);
    }

    @Override
    public void clearAll() {
        detectedAnomalies.clear();
    }

    @Override
    public AnomalyStats getStats() {
        Collection<Anomaly> anomalies = detectedAnomalies.values();

        long active = anomalies.stream().filter(Anomaly::isActive).count();
        long critical = anomalies.stream().filter(a -> a.isActive() && a.isCritical()).count();
        long warning = anomalies.stream().filter(a -> a.isActive() && !a.isCritical()).count();
        long resolved = anomalies.stream().filter(Anomaly::isResolved).count();

        return new AnomalyStats(
                anomalies.size(),
                active,
                critical,
                warning,
                resolved,
                lastDetectionRun,
                lastDetectionDuration
        );
    }

    // Internal record classes
    private record MetricSnapshot(
            long requestCount,
            double avgLatency,
            double p99Latency,
            double errorRate,
            long errorCount,
            Map<String, EndpointMetrics> endpointMetrics
    ) {}

    private record EndpointMetrics(
            String endpoint,
            int requestCount,
            double avgLatency,
            double p99Latency,
            double errorRate
    ) {}
}
