package io.github.jobs.spring.web;

import io.github.jobs.application.LogRepository;
import io.github.jobs.application.LogRepository.LogStats;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.domain.log.LogQuery;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for log data.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/logs")
public class LogApiController {

    private final LogRepository logRepository;

    public LogApiController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public LogsResponse getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String logger,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String thread,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        // Sanitize inputs
        int sanitizedLimit = InputSanitizer.sanitizeLimit(limit);
        int sanitizedOffset = InputSanitizer.sanitizeOffset(offset);
        String sanitizedLogger = InputSanitizer.sanitizeLogger(logger);
        String sanitizedMessage = InputSanitizer.sanitizeMessage(message);
        String sanitizedTraceId = InputSanitizer.sanitizeTraceId(traceId);
        String sanitizedThread = InputSanitizer.sanitizeThreadName(thread);

        LogQuery.Builder queryBuilder = LogQuery.builder()
                .limit(sanitizedLimit)
                .offset(sanitizedOffset);

        if (level != null && !level.isEmpty()) {
            queryBuilder.minLevel(LogLevel.fromString(level));
        }
        if (sanitizedLogger != null) {
            queryBuilder.loggerName(sanitizedLogger);
        }
        if (sanitizedMessage != null) {
            queryBuilder.messagePattern(sanitizedMessage);
        }
        if (sanitizedTraceId != null) {
            queryBuilder.traceId(sanitizedTraceId);
        }
        if (sanitizedThread != null) {
            queryBuilder.threadName(sanitizedThread);
        }

        LogQuery query = queryBuilder.build();
        List<LogEntry> logs = logRepository.query(query);
        long total = logRepository.count(query);

        return new LogsResponse(
                logs.stream().map(LogEntryDto::from).toList(),
                total,
                sanitizedLimit,
                sanitizedOffset
        );
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public LogStats getStats() {
        return logRepository.stats();
    }

    @GetMapping(value = "/levels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> getLevels() {
        return List.of(
                levelInfo(LogLevel.TRACE),
                levelInfo(LogLevel.DEBUG),
                levelInfo(LogLevel.INFO),
                levelInfo(LogLevel.WARN),
                levelInfo(LogLevel.ERROR)
        );
    }

    private Map<String, Object> levelInfo(LogLevel level) {
        return Map.of(
                "name", level.name(),
                "displayName", level.displayName(),
                "severity", level.severity(),
                "textCss", level.textCssClass(),
                "bgCss", level.bgCssClass()
        );
    }

    @GetMapping(value = "/loggers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getLoggers() {
        return logRepository.query(LogQuery.recent(1000)).stream()
                .map(LogEntry::loggerName)
                .filter(l -> l != null && !l.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearLogs() {
        logRepository.clear();
        return ResponseEntity.noContent().build();
    }

    // DTOs

    public record LogsResponse(
            List<LogEntryDto> logs,
            long total,
            int limit,
            int offset
    ) {}

    public record LogEntryDto(
            String id,
            Instant timestamp,
            String level,
            String levelCss,
            String levelBgCss,
            String logger,
            String shortLogger,
            String message,
            String thread,
            String traceId,
            String spanId,
            boolean hasThrowable,
            String throwable,
            Map<String, String> mdc
    ) {
        public static LogEntryDto from(LogEntry entry) {
            return new LogEntryDto(
                    entry.id(),
                    entry.timestamp(),
                    entry.level().name(),
                    entry.level().textCssClass(),
                    entry.level().bgCssClass(),
                    entry.loggerName(),
                    entry.shortLoggerName(),
                    entry.message(),
                    entry.threadName(),
                    entry.traceId(),
                    entry.spanId(),
                    entry.hasThrowable(),
                    entry.throwable(),
                    entry.mdc()
            );
        }
    }
}
