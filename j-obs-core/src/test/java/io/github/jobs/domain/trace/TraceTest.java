package io.github.jobs.domain.trace;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TraceTest {

    @Test
    void shouldCreateTraceFromSpans() {
        Instant start = Instant.now();

        Span rootSpan = createSpan("span-1", null, "GET /api/orders", start, 100, SpanStatus.OK, "order-service");
        Span childSpan = createSpan("span-2", "span-1", "SELECT orders", start.plusMillis(10), 50, SpanStatus.OK, "order-service");

        Trace trace = Trace.of("trace-123", List.of(rootSpan, childSpan));

        assertThat(trace.traceId()).isEqualTo("trace-123");
        assertThat(trace.name()).isEqualTo("GET /api/orders");
        assertThat(trace.serviceName()).isEqualTo("order-service");
        assertThat(trace.spanCount()).isEqualTo(2);
        assertThat(trace.status()).isEqualTo(SpanStatus.OK);
        assertThat(trace.hasError()).isFalse();
    }

    @Test
    void shouldCalculateTraceDuration() {
        Instant start = Instant.now();

        Span rootSpan = createSpan("span-1", null, "root", start, 200, SpanStatus.OK, "service-a");
        Span childSpan = createSpan("span-2", "span-1", "child", start.plusMillis(50), 100, SpanStatus.OK, "service-b");

        Trace trace = Trace.of("trace-123", List.of(rootSpan, childSpan));

        assertThat(trace.durationMs()).isEqualTo(200);
        assertThat(trace.startTime()).isEqualTo(start);
        assertThat(trace.endTime()).isEqualTo(start.plusMillis(200));
    }

    @Test
    void shouldDetectErrorInTrace() {
        Instant start = Instant.now();

        Span rootSpan = createSpan("span-1", null, "root", start, 200, SpanStatus.OK, "service-a");
        Span errorSpan = createSpan("span-2", "span-1", "error-child", start.plusMillis(50), 100, SpanStatus.ERROR, "service-b");

        Trace trace = Trace.of("trace-123", List.of(rootSpan, errorSpan));

        assertThat(trace.hasError()).isTrue();
        assertThat(trace.status()).isEqualTo(SpanStatus.ERROR);
        assertThat(trace.errorSpans()).hasSize(1);
        assertThat(trace.errorSpans().get(0).name()).isEqualTo("error-child");
    }

    @Test
    void shouldCollectAllServices() {
        Instant start = Instant.now();

        Span span1 = createSpan("span-1", null, "root", start, 200, SpanStatus.OK, "api-gateway");
        Span span2 = createSpan("span-2", "span-1", "order", start.plusMillis(10), 100, SpanStatus.OK, "order-service");
        Span span3 = createSpan("span-3", "span-2", "db", start.plusMillis(20), 50, SpanStatus.OK, "order-service");
        Span span4 = createSpan("span-4", "span-1", "payment", start.plusMillis(50), 80, SpanStatus.OK, "payment-service");

        Trace trace = Trace.of("trace-123", List.of(span1, span2, span3, span4));

        Set<String> services = trace.services();
        assertThat(services).containsExactlyInAnyOrder("api-gateway", "order-service", "payment-service");
    }

    @Test
    void shouldOrderSpansForWaterfall() {
        Instant start = Instant.now();

        // Add spans out of order
        Span span3 = createSpan("span-3", "span-2", "grandchild", start.plusMillis(20), 30, SpanStatus.OK, "service");
        Span span1 = createSpan("span-1", null, "root", start, 100, SpanStatus.OK, "service");
        Span span2 = createSpan("span-2", "span-1", "child", start.plusMillis(10), 50, SpanStatus.OK, "service");

        Trace trace = Trace.of("trace-123", List.of(span3, span1, span2));

        List<Span> waterfall = trace.spansForWaterfall();
        assertThat(waterfall).hasSize(3);
        assertThat(waterfall.get(0).name()).isEqualTo("root");
        assertThat(waterfall.get(1).name()).isEqualTo("child");
        assertThat(waterfall.get(2).name()).isEqualTo("grandchild");
    }

    @Test
    void shouldCalculateSpanDepth() {
        Instant start = Instant.now();

        Span root = createSpan("span-1", null, "root", start, 100, SpanStatus.OK, "service");
        Span child = createSpan("span-2", "span-1", "child", start.plusMillis(10), 50, SpanStatus.OK, "service");
        Span grandchild = createSpan("span-3", "span-2", "grandchild", start.plusMillis(20), 30, SpanStatus.OK, "service");

        Trace trace = Trace.of("trace-123", List.of(root, child, grandchild));

        assertThat(trace.depthOf(root)).isEqualTo(0);
        assertThat(trace.depthOf(child)).isEqualTo(1);
        assertThat(trace.depthOf(grandchild)).isEqualTo(2);
    }

    @Test
    void shouldMatchQueryByServiceName() {
        Instant start = Instant.now();

        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-1")
                .name("GET /api/orders")
                .serviceName("order-service")
                .kind(SpanKind.SERVER)
                .status(SpanStatus.OK)
                .startTime(start)
                .endTime(start.plusMillis(150))
                .attribute("http.method", "GET")
                .build();

        Trace trace = Trace.of("trace-123", List.of(span));

        TraceQuery matchingQuery = TraceQuery.builder()
                .serviceName("order-service")
                .build();

        TraceQuery nonMatchingQuery = TraceQuery.builder()
                .serviceName("payment-service")
                .build();

        assertThat(matchingQuery.matches(trace)).isTrue();
        assertThat(nonMatchingQuery.matches(trace)).isFalse();
    }

    @Test
    void shouldExtractHttpInfoFromRootSpan() {
        Instant start = Instant.now();

        Span rootSpan = Span.builder()
                .traceId("trace-123")
                .spanId("span-1")
                .name("GET /api/users")
                .serviceName("api-service")
                .kind(SpanKind.SERVER)
                .status(SpanStatus.OK)
                .startTime(start)
                .endTime(start.plusMillis(100))
                .attribute("http.method", "GET")
                .attribute("http.url", "/api/users")
                .attribute("http.status_code", "200")
                .build();

        Trace trace = Trace.of("trace-123", List.of(rootSpan));

        assertThat(trace.httpMethod()).isEqualTo("GET");
        assertThat(trace.httpUrl()).isEqualTo("/api/users");
        assertThat(trace.httpStatusCode()).isEqualTo(200);
    }

    private Span createSpan(String spanId, String parentSpanId, String name, Instant start, long durationMs, SpanStatus status, String serviceName) {
        return Span.builder()
                .traceId("trace-123")
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(name)
                .serviceName(serviceName)
                .kind(SpanKind.INTERNAL)
                .status(status)
                .startTime(start)
                .endTime(start.plusMillis(durationMs))
                .build();
    }
}
