package io.github.jobs.spring.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.SpanEvent;
import io.github.jobs.domain.trace.SpanKind;
import io.github.jobs.domain.trace.SpanStatus;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-backed implementation of TraceRepository.
 * Stores traces and spans in relational database tables.
 */
public class JdbcTraceRepository implements TraceRepository, Closeable {

    private static final Logger log = LoggerFactory.getLogger(JdbcTraceRepository.class);

    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<List<SpanEventDto>> EVENT_LIST_TYPE_REF = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTraceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addSpan(Span span) {
        try {
            ensureTraceExists(span);
            insertSpan(span);
            updateTraceMetadata(span.traceId());
        } catch (DataAccessException e) {
            log.error("Failed to add span {} to trace {}: {}", span.spanId(), span.traceId(), e.getMessage());
        }
    }

    private void ensureTraceExists(Span span) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO j_obs_traces (trace_id, service_name, name, status, start_time, end_time, duration_ms, span_count, has_error) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    span.traceId(),
                    span.serviceName(),
                    span.name(),
                    span.status().name(),
                    Timestamp.from(span.startTime()),
                    span.endTime() != null ? Timestamp.from(span.endTime()) : null,
                    span.durationMs(),
                    1,
                    span.hasError()
            );
        } catch (DuplicateKeyException e) {
            // Trace already exists, which is expected for subsequent spans
        }
    }

    private void insertSpan(Span span) {
        try {
            String attributesJson = serializeMap(span.attributes());
            String eventsJson = serializeEvents(span.events());

            jdbcTemplate.update(
                    "INSERT INTO j_obs_spans (span_id, trace_id, parent_span_id, name, service_name, kind, status, " +
                    "start_time, end_time, duration_ms, attributes, events) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    span.spanId(),
                    span.traceId(),
                    span.parentSpanId(),
                    span.name(),
                    span.serviceName(),
                    span.kind().name(),
                    span.status().name(),
                    Timestamp.from(span.startTime()),
                    span.endTime() != null ? Timestamp.from(span.endTime()) : null,
                    span.durationMs(),
                    attributesJson,
                    eventsJson
            );
        } catch (DuplicateKeyException e) {
            // Span already exists, skip
            log.debug("Span {} already exists, skipping", span.spanId());
        }
    }

    private void updateTraceMetadata(String traceId) {
        // Use subqueries compatible with both H2 and PostgreSQL.
        // Compute duration_ms from min/max timestamps in Java to avoid DB-specific functions.
        Map<String, Object> meta = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cnt, " +
                "MIN(start_time) AS min_start, " +
                "MAX(end_time) AS max_end, " +
                "MAX(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) AS has_err " +
                "FROM j_obs_spans WHERE trace_id = ?",
                traceId
        );

        int spanCount = ((Number) meta.get("cnt")).intValue();
        Timestamp minStart = (Timestamp) meta.get("min_start");
        Timestamp maxEnd = (Timestamp) meta.get("max_end");
        boolean hasError = ((Number) meta.get("has_err")).intValue() > 0;

        Long durationMs = null;
        if (minStart != null && maxEnd != null) {
            durationMs = maxEnd.getTime() - minStart.getTime();
        }

        jdbcTemplate.update(
                "UPDATE j_obs_traces SET span_count = ?, has_error = ?, start_time = ?, end_time = ?, duration_ms = ? " +
                "WHERE trace_id = ?",
                spanCount, hasError, minStart, maxEnd, durationMs, traceId
        );
    }

    @Override
    public Optional<Trace> findByTraceId(String traceId) {
        try {
            List<Span> spans = jdbcTemplate.query(
                    "SELECT * FROM j_obs_spans WHERE trace_id = ? ORDER BY start_time",
                    new SpanRowMapper(),
                    traceId
            );

            if (spans.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(Trace.of(traceId, spans));
        } catch (DataAccessException e) {
            log.error("Failed to find trace {}: {}", traceId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Trace> query(TraceQuery query) {
        try {
            StringBuilder sql = new StringBuilder("SELECT trace_id FROM j_obs_traces WHERE 1=1");
            List<Object> params = new ArrayList<>();

            buildWhereClause(query, sql, params);

            sql.append(" ORDER BY start_time DESC");
            sql.append(" LIMIT ? OFFSET ?");
            params.add(query.limit());
            params.add(query.offset());

            List<String> traceIds = jdbcTemplate.queryForList(
                    sql.toString(), String.class, params.toArray()
            );

            List<Trace> traces = new ArrayList<>(traceIds.size());
            for (String traceId : traceIds) {
                findByTraceId(traceId).ifPresent(traces::add);
            }
            return traces;
        } catch (DataAccessException e) {
            log.error("Failed to query traces: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long count() {
        try {
            Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_traces", Long.class);
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count traces: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long count(TraceQuery query) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM j_obs_traces WHERE 1=1");
            List<Object> params = new ArrayList<>();

            buildWhereClause(query, sql, params);

            Long result = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count traces with query: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public void clear() {
        try {
            jdbcTemplate.update("DELETE FROM j_obs_spans");
            jdbcTemplate.update("DELETE FROM j_obs_traces");
        } catch (DataAccessException e) {
            log.error("Failed to clear traces: {}", e.getMessage());
        }
    }

    @Override
    public TraceStats stats() {
        try {
            Long totalTraces = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_traces", Long.class);
            Long totalSpans = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_spans", Long.class);
            Long errorTraces = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM j_obs_traces WHERE has_error = TRUE", Long.class);

            Double avgDuration = jdbcTemplate.queryForObject(
                    "SELECT AVG(CAST(duration_ms AS DOUBLE PRECISION)) FROM j_obs_traces WHERE duration_ms IS NOT NULL",
                    Double.class);

            // Percentiles via sorted durations
            List<Long> durations = jdbcTemplate.queryForList(
                    "SELECT duration_ms FROM j_obs_traces WHERE duration_ms IS NOT NULL ORDER BY duration_ms",
                    Long.class);

            double p50 = percentile(durations, 50);
            double p95 = percentile(durations, 95);
            double p99 = percentile(durations, 99);

            return new TraceStats(
                    totalTraces != null ? totalTraces : 0,
                    totalSpans != null ? totalSpans : 0,
                    errorTraces != null ? errorTraces : 0,
                    avgDuration != null ? avgDuration : 0,
                    p50, p95, p99
            );
        } catch (DataAccessException e) {
            log.error("Failed to compute trace stats: {}", e.getMessage());
            return TraceStats.empty();
        }
    }

    @Override
    public void close() {
        // No resources to clean up; JdbcTemplate lifecycle is managed by Spring
    }

    private void buildWhereClause(TraceQuery query, StringBuilder sql, List<Object> params) {
        if (query.traceId() != null) {
            sql.append(" AND trace_id = ?");
            params.add(query.traceId());
        }
        if (query.serviceName() != null) {
            sql.append(" AND service_name = ?");
            params.add(query.serviceName());
        }
        if (query.status() != null) {
            if (query.status() == SpanStatus.ERROR) {
                sql.append(" AND has_error = TRUE");
            } else {
                sql.append(" AND has_error = FALSE");
            }
        }
        if (query.minDuration() != null) {
            sql.append(" AND duration_ms >= ?");
            params.add(query.minDuration().toMillis());
        }
        if (query.maxDuration() != null) {
            sql.append(" AND duration_ms <= ?");
            params.add(query.maxDuration().toMillis());
        }
        if (query.startTime() != null) {
            sql.append(" AND start_time >= ?");
            params.add(Timestamp.from(query.startTime()));
        }
        if (query.endTime() != null) {
            sql.append(" AND start_time <= ?");
            params.add(Timestamp.from(query.endTime()));
        }
        if (query.spanName() != null) {
            sql.append(" AND trace_id IN (SELECT trace_id FROM j_obs_spans WHERE name LIKE ?)");
            params.add("%" + query.spanName() + "%");
        }
        if (query.spanKind() != null) {
            sql.append(" AND trace_id IN (SELECT trace_id FROM j_obs_spans WHERE kind = ?)");
            params.add(query.spanKind().name());
        }
    }

    private double percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize map: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> deserializeMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE_REF);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize map: {}", e.getMessage());
            return Map.of();
        }
    }

    private String serializeEvents(List<SpanEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        try {
            List<SpanEventDto> dtos = events.stream()
                    .map(e -> new SpanEventDto(e.name(), e.timestamp().toString(), e.attributes()))
                    .toList();
            return objectMapper.writeValueAsString(dtos);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize events: {}", e.getMessage());
            return null;
        }
    }

    private List<SpanEvent> deserializeEvents(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<SpanEventDto> dtos = objectMapper.readValue(json, EVENT_LIST_TYPE_REF);
            return dtos.stream()
                    .map(dto -> SpanEvent.of(dto.name(), Instant.parse(dto.timestamp()), dto.attributes()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to deserialize events: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * DTO for serializing SpanEvent to JSON.
     */
    record SpanEventDto(String name, String timestamp, Map<String, String> attributes) {}

    /**
     * RowMapper for reconstructing Span objects from database rows.
     */
    private class SpanRowMapper implements RowMapper<Span> {
        @Override
        public Span mapRow(ResultSet rs, int rowNum) throws SQLException {
            Span.Builder builder = Span.builder()
                    .spanId(rs.getString("span_id"))
                    .traceId(rs.getString("trace_id"))
                    .parentSpanId(rs.getString("parent_span_id"))
                    .name(rs.getString("name"))
                    .serviceName(rs.getString("service_name"))
                    .startTime(rs.getTimestamp("start_time").toInstant());

            Timestamp endTime = rs.getTimestamp("end_time");
            if (endTime != null) {
                builder.endTime(endTime.toInstant());
            }

            String kind = rs.getString("kind");
            if (kind != null) {
                try {
                    builder.kind(SpanKind.valueOf(kind));
                } catch (IllegalArgumentException e) {
                    builder.kind(SpanKind.INTERNAL);
                }
            }

            String status = rs.getString("status");
            if (status != null) {
                try {
                    builder.status(SpanStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    builder.status(SpanStatus.UNSET);
                }
            }

            builder.attributes(deserializeMap(rs.getString("attributes")));
            builder.events(deserializeEvents(rs.getString("events")));

            return builder.build();
        }
    }
}
