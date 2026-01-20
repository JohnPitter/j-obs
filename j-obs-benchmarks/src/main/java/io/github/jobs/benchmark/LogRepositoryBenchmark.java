package io.github.jobs.benchmark;

import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.domain.log.LogQuery;
import io.github.jobs.infrastructure.InMemoryLogRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for InMemoryLogRepository operations.
 * Measures throughput of add, query, and count operations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx512m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class LogRepositoryBenchmark {

    private InMemoryLogRepository repository;
    private LogEntry sampleEntry;
    private int counter;

    @Param({"1000", "10000"})
    private int repositorySize;

    @Setup(Level.Trial)
    public void setupTrial() {
        repository = new InMemoryLogRepository(repositorySize);
        // Pre-populate repository
        for (int i = 0; i < repositorySize / 2; i++) {
            repository.add(createEntry(i));
        }
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        sampleEntry = createEntry(counter++);
    }

    private LogEntry createEntry(int index) {
        LogLevel level = LogLevel.values()[index % LogLevel.values().length];
        return LogEntry.builder()
                .timestamp(Instant.now())
                .level(level)
                .loggerName("io.github.jobs.benchmark.Test" + (index % 10))
                .message("Benchmark log message number " + index)
                .threadName("main")
                .traceId("trace-" + index)
                .build();
    }

    @Benchmark
    public void add(Blackhole bh) {
        repository.add(sampleEntry);
        bh.consume(sampleEntry);
    }

    @Benchmark
    public void query_Recent100(Blackhole bh) {
        LogQuery query = LogQuery.builder().limit(100).build();
        List<LogEntry> entries = repository.query(query);
        bh.consume(entries);
    }

    @Benchmark
    public void query_Recent500(Blackhole bh) {
        LogQuery query = LogQuery.builder().limit(500).build();
        List<LogEntry> entries = repository.query(query);
        bh.consume(entries);
    }

    @Benchmark
    public void query_ByMinLevel_INFO(Blackhole bh) {
        LogQuery query = LogQuery.builder()
                .minLevel(LogLevel.INFO)
                .limit(100)
                .build();
        List<LogEntry> entries = repository.query(query);
        bh.consume(entries);
    }

    @Benchmark
    public void query_ByMinLevel_ERROR(Blackhole bh) {
        LogQuery query = LogQuery.builder()
                .minLevel(LogLevel.ERROR)
                .limit(100)
                .build();
        List<LogEntry> entries = repository.query(query);
        bh.consume(entries);
    }

    @Benchmark
    public void count(Blackhole bh) {
        long count = repository.count();
        bh.consume(count);
    }

    @Benchmark
    public void stats(Blackhole bh) {
        var stats = repository.stats();
        bh.consume(stats);
    }

    @Benchmark
    @Threads(4)
    public void add_Concurrent(Blackhole bh) {
        repository.add(sampleEntry);
        bh.consume(sampleEntry);
    }

    @Benchmark
    @Threads(4)
    public void query_Concurrent(Blackhole bh) {
        LogQuery query = LogQuery.builder().limit(100).build();
        List<LogEntry> entries = repository.query(query);
        bh.consume(entries);
    }
}
