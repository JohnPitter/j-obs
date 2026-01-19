package io.github.jobs.spring.sql;

import io.github.jobs.application.SqlAnalyzer;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.sql.SqlIssue;
import io.github.jobs.domain.sql.SqlProblemType;
import io.github.jobs.domain.sql.SqlQuery;
import io.github.jobs.domain.trace.Span;
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
 * Default implementation of SqlAnalyzer that analyzes traces for SQL issues.
 */
public class DefaultSqlAnalyzer implements SqlAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DefaultSqlAnalyzer.class);

    private final TraceRepository traceRepository;
    private final SqlAnalyzerConfig config;
    private final Map<String, SqlIssue> detectedIssues = new ConcurrentHashMap<>();

    public DefaultSqlAnalyzer(TraceRepository traceRepository, SqlAnalyzerConfig config) {
        this.traceRepository = traceRepository;
        this.config = config;
    }

    @Override
    public List<SqlIssue> analyze(Duration window) {
        Instant since = Instant.now().minus(window);
        List<Trace> traces = traceRepository.query(TraceQuery.builder()
                .startTime(since)
                .limit(1000)
                .build());

        List<SqlIssue> issues = new ArrayList<>();
        for (Trace trace : traces) {
            issues.addAll(analyzeTraceInternal(trace));
        }

        // Store detected issues
        for (SqlIssue issue : issues) {
            detectedIssues.put(issue.id(), issue);
        }

        return issues;
    }

    @Override
    public List<SqlIssue> analyzeTrace(String traceId) {
        return traceRepository.findByTraceId(traceId)
                .map(this::analyzeTraceInternal)
                .orElse(List.of());
    }

    private List<SqlIssue> analyzeTraceInternal(Trace trace) {
        List<SqlIssue> issues = new ArrayList<>();
        List<SqlQuery> queries = extractQueries(trace);

        if (queries.isEmpty()) {
            return issues;
        }

        // Detect N+1 queries
        issues.addAll(detectNPlusOne(trace, queries));

        // Detect slow queries
        issues.addAll(detectSlowQueries(trace, queries));

        // Detect SELECT *
        if (config.isDetectSelectStar()) {
            issues.addAll(detectSelectStar(trace, queries));
        }

        // Detect missing LIMIT
        if (config.isDetectMissingLimit()) {
            issues.addAll(detectMissingLimit(trace, queries));
        }

        return issues;
    }

    private List<SqlQuery> extractQueries(Trace trace) {
        String endpoint = extractEndpoint(trace);

        return trace.spans().stream()
                .filter(span -> span.dbStatement() != null && !span.dbStatement().isEmpty())
                .map(span -> new SqlQuery(
                        span.spanId(),
                        span.traceId(),
                        span.dbStatement(),
                        SqlQuery.normalize(span.dbStatement()),
                        span.dbOperation() != null ? span.dbOperation() : SqlQuery.extractOperation(span.dbStatement()),
                        span.dbSystem(),
                        span.attribute("db.name"),
                        span.durationMs(),
                        span.startTime(),
                        endpoint
                ))
                .toList();
    }

    private String extractEndpoint(Trace trace) {
        String method = trace.httpMethod();
        String url = trace.httpUrl();
        if (method != null && url != null) {
            // Normalize URL by removing query params and IDs
            String normalizedUrl = url.replaceAll("\\?.*", "").replaceAll("/\\d+", "/{id}");
            return method + " " + normalizedUrl;
        }
        return trace.name();
    }

    private List<SqlIssue> detectNPlusOne(Trace trace, List<SqlQuery> queries) {
        List<SqlIssue> issues = new ArrayList<>();

        // Group queries by normalized statement
        Map<String, List<SqlQuery>> grouped = queries.stream()
                .filter(q -> "SELECT".equals(q.operation()))
                .collect(Collectors.groupingBy(SqlQuery::normalizedStatement));

        for (Map.Entry<String, List<SqlQuery>> entry : grouped.entrySet()) {
            List<SqlQuery> similarQueries = entry.getValue();

            if (similarQueries.size() >= config.getNPlusOneMinQueries()) {
                // Check if queries have high similarity
                SqlQuery first = similarQueries.get(0);
                long similarCount = similarQueries.stream()
                        .filter(q -> first.similarityTo(q) >= config.getNPlusOneSimilarity())
                        .count();

                if (similarCount >= config.getNPlusOneMinQueries()) {
                    String id = generateIssueId(trace.traceId(), "n+1", entry.getKey());
                    long totalDuration = similarQueries.stream().mapToLong(SqlQuery::durationMs).sum();

                    String suggestion = generateNPlusOneSuggestion(first);

                    SqlIssue issue = SqlIssue.builder()
                            .id(id)
                            .type(SqlProblemType.N_PLUS_ONE)
                            .traceId(trace.traceId())
                            .endpoint(first.endpoint())
                            .query(first.statement())
                            .normalizedQuery(first.normalizedStatement())
                            .dbSystem(first.dbSystem())
                            .durationMs(totalDuration)
                            .occurrences((int) similarCount)
                            .detectedAt(Instant.now())
                            .suggestion(suggestion)
                            .relatedSpanIds(similarQueries.stream()
                                    .map(SqlQuery::spanId)
                                    .toList())
                            .build();

                    issues.add(issue);
                    log.debug("Detected N+1 query in trace {}: {} occurrences", trace.traceId(), similarCount);
                }
            }
        }

        return issues;
    }

    private String generateNPlusOneSuggestion(SqlQuery query) {
        String table = SqlQuery.extractTable(query.statement());
        if (table != null) {
            return "Consider using a JOIN to fetch related data in a single query:\n" +
                    "SELECT ... FROM parent_table\n" +
                    "LEFT JOIN " + table + " ON parent_table.id = " + table + ".parent_id\n" +
                    "Or use @EntityGraph/@BatchSize in JPA.";
        }
        return "Consider using a JOIN to fetch related data in a single query, " +
                "or use @EntityGraph/@BatchSize in JPA.";
    }

    private List<SqlIssue> detectSlowQueries(Trace trace, List<SqlQuery> queries) {
        List<SqlIssue> issues = new ArrayList<>();
        long slowThreshold = config.getSlowQueryThreshold().toMillis();
        long verySlowThreshold = config.getVerySlowQueryThreshold().toMillis();

        for (SqlQuery query : queries) {
            SqlProblemType type = null;
            String suggestion = null;

            if (query.durationMs() >= verySlowThreshold) {
                type = SqlProblemType.VERY_SLOW_QUERY;
                suggestion = "This query is critically slow. Consider:\n" +
                        "• Adding appropriate indexes\n" +
                        "• Optimizing the query plan\n" +
                        "• Using query caching\n" +
                        "• Reviewing JOIN conditions";
            } else if (query.durationMs() >= slowThreshold) {
                type = SqlProblemType.SLOW_QUERY;
                suggestion = "Consider adding an index or optimizing the query.";
            }

            if (type != null) {
                String id = generateIssueId(trace.traceId(), "slow", query.spanId());

                SqlIssue issue = SqlIssue.builder()
                        .id(id)
                        .type(type)
                        .traceId(trace.traceId())
                        .endpoint(query.endpoint())
                        .query(query.statement())
                        .normalizedQuery(query.normalizedStatement())
                        .dbSystem(query.dbSystem())
                        .durationMs(query.durationMs())
                        .occurrences(1)
                        .detectedAt(Instant.now())
                        .suggestion(suggestion)
                        .relatedSpanIds(List.of(query.spanId()))
                        .build();

                issues.add(issue);
            }
        }

        return issues;
    }

    private List<SqlIssue> detectSelectStar(Trace trace, List<SqlQuery> queries) {
        List<SqlIssue> issues = new ArrayList<>();

        for (SqlQuery query : queries) {
            if (query.usesSelectStar()) {
                String id = generateIssueId(trace.traceId(), "select-star", query.spanId());
                String table = SqlQuery.extractTable(query.statement());

                String suggestion = table != null ?
                        "Replace SELECT * with specific columns from " + table :
                        "Replace SELECT * with specific columns to reduce data transfer.";

                SqlIssue issue = SqlIssue.builder()
                        .id(id)
                        .type(SqlProblemType.SELECT_STAR)
                        .traceId(trace.traceId())
                        .endpoint(query.endpoint())
                        .query(query.statement())
                        .normalizedQuery(query.normalizedStatement())
                        .dbSystem(query.dbSystem())
                        .durationMs(query.durationMs())
                        .detectedAt(Instant.now())
                        .suggestion(suggestion)
                        .relatedSpanIds(List.of(query.spanId()))
                        .build();

                issues.add(issue);
            }
        }

        return issues;
    }

    private List<SqlIssue> detectMissingLimit(Trace trace, List<SqlQuery> queries) {
        List<SqlIssue> issues = new ArrayList<>();

        for (SqlQuery query : queries) {
            if (query.missingLimit()) {
                String id = generateIssueId(trace.traceId(), "no-limit", query.spanId());

                SqlIssue issue = SqlIssue.builder()
                        .id(id)
                        .type(SqlProblemType.MISSING_LIMIT)
                        .traceId(trace.traceId())
                        .endpoint(query.endpoint())
                        .query(query.statement())
                        .normalizedQuery(query.normalizedStatement())
                        .dbSystem(query.dbSystem())
                        .durationMs(query.durationMs())
                        .detectedAt(Instant.now())
                        .suggestion("Add LIMIT clause to prevent large result sets.")
                        .relatedSpanIds(List.of(query.spanId()))
                        .build();

                issues.add(issue);
            }
        }

        return issues;
    }

    private String generateIssueId(String traceId, String type, String key) {
        return type + "-" + traceId.substring(0, Math.min(8, traceId.length())) +
                "-" + key.hashCode();
    }

    @Override
    public List<SqlIssue> getIssues(SqlProblemType minSeverity) {
        return detectedIssues.values().stream()
                .filter(issue -> minSeverity == null ||
                        compareSeverity(issue.type(), minSeverity) >= 0)
                .sorted(Comparator.comparing(SqlIssue::detectedAt).reversed())
                .toList();
    }

    private int compareSeverity(SqlProblemType a, SqlProblemType b) {
        int severityA = getSeverityLevel(a);
        int severityB = getSeverityLevel(b);
        return Integer.compare(severityA, severityB);
    }

    private int getSeverityLevel(SqlProblemType type) {
        return switch (type.severity()) {
            case "critical" -> 3;
            case "warning" -> 2;
            default -> 1;
        };
    }

    @Override
    public List<SqlQuery> getSlowQueries(Duration threshold, Duration window) {
        Instant since = Instant.now().minus(window);
        long thresholdMs = threshold.toMillis();

        return traceRepository.query(TraceQuery.builder()
                        .startTime(since)
                        .limit(1000)
                        .build())
                .stream()
                .flatMap(trace -> extractQueries(trace).stream())
                .filter(q -> q.durationMs() >= thresholdMs)
                .sorted(Comparator.comparingLong(SqlQuery::durationMs).reversed())
                .toList();
    }

    @Override
    public List<QueryStats> getTopQueries(int limit, Duration window) {
        Instant since = Instant.now().minus(window);

        Map<String, List<SqlQuery>> grouped = traceRepository.query(TraceQuery.builder()
                        .startTime(since)
                        .limit(1000)
                        .build())
                .stream()
                .flatMap(trace -> extractQueries(trace).stream())
                .collect(Collectors.groupingBy(SqlQuery::normalizedStatement));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<SqlQuery> queries = entry.getValue();
                    SqlQuery first = queries.get(0);
                    long[] durations = queries.stream()
                            .mapToLong(SqlQuery::durationMs)
                            .sorted()
                            .toArray();

                    return new QueryStats(
                            entry.getKey(),
                            first.operation(),
                            first.dbSystem(),
                            queries.size(),
                            Arrays.stream(durations).average().orElse(0),
                            durations.length > 0 ? durations[0] : 0,
                            durations.length > 0 ? durations[durations.length - 1] : 0,
                            percentile(durations, 50),
                            percentile(durations, 95),
                            percentile(durations, 99)
                    );
                })
                .sorted(Comparator.comparingLong(QueryStats::executionCount).reversed())
                .limit(limit)
                .toList();
    }

    private long percentile(long[] sorted, int percentile) {
        if (sorted.length == 0) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    @Override
    public SqlAnalysisStats getStats() {
        Collection<SqlIssue> issues = detectedIssues.values();

        long critical = issues.stream().filter(SqlIssue::isCritical).count();
        long warning = issues.stream()
                .filter(i -> i.type().isWarning())
                .count();
        long nPlusOne = issues.stream()
                .filter(i -> i.type() == SqlProblemType.N_PLUS_ONE)
                .count();
        long slowQueries = issues.stream()
                .filter(i -> i.type() == SqlProblemType.SLOW_QUERY ||
                        i.type() == SqlProblemType.VERY_SLOW_QUERY)
                .count();

        return new SqlAnalysisStats(
                0, // Would need query count tracking
                issues.size(),
                critical,
                warning,
                nPlusOne,
                slowQueries,
                0.0 // Would need query duration tracking
        );
    }

    @Override
    public void clearIssues() {
        detectedIssues.clear();
    }
}
