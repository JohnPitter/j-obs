package io.github.jobs.spring.web;

import io.github.jobs.application.SqlAnalyzer;
import io.github.jobs.application.SqlAnalyzer.QueryStats;
import io.github.jobs.application.SqlAnalyzer.SqlAnalysisStats;
import io.github.jobs.domain.sql.SqlIssue;
import io.github.jobs.domain.sql.SqlProblemType;
import io.github.jobs.domain.sql.SqlQuery;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * REST API controller for SQL analysis.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/sql")
public class SqlAnalyzerApiController {

    private final SqlAnalyzer sqlAnalyzer;
    private final JObsProperties properties;

    public SqlAnalyzerApiController(SqlAnalyzer sqlAnalyzer, JObsProperties properties) {
        this.sqlAnalyzer = sqlAnalyzer;
        this.properties = properties;
    }

    @GetMapping(value = "/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse analyze(
            @RequestParam(defaultValue = "1h") String window) {
        Duration duration = parseDuration(window);
        List<SqlIssue> issues = sqlAnalyzer.analyze(duration);
        return new AnalysisResponse(
                issues.stream().map(SqlIssueDto::from).toList(),
                issues.size(),
                issues.stream().filter(SqlIssue::isCritical).count(),
                issues.stream().filter(i -> i.type().isWarning()).count()
        );
    }

    @GetMapping(value = "/analyze/{traceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeTrace(@PathVariable String traceId) {
        String sanitizedTraceId = sanitizeInput(traceId);
        List<SqlIssue> issues = sqlAnalyzer.analyzeTrace(sanitizedTraceId);
        if (issues.isEmpty()) {
            return ResponseEntity.ok(new AnalysisResponse(List.of(), 0, 0, 0));
        }
        return ResponseEntity.ok(new AnalysisResponse(
                issues.stream().map(SqlIssueDto::from).toList(),
                issues.size(),
                issues.stream().filter(SqlIssue::isCritical).count(),
                issues.stream().filter(i -> i.type().isWarning()).count()
        ));
    }

    @GetMapping(value = "/issues", produces = MediaType.APPLICATION_JSON_VALUE)
    public IssuesResponse getIssues(
            @RequestParam(required = false) String severity) {
        SqlProblemType minSeverity = severity != null ? parseProblemType(severity) : null;
        List<SqlIssue> issues = sqlAnalyzer.getIssues(minSeverity);
        return new IssuesResponse(
                issues.stream().map(SqlIssueDto::from).toList(),
                issues.size()
        );
    }

    @GetMapping(value = "/slow-queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public SlowQueriesResponse getSlowQueries(
            @RequestParam(defaultValue = "1s") String threshold,
            @RequestParam(defaultValue = "1h") String window) {
        Duration thresholdDuration = parseDuration(threshold);
        Duration windowDuration = parseDuration(window);
        List<SqlQuery> queries = sqlAnalyzer.getSlowQueries(thresholdDuration, windowDuration);
        return new SlowQueriesResponse(
                queries.stream().map(SqlQueryDto::from).toList(),
                queries.size(),
                thresholdDuration.toMillis()
        );
    }

    @GetMapping(value = "/top-queries", produces = MediaType.APPLICATION_JSON_VALUE)
    public TopQueriesResponse getTopQueries(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "1h") String window) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        Duration windowDuration = parseDuration(window);
        List<QueryStats> stats = sqlAnalyzer.getTopQueries(safeLimit, windowDuration);
        return new TopQueriesResponse(
                stats.stream().map(QueryStatsDto::from).toList(),
                stats.size()
        );
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatsResponse getStats() {
        SqlAnalysisStats stats = sqlAnalyzer.getStats();
        return new StatsResponse(
                stats.totalQueries(),
                stats.totalIssues(),
                stats.criticalIssues(),
                stats.warningIssues(),
                stats.nPlusOneDetected(),
                stats.slowQueriesDetected(),
                stats.avgQueryDurationMs()
        );
    }

    @DeleteMapping(value = "/issues")
    public ResponseEntity<Void> clearIssues() {
        sqlAnalyzer.clearIssues();
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/problem-types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProblemTypeDto> getProblemTypes() {
        return Arrays.stream(SqlProblemType.values())
                .map(ProblemTypeDto::from)
                .toList();
    }

    private Duration parseDuration(String duration) {
        if (duration == null || duration.isBlank()) {
            return Duration.ofHours(1);
        }
        String trimmed = duration.trim().toLowerCase();
        try {
            if (trimmed.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(trimmed.replace("ms", "")));
            } else if (trimmed.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(trimmed.replace("s", "")));
            } else if (trimmed.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(trimmed.replace("m", "")));
            } else if (trimmed.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(trimmed.replace("h", "")));
            } else if (trimmed.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(trimmed.replace("d", "")));
            }
            return Duration.parse("PT" + trimmed.toUpperCase());
        } catch (Exception e) {
            return Duration.ofHours(1);
        }
    }

    private SqlProblemType parseProblemType(String type) {
        try {
            return SqlProblemType.valueOf(type.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9\\-_]", "").substring(0, Math.min(input.length(), 64));
    }

    // DTOs

    public record AnalysisResponse(
            List<SqlIssueDto> issues,
            long total,
            long criticalCount,
            long warningCount
    ) {}

    public record IssuesResponse(
            List<SqlIssueDto> issues,
            long total
    ) {}

    public record SlowQueriesResponse(
            List<SqlQueryDto> queries,
            long total,
            long thresholdMs
    ) {}

    public record TopQueriesResponse(
            List<QueryStatsDto> queries,
            long total
    ) {}

    public record StatsResponse(
            long totalQueries,
            long totalIssues,
            long criticalIssues,
            long warningIssues,
            long nPlusOneDetected,
            long slowQueriesDetected,
            double avgQueryDurationMs
    ) {}

    public record SqlIssueDto(
            String id,
            String type,
            String typeDisplayName,
            String severity,
            String cssClass,
            String bgCssClass,
            String traceId,
            String endpoint,
            String query,
            String normalizedQuery,
            String dbSystem,
            long durationMs,
            int occurrences,
            Instant detectedAt,
            String suggestion,
            List<String> relatedSpanIds
    ) {
        public static SqlIssueDto from(SqlIssue issue) {
            return new SqlIssueDto(
                    issue.id(),
                    issue.type().name(),
                    issue.type().displayName(),
                    issue.type().severity(),
                    issue.type().cssClass(),
                    issue.type().bgCssClass(),
                    issue.traceId(),
                    issue.endpoint(),
                    issue.query(),
                    issue.normalizedQuery(),
                    issue.dbSystem(),
                    issue.durationMs(),
                    issue.occurrences(),
                    issue.detectedAt(),
                    issue.suggestion(),
                    issue.relatedSpanIds()
            );
        }
    }

    public record SqlQueryDto(
            String spanId,
            String traceId,
            String statement,
            String normalizedStatement,
            String operation,
            String dbSystem,
            String dbName,
            long durationMs,
            Instant executedAt,
            String endpoint
    ) {
        public static SqlQueryDto from(SqlQuery query) {
            return new SqlQueryDto(
                    query.spanId(),
                    query.traceId(),
                    query.statement(),
                    query.normalizedStatement(),
                    query.operation(),
                    query.dbSystem(),
                    query.dbName(),
                    query.durationMs(),
                    query.executedAt(),
                    query.endpoint()
            );
        }
    }

    public record QueryStatsDto(
            String normalizedQuery,
            String operation,
            String dbSystem,
            long executionCount,
            double avgDurationMs,
            long minDurationMs,
            long maxDurationMs,
            long p50DurationMs,
            long p95DurationMs,
            long p99DurationMs
    ) {
        public static QueryStatsDto from(QueryStats stats) {
            return new QueryStatsDto(
                    stats.normalizedQuery(),
                    stats.operation(),
                    stats.dbSystem(),
                    stats.executionCount(),
                    stats.avgDurationMs(),
                    stats.minDurationMs(),
                    stats.maxDurationMs(),
                    stats.p50DurationMs(),
                    stats.p95DurationMs(),
                    stats.p99DurationMs()
            );
        }
    }

    public record ProblemTypeDto(
            String name,
            String displayName,
            String severity,
            String description,
            String cssClass,
            String bgCssClass,
            boolean isCritical
    ) {
        public static ProblemTypeDto from(SqlProblemType type) {
            return new ProblemTypeDto(
                    type.name(),
                    type.displayName(),
                    type.severity(),
                    type.description(),
                    type.cssClass(),
                    type.bgCssClass(),
                    type.isCritical()
            );
        }
    }
}
