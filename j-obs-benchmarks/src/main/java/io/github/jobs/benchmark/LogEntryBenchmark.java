package io.github.jobs.benchmark;

import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for LogEntry creation performance.
 * Measures throughput of LogEntry instantiation using Builder.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms256m", "-Xmx256m"})
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class LogEntryBenchmark {

    private Instant timestamp;

    @Setup
    public void setup() {
        timestamp = Instant.now();
    }

    @Benchmark
    public void createLogEntry_Simple(Blackhole bh) {
        LogEntry entry = LogEntry.builder()
                .timestamp(timestamp)
                .level(LogLevel.INFO)
                .loggerName("io.github.jobs.Test")
                .message("Test message with some content")
                .threadName("main")
                .build();
        bh.consume(entry);
    }

    @Benchmark
    public void createLogEntry_WithTraceContext(Blackhole bh) {
        LogEntry entry = LogEntry.builder()
                .timestamp(timestamp)
                .level(LogLevel.INFO)
                .loggerName("io.github.jobs.Test")
                .message("Test message with trace context")
                .threadName("main")
                .traceId("abc123def456")
                .spanId("span789")
                .build();
        bh.consume(entry);
    }

    @Benchmark
    public void createLogEntry_WithLongMessage(Blackhole bh) {
        LogEntry entry = LogEntry.builder()
                .timestamp(timestamp)
                .level(LogLevel.DEBUG)
                .loggerName("io.github.jobs.very.deep.package.name.ClassName")
                .message("This is a much longer log message that contains more details about what happened " +
                        "during the execution of the application. It includes some context like user ID: 12345, " +
                        "request ID: abc-def-ghi, and timestamp: " + timestamp)
                .threadName("http-nio-8080-exec-1")
                .build();
        bh.consume(entry);
    }

    @Benchmark
    public void createLogEntry_AllLevels(Blackhole bh) {
        for (LogLevel level : LogLevel.values()) {
            LogEntry entry = LogEntry.builder()
                    .timestamp(timestamp)
                    .level(level)
                    .loggerName("io.github.jobs.Test")
                    .message("Message at level " + level)
                    .threadName("main")
                    .build();
            bh.consume(entry);
        }
    }

    @Benchmark
    @Threads(4)
    public void createLogEntry_Concurrent(Blackhole bh) {
        LogEntry entry = LogEntry.builder()
                .timestamp(Instant.now())
                .level(LogLevel.INFO)
                .loggerName("io.github.jobs.Test")
                .message("Concurrent log message")
                .threadName(Thread.currentThread().getName())
                .build();
        bh.consume(entry);
    }
}
