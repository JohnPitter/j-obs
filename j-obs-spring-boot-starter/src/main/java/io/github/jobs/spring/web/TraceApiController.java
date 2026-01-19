package io.github.jobs.spring.web;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.application.TraceRepository.TraceStats;
import io.github.jobs.domain.trace.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API controller for trace data.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/traces")
public class TraceApiController {

    private final TraceRepository traceRepository;

    public TraceApiController(TraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public TracesResponse getTraces(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long minDurationMs,
            @RequestParam(required = false) Long maxDurationMs,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        // Sanitize inputs
        int sanitizedLimit = InputSanitizer.sanitizeLimit(limit);
        int sanitizedOffset = InputSanitizer.sanitizeOffset(offset);
        String sanitizedService = InputSanitizer.sanitizeGeneric(service);
        String sanitizedName = InputSanitizer.sanitizeGeneric(name);

        TraceQuery.Builder queryBuilder = TraceQuery.builder()
                .limit(sanitizedLimit)
                .offset(sanitizedOffset);

        if (sanitizedService != null) {
            queryBuilder.serviceName(sanitizedService);
        }
        if (sanitizedName != null) {
            queryBuilder.spanName(sanitizedName);
        }
        if (status != null && !status.isEmpty()) {
            try {
                queryBuilder.status(SpanStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Invalid status, ignore filter
            }
        }
        if (minDurationMs != null && minDurationMs >= 0) {
            queryBuilder.minDuration(Duration.ofMillis(minDurationMs));
        }
        if (maxDurationMs != null && maxDurationMs >= 0) {
            queryBuilder.maxDuration(Duration.ofMillis(maxDurationMs));
        }

        TraceQuery query = queryBuilder.build();
        List<Trace> traces = traceRepository.query(query);
        long total = traceRepository.count(query);

        return new TracesResponse(
            traces.stream().map(TraceSummaryDto::from).toList(),
            total,
            sanitizedLimit,
            sanitizedOffset
        );
    }

    @GetMapping(value = "/{traceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TraceDetailDto> getTrace(
            @PathVariable String traceId,
            @RequestParam(defaultValue = "true") boolean includeSpans,
            @RequestParam(defaultValue = "100") int maxSpans
    ) {
        String sanitizedTraceId = InputSanitizer.sanitizeTraceId(traceId);
        if (sanitizedTraceId == null) {
            return ResponseEntity.badRequest().build();
        }
        int sanitizedMaxSpans = Math.max(1, Math.min(maxSpans, 1000));
        return traceRepository.findByTraceId(sanitizedTraceId)
                .map(trace -> TraceDetailDto.from(trace, includeSpans, sanitizedMaxSpans))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{traceId}/spans", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SpansResponse> getSpans(
            @PathVariable String traceId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        String sanitizedTraceId = InputSanitizer.sanitizeTraceId(traceId);
        if (sanitizedTraceId == null) {
            return ResponseEntity.badRequest().build();
        }
        int sanitizedLimit = InputSanitizer.sanitizeLimit(limit);
        int sanitizedOffset = InputSanitizer.sanitizeOffset(offset);

        return traceRepository.findByTraceId(sanitizedTraceId)
                .map(trace -> {
                    List<Span> allSpans = trace.spansForWaterfall();
                    int total = allSpans.size();
                    List<SpanDto> spans = allSpans.stream()
                            .skip(sanitizedOffset)
                            .limit(sanitizedLimit)
                            .map(span -> SpanDto.from(span, trace.depthOf(span), trace))
                            .toList();
                    boolean hasMore = sanitizedOffset + spans.size() < total;
                    return new SpansResponse(spans, total, sanitizedLimit, sanitizedOffset, hasMore);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public TraceStats getStats() {
        return traceRepository.stats();
    }

    @GetMapping(value = "/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getServices() {
        return traceRepository.query(TraceQuery.recent(1000)).stream()
                .map(Trace::serviceName)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearTraces() {
        traceRepository.clear();
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns request rate history for the last N minutes, grouped by minute.
     */
    @GetMapping(value = "/request-rate", produces = MediaType.APPLICATION_JSON_VALUE)
    public RequestRateResponse getRequestRate(
            @RequestParam(defaultValue = "5") int minutes
    ) {
        Instant now = Instant.now();
        Instant start = now.minus(minutes, ChronoUnit.MINUTES);

        List<Trace> recentTraces = traceRepository.recent(1000);

        // Group traces by minute
        Map<Long, Long> countsByMinute = new LinkedHashMap<>();

        // Initialize all minutes to 0
        for (int i = minutes - 1; i >= 0; i--) {
            Instant minuteStart = now.minus(i, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
            countsByMinute.put(minuteStart.toEpochMilli(), 0L);
        }

        // Count traces per minute
        for (Trace trace : recentTraces) {
            if (trace.startTime().isAfter(start)) {
                long minuteKey = trace.startTime().truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
                countsByMinute.computeIfPresent(minuteKey, (k, v) -> v + 1);
            }
        }

        List<RequestRatePoint> points = countsByMinute.entrySet().stream()
                .map(e -> new RequestRatePoint(Instant.ofEpochMilli(e.getKey()), e.getValue()))
                .toList();

        long totalRequests = points.stream().mapToLong(RequestRatePoint::count).sum();
        double avgPerMinute = minutes > 0 ? (double) totalRequests / minutes : 0;

        return new RequestRateResponse(points, totalRequests, avgPerMinute);
    }

    /**
     * Returns top endpoints by average latency.
     */
    @GetMapping(value = "/top-endpoints", produces = MediaType.APPLICATION_JSON_VALUE)
    public TopEndpointsResponse getTopEndpoints(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "latency") String sortBy
    ) {
        List<Trace> recentTraces = traceRepository.recent(1000);

        // Group by endpoint (method + url pattern)
        Map<String, EndpointStats> endpointStats = new HashMap<>();

        for (Trace trace : recentTraces) {
            String method = trace.httpMethod();
            String url = trace.httpUrl();

            if (method == null || url == null) {
                // Use trace name for non-HTTP traces
                String endpoint = trace.name() != null ? trace.name() : "Unknown";
                endpointStats.computeIfAbsent(endpoint, k -> new EndpointStats(endpoint, null))
                        .record(trace.durationMs(), trace.hasError());
            } else {
                // Normalize URL by removing query params and IDs
                String normalizedUrl = normalizeUrl(url);
                String endpoint = method + " " + normalizedUrl;
                endpointStats.computeIfAbsent(endpoint, k -> new EndpointStats(endpoint, method))
                        .record(trace.durationMs(), trace.hasError());
            }
        }

        // Sort by latency or count
        Comparator<EndpointStatsDto> comparator = "count".equals(sortBy)
                ? Comparator.comparingLong(EndpointStatsDto::count).reversed()
                : Comparator.comparingDouble(EndpointStatsDto::avgLatencyMs).reversed();

        List<EndpointStatsDto> endpoints = endpointStats.values().stream()
                .map(EndpointStats::toDto)
                .sorted(comparator)
                .limit(limit)
                .toList();

        return new TopEndpointsResponse(endpoints);
    }

    private String normalizeUrl(String url) {
        try {
            URL parsed = new URL(url);
            String path = parsed.getPath();
            // Replace numeric IDs with {id}
            return path.replaceAll("/\\d+", "/{id}");
        } catch (Exception e) {
            return url.replaceAll("/\\d+", "/{id}");
        }
    }

    // Helper class for aggregating endpoint stats
    private static class EndpointStats {
        final String endpoint;
        final String method;
        long totalLatency = 0;
        long count = 0;
        long errorCount = 0;
        long minLatency = Long.MAX_VALUE;
        long maxLatency = 0;

        EndpointStats(String endpoint, String method) {
            this.endpoint = endpoint;
            this.method = method;
        }

        void record(long latencyMs, boolean hasError) {
            totalLatency += latencyMs;
            count++;
            if (hasError) errorCount++;
            minLatency = Math.min(minLatency, latencyMs);
            maxLatency = Math.max(maxLatency, latencyMs);
        }

        EndpointStatsDto toDto() {
            return new EndpointStatsDto(
                    endpoint,
                    method,
                    count,
                    count > 0 ? (double) totalLatency / count : 0,
                    minLatency == Long.MAX_VALUE ? 0 : minLatency,
                    maxLatency,
                    errorCount,
                    count > 0 ? ((double) errorCount / count) * 100 : 0
            );
        }
    }

    // DTOs

    public record TracesResponse(
        List<TraceSummaryDto> traces,
        long total,
        int limit,
        int offset
    ) {}

    public record TraceSummaryDto(
        String traceId,
        String name,
        String serviceName,
        String status,
        long durationMs,
        int spanCount,
        Instant startTime,
        String httpMethod,
        String httpUrl,
        Integer httpStatusCode,
        boolean hasError
    ) {
        public static TraceSummaryDto from(Trace trace) {
            return new TraceSummaryDto(
                trace.traceId(),
                trace.name(),
                trace.serviceName(),
                trace.status().name(),
                trace.durationMs(),
                trace.spanCount(),
                trace.startTime(),
                trace.httpMethod(),
                trace.httpUrl(),
                trace.httpStatusCode(),
                trace.hasError()
            );
        }
    }

    public record TraceDetailDto(
        String traceId,
        String name,
        String serviceName,
        String status,
        long durationMs,
        int spanCount,
        Instant startTime,
        Instant endTime,
        List<String> services,
        List<SpanDto> spans,
        List<SpanDto> errorSpans,
        boolean hasError,
        boolean spansIncluded,
        boolean hasMoreSpans
    ) {
        /**
         * Creates a TraceDetailDto with all spans included (backward compatible).
         */
        public static TraceDetailDto from(Trace trace) {
            return from(trace, true, Integer.MAX_VALUE);
        }

        /**
         * Creates a TraceDetailDto with optional lazy loading of spans.
         *
         * @param trace the trace to convert
         * @param includeSpans whether to include spans in the response
         * @param maxSpans maximum number of spans to include (only used if includeSpans is true)
         * @return the DTO
         */
        public static TraceDetailDto from(Trace trace, boolean includeSpans, int maxSpans) {
            List<SpanDto> spans;
            List<SpanDto> errorSpans;
            boolean hasMoreSpans = false;

            if (includeSpans) {
                List<Span> allSpans = trace.spansForWaterfall();
                hasMoreSpans = allSpans.size() > maxSpans;
                spans = allSpans.stream()
                    .limit(maxSpans)
                    .map(span -> SpanDto.from(span, trace.depthOf(span), trace))
                    .toList();
                // Always include all error spans (they're important for debugging)
                errorSpans = trace.errorSpans().stream()
                    .map(span -> SpanDto.from(span, trace.depthOf(span), trace))
                    .toList();
            } else {
                spans = List.of();
                errorSpans = List.of();
            }

            return new TraceDetailDto(
                trace.traceId(),
                trace.name(),
                trace.serviceName(),
                trace.status().name(),
                trace.durationMs(),
                trace.spanCount(),
                trace.startTime(),
                trace.endTime(),
                trace.services().stream().sorted().toList(),
                spans,
                errorSpans,
                trace.hasError(),
                includeSpans,
                hasMoreSpans
            );
        }
    }

    public record SpanDto(
        String spanId,
        String parentSpanId,
        String name,
        String kind,
        String kindCss,
        String status,
        String statusCss,
        String statusMessage,
        String serviceName,
        long durationMs,
        Instant startTime,
        Instant endTime,
        int depth,
        double offsetPercent,
        double widthPercent,
        Map<String, String> attributes,
        List<SpanEventDto> events,
        boolean hasError,
        String httpMethod,
        String httpUrl,
        Integer httpStatusCode,
        String dbSystem,
        String dbStatement,
        String dbOperation
    ) {
        public static SpanDto from(Span span, int depth, Trace trace) {
            long traceStart = trace.startTime().toEpochMilli();
            long traceDuration = Math.max(1, trace.durationMs());
            long spanStart = span.startTime().toEpochMilli();
            long spanDuration = span.durationMs();

            double offsetPercent = ((double)(spanStart - traceStart) / traceDuration) * 100;
            double widthPercent = Math.max(0.5, ((double)spanDuration / traceDuration) * 100);

            return new SpanDto(
                span.spanId(),
                span.parentSpanId(),
                span.name(),
                span.kind().displayName(),
                span.kind().cssClass(),
                span.status().displayName(),
                span.status().cssClass(),
                span.statusMessage(),
                span.serviceName(),
                spanDuration,
                span.startTime(),
                span.endTime(),
                depth,
                offsetPercent,
                widthPercent,
                span.attributes(),
                span.events().stream().map(SpanEventDto::from).toList(),
                span.hasError(),
                span.httpMethod(),
                span.httpUrl(),
                span.httpStatusCode(),
                span.dbSystem(),
                span.dbStatement(),
                span.dbOperation()
            );
        }
    }

    public record SpanEventDto(
        String name,
        Instant timestamp,
        Map<String, String> attributes,
        boolean isException,
        String exceptionType,
        String exceptionMessage
    ) {
        public static SpanEventDto from(SpanEvent event) {
            return new SpanEventDto(
                event.name(),
                event.timestamp(),
                event.attributes(),
                event.isException(),
                event.exceptionType(),
                event.exceptionMessage()
            );
        }
    }

    // Spans pagination response
    public record SpansResponse(
        List<SpanDto> spans,
        int total,
        int limit,
        int offset,
        boolean hasMore
    ) {}

    // Dashboard DTOs

    public record RequestRateResponse(
        List<RequestRatePoint> points,
        long totalRequests,
        double avgPerMinute
    ) {}

    public record RequestRatePoint(
        Instant timestamp,
        long count
    ) {}

    public record TopEndpointsResponse(
        List<EndpointStatsDto> endpoints
    ) {}

    public record EndpointStatsDto(
        String endpoint,
        String method,
        long count,
        double avgLatencyMs,
        long minLatencyMs,
        long maxLatencyMs,
        long errorCount,
        double errorRate
    ) {}
}
