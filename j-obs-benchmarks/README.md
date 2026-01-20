# J-Obs Benchmarks

Performance benchmarks for J-Obs using JMH (Java Microbenchmark Harness).

## Available Benchmarks

### LogEntry Benchmarks
- `createLogEntry_Simple` - Basic LogEntry creation
- `createLogEntry_WithTraceContext` - LogEntry with trace/span IDs
- `createLogEntry_WithLongMessage` - LogEntry with long message
- `createLogEntry_AllLevels` - Create entries for all log levels
- `createLogEntry_Concurrent` - Concurrent LogEntry creation (4 threads)

### LogRepository Benchmarks
- `add` - Add single entry to repository
- `query_Recent100` - Query last 100 entries
- `query_Recent500` - Query last 500 entries
- `query_ByMinLevel_INFO` - Query by minimum log level (INFO)
- `query_ByMinLevel_ERROR` - Query by minimum log level (ERROR)
- `count` - Count total entries
- `stats` - Get repository statistics
- `add_Concurrent` - Concurrent add operations (4 threads)
- `query_Concurrent` - Concurrent query operations (4 threads)

### TraceRepository Benchmarks
- `addSpan` - Add single span to repository
- `findByTraceId` - Find trace by ID
- `query_Recent50` - Query last 50 traces
- `query_Recent200` - Query last 200 traces
- `query_ByServiceName` - Query by service name
- `count` - Count total traces
- `addSpan_Concurrent` - Concurrent addSpan operations (4 threads)
- `query_Concurrent` - Concurrent query operations (4 threads)

## Running Benchmarks

### Build the benchmark JAR

```bash
cd j-obs
mvn package -pl j-obs-benchmarks -am -DskipTests
```

### List available benchmarks

```bash
java -jar j-obs-benchmarks/target/benchmarks.jar -l
```

### Run all benchmarks (full suite ~10 minutes)

```bash
java -jar j-obs-benchmarks/target/benchmarks.jar
```

### Run specific benchmark

```bash
java -jar j-obs-benchmarks/target/benchmarks.jar "LogEntryBenchmark.*"
```

### Run with custom parameters

```bash
# Quick test: 1 fork, 1 warmup, 1 measurement iteration
java -jar j-obs-benchmarks/target/benchmarks.jar -f 1 -wi 1 -i 1

# Thorough test: 3 forks, 5 warmup, 10 measurement iterations
java -jar j-obs-benchmarks/target/benchmarks.jar -f 3 -wi 5 -i 10
```

### Export results to JSON

```bash
java -jar j-obs-benchmarks/target/benchmarks.jar -rf json -rff results.json
```

## Benchmark Parameters

- **repositorySize**: Size of the repository buffer (1000 or 10000)

## Interpreting Results

Results are in **ops/μs** (operations per microsecond):
- Higher is better
- 1 ops/μs = 1,000,000 ops/second

Example output:
```
Benchmark                                 Mode  Cnt   Score   Error   Units
LogEntryBenchmark.createLogEntry_Simple  thrpt   10   2.772 ± 0.123  ops/us
```

This means ~2.77 million LogEntry objects can be created per second.

## Performance Tips

1. **LogEntry creation**: ~2-3M ops/sec (very fast)
2. **Repository add**: ~0.5-1M ops/sec depending on buffer size
3. **Repository query**: ~10-50K ops/sec for 100 entries
4. **Concurrent operations**: Scale linearly with threads up to CPU cores
