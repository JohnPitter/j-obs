package io.github.jobs.infrastructure;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTraceRepositoryTest {

    private InMemoryTraceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTraceRepository(Duration.ofHours(1), 1000);
    }

    @Test
    void shouldSaveAndFindTrace() {
        Span span = createSpan("trace-1", "span-1", null, "GET /api/users", 100, "user-service", SpanStatus.OK);
        repository.addSpan(span);

        Optional<Trace> found = repository.findByTraceId("trace-1");
        assertThat(found).isPresent();
        assertThat(found.get().traceId()).isEqualTo("trace-1");
        assertThat(found.get().name()).isEqualTo("GET /api/users");
    }

    @Test
    void shouldReturnEmptyForNonExistentTrace() {
        Optional<Trace> found = repository.findByTraceId("non-existent");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldQueryRecentTraces() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "GET /api/users", 100, "user-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "POST /api/orders", 200, "order-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "GET /api/products", 50, "product-service", SpanStatus.ERROR));

        List<Trace> recent = repository.query(TraceQuery.recent(10));

        assertThat(recent).hasSize(3);
    }

    @Test
    void shouldFilterByServiceName() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "GET /api/users", 100, "user-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "POST /api/orders", 200, "order-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "GET /api/users/1", 50, "user-service", SpanStatus.OK));

        TraceQuery query = TraceQuery.builder()
                .serviceName("user-service")
                .build();

        List<Trace> result = repository.query(query);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.serviceName().equals("user-service"));
    }

    @Test
    void shouldFilterByStatus() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "GET /api/users", 100, "user-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "POST /api/orders", 200, "order-service", SpanStatus.ERROR));
        repository.addSpan(createSpan("trace-3", "span-3", null, "GET /api/products", 50, "product-service", SpanStatus.ERROR));

        TraceQuery query = TraceQuery.builder()
                .status(SpanStatus.ERROR)
                .build();

        List<Trace> result = repository.query(query);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.status() == SpanStatus.ERROR);
    }

    @Test
    void shouldFilterByMinDuration() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "fast", 50, "service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "medium", 150, "service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "slow", 500, "service", SpanStatus.OK));

        TraceQuery query = TraceQuery.builder()
                .minDuration(Duration.ofMillis(100))
                .build();

        List<Trace> result = repository.query(query);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.durationMs() >= 100);
    }

    @Test
    void shouldFilterByMaxDuration() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "fast", 50, "service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "medium", 150, "service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "slow", 500, "service", SpanStatus.OK));

        TraceQuery query = TraceQuery.builder()
                .maxDuration(Duration.ofMillis(200))
                .build();

        List<Trace> result = repository.query(query);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.durationMs() <= 200);
    }

    @Test
    void shouldRespectLimitAndOffset() {
        for (int i = 0; i < 10; i++) {
            repository.addSpan(createSpan("trace-" + i, "span-" + i, null, "name-" + i, 100, "service", SpanStatus.OK));
        }

        TraceQuery query = TraceQuery.builder()
                .limit(3)
                .offset(2)
                .build();

        List<Trace> result = repository.query(query);

        assertThat(result).hasSize(3);
    }

    @Test
    void shouldCountTraces() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "name", 100, "user-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "name", 100, "user-service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "name", 100, "order-service", SpanStatus.OK));

        TraceQuery allQuery = TraceQuery.recent(100);
        assertThat(repository.count(allQuery)).isEqualTo(3);

        TraceQuery serviceQuery = TraceQuery.builder()
                .serviceName("user-service")
                .build();
        assertThat(repository.count(serviceQuery)).isEqualTo(2);
    }

    @Test
    void shouldCalculateStats() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "name", 100, "service-a", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "name", 200, "service-b", SpanStatus.OK));
        repository.addSpan(createSpan("trace-3", "span-3", null, "name", 150, "service-a", SpanStatus.ERROR));

        TraceRepository.TraceStats stats = repository.stats();

        assertThat(stats.totalTraces()).isEqualTo(3);
        assertThat(stats.errorTraces()).isEqualTo(1);
    }

    @Test
    void shouldClearAllTraces() {
        repository.addSpan(createSpan("trace-1", "span-1", null, "name", 100, "service", SpanStatus.OK));
        repository.addSpan(createSpan("trace-2", "span-2", null, "name", 100, "service", SpanStatus.OK));

        assertThat(repository.count(TraceQuery.recent(100))).isEqualTo(2);

        repository.clear();

        assertThat(repository.count(TraceQuery.recent(100))).isEqualTo(0);
    }

    @Test
    void shouldEvictOldestWhenMaxTracesReached() {
        InMemoryTraceRepository smallRepo = new InMemoryTraceRepository(Duration.ofHours(1), 3);

        smallRepo.addSpan(createSpan("trace-1", "span-1", null, "first", 100, "service", SpanStatus.OK));
        smallRepo.addSpan(createSpan("trace-2", "span-2", null, "second", 100, "service", SpanStatus.OK));
        smallRepo.addSpan(createSpan("trace-3", "span-3", null, "third", 100, "service", SpanStatus.OK));
        smallRepo.addSpan(createSpan("trace-4", "span-4", null, "fourth", 100, "service", SpanStatus.OK));

        assertThat(smallRepo.count(TraceQuery.recent(100))).isEqualTo(3);
        assertThat(smallRepo.findByTraceId("trace-1")).isEmpty(); // First one should be evicted
        assertThat(smallRepo.findByTraceId("trace-4")).isPresent(); // Latest should be present
    }

    @Test
    void shouldAddSpanToExistingTrace() {
        Span span1 = createSpan("trace-123", "span-1", null, "root", 100, "test-service", SpanStatus.OK);
        repository.addSpan(span1);

        Optional<Trace> trace = repository.findByTraceId("trace-123");
        assertThat(trace).isPresent();
        assertThat(trace.get().spanCount()).isEqualTo(1);

        Span span2 = createSpan("trace-123", "span-2", "span-1", "child", 50, "test-service", SpanStatus.OK);
        repository.addSpan(span2);

        trace = repository.findByTraceId("trace-123");
        assertThat(trace).isPresent();
        assertThat(trace.get().spanCount()).isEqualTo(2);
    }

    private Span createSpan(String traceId, String spanId, String parentSpanId, String name, long durationMs, String serviceName, SpanStatus status) {
        Instant start = Instant.now();
        return Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(name)
                .serviceName(serviceName)
                .kind(SpanKind.SERVER)
                .status(status)
                .startTime(start)
                .endTime(start.plusMillis(durationMs))
                .build();
    }
}
