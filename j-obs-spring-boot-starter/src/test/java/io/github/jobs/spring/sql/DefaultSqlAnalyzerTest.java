package io.github.jobs.spring.sql;

import io.github.jobs.application.SqlAnalyzer.QueryStats;
import io.github.jobs.application.SqlAnalyzer.SqlAnalysisStats;
import io.github.jobs.domain.sql.SqlIssue;
import io.github.jobs.domain.sql.SqlProblemType;
import io.github.jobs.domain.sql.SqlQuery;
import io.github.jobs.domain.trace.*;
import io.github.jobs.infrastructure.InMemoryTraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultSqlAnalyzerTest {

    private InMemoryTraceRepository traceRepository;
    private SqlAnalyzerConfig config;
    private DefaultSqlAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        traceRepository = new InMemoryTraceRepository(Duration.ofHours(1), 1000);
        config = new SqlAnalyzerConfig();
        config.setSlowQueryThreshold(Duration.ofSeconds(1));
        config.setVerySlowQueryThreshold(Duration.ofSeconds(5));
        config.setNPlusOneMinQueries(3);
        config.setNPlusOneSimilarity(0.9);
        analyzer = new DefaultSqlAnalyzer(traceRepository, config);
    }

    @Test
    void shouldDetectNPlusOneQuery() {
        // Create a trace with multiple similar queries (N+1 pattern)
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        // Add root span
        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/orders", SpanKind.SERVER, baseTime, 500));

        // Add 5 similar SELECT queries (N+1 pattern)
        for (int i = 1; i <= 5; i++) {
            traceRepository.addSpan(createDbSpan(
                    traceId,
                    "span-" + i,
                    "span-0",
                    "SELECT * FROM order_items WHERE order_id = " + i,
                    "SELECT",
                    baseTime.plusMillis(i * 10),
                    50
            ));
        }

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertTrue(issues.stream().anyMatch(i -> i.type() == SqlProblemType.N_PLUS_ONE),
                "Should detect N+1 query pattern");
    }

    @Test
    void shouldDetectSlowQuery() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 2000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 1500));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertTrue(issues.stream().anyMatch(i -> i.type() == SqlProblemType.SLOW_QUERY),
                "Should detect slow query");
    }

    @Test
    void shouldDetectVerySlowQuery() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/reports", SpanKind.SERVER, baseTime, 6000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM large_table", "SELECT", baseTime.plusMillis(10), 5500));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertTrue(issues.stream().anyMatch(i -> i.type() == SqlProblemType.VERY_SLOW_QUERY),
                "Should detect very slow query");
    }

    @Test
    void shouldDetectSelectStar() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 100));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users WHERE id = 1", "SELECT", baseTime.plusMillis(10), 50));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertTrue(issues.stream().anyMatch(i -> i.type() == SqlProblemType.SELECT_STAR),
                "Should detect SELECT * usage");
    }

    @Test
    void shouldDetectMissingLimit() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 100));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT id, name FROM users WHERE active = true", "SELECT", baseTime.plusMillis(10), 50));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertTrue(issues.stream().anyMatch(i -> i.type() == SqlProblemType.MISSING_LIMIT),
                "Should detect missing LIMIT clause");
    }

    @Test
    void shouldNotDetectMissingLimitWhenDisabled() {
        config.setDetectMissingLimit(false);

        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 100));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT id FROM users", "SELECT", baseTime.plusMillis(10), 50));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertFalse(issues.stream().anyMatch(i -> i.type() == SqlProblemType.MISSING_LIMIT),
                "Should not detect missing LIMIT when disabled");
    }

    @Test
    void shouldNotDetectSelectStarWhenDisabled() {
        config.setDetectSelectStar(false);

        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 100));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 50));

        List<SqlIssue> issues = analyzer.analyze(Duration.ofHours(1));

        assertFalse(issues.stream().anyMatch(i -> i.type() == SqlProblemType.SELECT_STAR),
                "Should not detect SELECT * when disabled");
    }

    @Test
    void shouldAnalyzeSpecificTrace() {
        String traceId = "trace-123";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 2000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 1500));

        List<SqlIssue> issues = analyzer.analyzeTrace(traceId);

        assertFalse(issues.isEmpty(), "Should find issues in specific trace");
    }

    @Test
    void shouldReturnEmptyForUnknownTrace() {
        List<SqlIssue> issues = analyzer.analyzeTrace("unknown-trace-id");
        assertTrue(issues.isEmpty());
    }

    @Test
    void shouldGetIssuesBySeverity() {
        // Create traces with different issue types
        String traceId1 = "trace-1";
        Instant baseTime1 = Instant.now();
        traceRepository.addSpan(createSpan(traceId1, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime1, 6000));
        traceRepository.addSpan(createDbSpan(traceId1, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime1.plusMillis(10), 5500));

        String traceId2 = "trace-2";
        Instant baseTime2 = Instant.now().plusMillis(100);
        traceRepository.addSpan(createSpan(traceId2, "span-0", null, "GET /api/orders", SpanKind.SERVER, baseTime2, 100));
        traceRepository.addSpan(createDbSpan(traceId2, "span-1", "span-0", "SELECT id FROM orders", "SELECT", baseTime2.plusMillis(10), 50));

        analyzer.analyze(Duration.ofHours(1));

        List<SqlIssue> allIssues = analyzer.getIssues(null);
        List<SqlIssue> criticalOnly = analyzer.getIssues(SqlProblemType.VERY_SLOW_QUERY);

        assertTrue(allIssues.size() >= criticalOnly.size());
    }

    @Test
    void shouldGetSlowQueries() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 2000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 1500));

        List<SqlQuery> slowQueries = analyzer.getSlowQueries(Duration.ofSeconds(1), Duration.ofHours(1));

        assertFalse(slowQueries.isEmpty(), "Should find slow queries");
        assertTrue(slowQueries.get(0).durationMs() >= 1000);
    }

    @Test
    void shouldGetTopQueries() {
        // Create multiple traces with the same query pattern
        for (int i = 0; i < 5; i++) {
            String traceId = "trace-" + i;
            Instant baseTime = Instant.now().plusMillis(i * 100);
            traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 100));
            traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT id, name FROM users WHERE id = " + i, "SELECT", baseTime.plusMillis(10), 50 + i * 10));
        }

        List<QueryStats> topQueries = analyzer.getTopQueries(10, Duration.ofHours(1));

        assertFalse(topQueries.isEmpty(), "Should find top queries");
        assertTrue(topQueries.get(0).executionCount() >= 1);
    }

    @Test
    void shouldGetStats() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 6000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 5500));

        analyzer.analyze(Duration.ofHours(1));

        SqlAnalysisStats stats = analyzer.getStats();

        assertTrue(stats.totalIssues() > 0);
    }

    @Test
    void shouldClearIssues() {
        String traceId = "trace-1";
        Instant baseTime = Instant.now();

        traceRepository.addSpan(createSpan(traceId, "span-0", null, "GET /api/users", SpanKind.SERVER, baseTime, 6000));
        traceRepository.addSpan(createDbSpan(traceId, "span-1", "span-0", "SELECT * FROM users", "SELECT", baseTime.plusMillis(10), 5500));

        analyzer.analyze(Duration.ofHours(1));

        assertFalse(analyzer.getIssues(null).isEmpty());

        analyzer.clearIssues();

        assertTrue(analyzer.getIssues(null).isEmpty());
    }

    private Span createSpan(String traceId, String spanId, String parentSpanId, String name, SpanKind kind, Instant startTime, long durationMs) {
        Map<String, String> attributes = new HashMap<>();
        if (name.startsWith("GET") || name.startsWith("POST")) {
            attributes.put("http.method", name.split(" ")[0]);
            attributes.put("http.url", name.contains(" ") ? name.split(" ")[1] : "/");
        }

        return Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(name)
                .kind(kind)
                .status(SpanStatus.OK)
                .startTime(startTime)
                .endTime(startTime.plusMillis(durationMs))
                .attributes(attributes)
                .build();
    }

    private Span createDbSpan(String traceId, String spanId, String parentSpanId, String statement, String operation, Instant startTime, long durationMs) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("db.system", "postgresql");
        attributes.put("db.statement", statement);
        attributes.put("db.operation", operation);
        attributes.put("db.name", "testdb");

        return Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(operation + " " + extractTableName(statement))
                .kind(SpanKind.CLIENT)
                .status(SpanStatus.OK)
                .startTime(startTime)
                .endTime(startTime.plusMillis(durationMs))
                .attributes(attributes)
                .build();
    }

    private String extractTableName(String statement) {
        if (statement.contains("FROM")) {
            String[] parts = statement.split("FROM\\s+");
            if (parts.length > 1) {
                return parts[1].split("\\s+")[0];
            }
        }
        return "unknown";
    }
}
