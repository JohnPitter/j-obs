package io.github.jobs.benchmark;

import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.SpanKind;
import io.github.jobs.domain.trace.SpanStatus;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;
import io.github.jobs.infrastructure.InMemoryTraceRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for InMemoryTraceRepository operations.
 * Measures throughput of addSpan, findByTraceId, and query operations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class TraceRepositoryBenchmark {

    private InMemoryTraceRepository repository;
    private Span sampleSpan;
    private String existingTraceId;
    private int counter;

    @Param({"1000", "5000"})
    private int repositorySize;

    @Setup(Level.Trial)
    public void setupTrial() {
        repository = new InMemoryTraceRepository(Duration.ofHours(1), repositorySize);
        // Pre-populate repository
        for (int i = 0; i < repositorySize / 2; i++) {
            Span span = createSpan(i);
            repository.addSpan(span);
            if (i == repositorySize / 4) {
                existingTraceId = span.traceId();
            }
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        sampleSpan = createSpan(counter++);
    }

    private Span createSpan(int index) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant startTime = Instant.now().minusSeconds(index % 3600);

        return Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(index > 0 ? "parent-" + (index - 1) : null)
                .name("benchmark-operation-" + (index % 10))
                .kind(SpanKind.SERVER)
                .startTime(startTime)
                .endTime(startTime.plusMillis(50 + (index % 100)))
                .status(SpanStatus.OK)
                .serviceName("benchmark-service")
                .attribute("http.method", "GET")
                .attribute("http.url", "/api/test/" + index)
                .attribute("http.status_code", "200")
                .build();
    }

    @Benchmark
    public void addSpan(Blackhole bh) {
        repository.addSpan(sampleSpan);
        bh.consume(sampleSpan);
    }

    @Benchmark
    public void findByTraceId(Blackhole bh) {
        Optional<Trace> trace = repository.findByTraceId(existingTraceId);
        bh.consume(trace);
    }

    @Benchmark
    public void query_Recent50(Blackhole bh) {
        TraceQuery query = TraceQuery.builder().limit(50).build();
        List<Trace> traces = repository.query(query);
        bh.consume(traces);
    }

    @Benchmark
    public void query_Recent200(Blackhole bh) {
        TraceQuery query = TraceQuery.builder().limit(200).build();
        List<Trace> traces = repository.query(query);
        bh.consume(traces);
    }

    @Benchmark
    public void query_ByServiceName(Blackhole bh) {
        TraceQuery query = TraceQuery.builder()
                .serviceName("benchmark-service")
                .limit(100)
                .build();
        List<Trace> traces = repository.query(query);
        bh.consume(traces);
    }

    @Benchmark
    public void count(Blackhole bh) {
        long count = repository.count();
        bh.consume(count);
    }

    @Benchmark
    @Threads(4)
    public void addSpan_Concurrent(Blackhole bh) {
        repository.addSpan(sampleSpan);
        bh.consume(sampleSpan);
    }

    @Benchmark
    @Threads(4)
    public void query_Concurrent(Blackhole bh) {
        TraceQuery query = TraceQuery.builder().limit(50).build();
        List<Trace> traces = repository.query(query);
        bh.consume(traces);
    }
}
