package io.github.jobs.domain.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete distributed trace.
 * A trace is a collection of spans that share the same trace ID.
 */
public final class Trace {

    private final String traceId;
    private final List<Span> spans;
    private final Span rootSpan;
    private final Instant startTime;
    private final Instant endTime;

    private Trace(String traceId, List<Span> spans) {
        this.traceId = Objects.requireNonNull(traceId, "traceId is required");
        this.spans = Collections.unmodifiableList(new ArrayList<>(spans));
        this.rootSpan = findRootSpan(spans);
        this.startTime = calculateStartTime(spans);
        this.endTime = calculateEndTime(spans);
    }

    /**
     * Creates a trace from a list of spans.
     */
    public static Trace of(String traceId, List<Span> spans) {
        return new Trace(traceId, spans);
    }

    /**
     * Creates a trace from a single span.
     */
    public static Trace of(Span span) {
        return new Trace(span.traceId(), List.of(span));
    }

    private static Span findRootSpan(List<Span> spans) {
        return spans.stream()
                .filter(Span::isRoot)
                .findFirst()
                .orElse(spans.isEmpty() ? null : spans.get(0));
    }

    private static Instant calculateStartTime(List<Span> spans) {
        return spans.stream()
                .map(Span::startTime)
                .min(Instant::compareTo)
                .orElse(Instant.now());
    }

    private static Instant calculateEndTime(List<Span> spans) {
        return spans.stream()
                .map(Span::endTime)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    public String traceId() {
        return traceId;
    }

    public List<Span> spans() {
        return spans;
    }

    public int spanCount() {
        return spans.size();
    }

    public Span rootSpan() {
        return rootSpan;
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public boolean isComplete() {
        return endTime != null && spans.stream().allMatch(Span::isComplete);
    }

    public Duration duration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }

    public long durationMs() {
        return duration().toMillis();
    }

    /**
     * Returns the primary service name (from root span).
     */
    public String serviceName() {
        return rootSpan != null ? rootSpan.serviceName() : null;
    }

    /**
     * Returns the name of the trace (from root span).
     */
    public String name() {
        return rootSpan != null ? rootSpan.name() : traceId;
    }

    /**
     * Returns the overall status of the trace.
     * ERROR if any span has error, otherwise OK.
     */
    public SpanStatus status() {
        boolean hasError = spans.stream().anyMatch(Span::hasError);
        return hasError ? SpanStatus.ERROR : SpanStatus.OK;
    }

    public boolean hasError() {
        return status().isError();
    }

    /**
     * Returns all unique service names in this trace.
     */
    public Set<String> services() {
        return spans.stream()
                .map(Span::serviceName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Returns spans organized by their parent relationships.
     * Key is parent span ID (null for root spans), value is list of child spans.
     */
    public Map<String, List<Span>> spansByParent() {
        return spans.stream()
                .collect(Collectors.groupingBy(
                    span -> span.parentSpanId() != null ? span.parentSpanId() : "",
                    Collectors.toList()
                ));
    }

    /**
     * Returns spans sorted for waterfall display.
     * Root span first, then children in order of start time.
     */
    public List<Span> spansForWaterfall() {
        List<Span> result = new ArrayList<>();
        Map<String, List<Span>> byParent = spansByParent();

        // Start with root spans
        List<Span> roots = byParent.getOrDefault("", Collections.emptyList());
        roots.stream()
            .sorted(Comparator.comparing(Span::startTime))
            .forEach(root -> addSpanAndChildren(root, byParent, result));

        return result;
    }

    private void addSpanAndChildren(Span span, Map<String, List<Span>> byParent, List<Span> result) {
        result.add(span);
        List<Span> children = byParent.getOrDefault(span.spanId(), Collections.emptyList());
        children.stream()
            .sorted(Comparator.comparing(Span::startTime))
            .forEach(child -> addSpanAndChildren(child, byParent, result));
    }

    /**
     * Calculates the depth of a span in the trace tree.
     */
    public int depthOf(Span span) {
        int depth = 0;
        String parentId = span.parentSpanId();

        Map<String, Span> spansById = spans.stream()
                .collect(Collectors.toMap(Span::spanId, s -> s));

        while (parentId != null && !parentId.isEmpty()) {
            depth++;
            Span parent = spansById.get(parentId);
            if (parent == null) break;
            parentId = parent.parentSpanId();
        }

        return depth;
    }

    /**
     * Returns the HTTP method if this is an HTTP trace.
     */
    public String httpMethod() {
        return rootSpan != null ? rootSpan.httpMethod() : null;
    }

    /**
     * Returns the HTTP URL if this is an HTTP trace.
     */
    public String httpUrl() {
        return rootSpan != null ? rootSpan.httpUrl() : null;
    }

    /**
     * Returns the HTTP status code if this is an HTTP trace.
     */
    public Integer httpStatusCode() {
        return rootSpan != null ? rootSpan.httpStatusCode() : null;
    }

    /**
     * Returns spans that have errors.
     */
    public List<Span> errorSpans() {
        return spans.stream()
                .filter(Span::hasError)
                .collect(Collectors.toList());
    }

    /**
     * Returns database spans.
     */
    public List<Span> dbSpans() {
        return spans.stream()
                .filter(s -> s.dbSystem() != null)
                .collect(Collectors.toList());
    }

    /**
     * Returns HTTP client spans.
     */
    public List<Span> httpClientSpans() {
        return spans.stream()
                .filter(s -> s.kind() == SpanKind.CLIENT && s.httpMethod() != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trace trace = (Trace) o;
        return Objects.equals(traceId, trace.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId);
    }

    @Override
    public String toString() {
        return "Trace{" +
                "traceId='" + traceId + '\'' +
                ", spans=" + spanCount() +
                ", duration=" + durationMs() + "ms" +
                ", status=" + status() +
                '}';
    }
}
