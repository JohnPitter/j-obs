package io.github.jobs.spring.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertEventStatus;
import io.github.jobs.domain.alert.AlertSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC-backed implementation of AlertEventRepository.
 */
public class JdbcAlertEventRepository implements AlertEventRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAlertEventRepository.class);
    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AlertEventRowMapper rowMapper = new AlertEventRowMapper();

    public JdbcAlertEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public AlertEvent save(AlertEvent event) {
        try {
            String labelsJson = serializeMap(event.labels());

            int updated = jdbcTemplate.update(
                    "UPDATE j_obs_alert_events SET status = ?, message = ?, labels = ?, " +
                    "acknowledged_at = ?, acknowledged_by = ?, resolved_at = ?, resolved_by = ? WHERE id = ?",
                    event.status().name(),
                    event.message(),
                    labelsJson,
                    event.acknowledgedAt() != null ? Timestamp.from(event.acknowledgedAt()) : null,
                    event.acknowledgedBy(),
                    event.resolvedAt() != null ? Timestamp.from(event.resolvedAt()) : null,
                    event.resolvedBy(),
                    event.id()
            );

            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO j_obs_alert_events (id, alert_id, alert_name, severity, status, message, labels, " +
                        "fired_at, acknowledged_at, acknowledged_by, resolved_at, resolved_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        event.id(),
                        event.alertId(),
                        event.alertName(),
                        event.severity().name(),
                        event.status().name(),
                        event.message(),
                        labelsJson,
                        Timestamp.from(event.firedAt()),
                        event.acknowledgedAt() != null ? Timestamp.from(event.acknowledgedAt()) : null,
                        event.acknowledgedBy(),
                        event.resolvedAt() != null ? Timestamp.from(event.resolvedAt()) : null,
                        event.resolvedBy()
                );
            }

            return event;
        } catch (DataAccessException e) {
            log.error("Failed to save alert event {}: {}", event.id(), e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<AlertEvent> findById(String id) {
        try {
            AlertEvent event = jdbcTemplate.queryForObject(
                    "SELECT * FROM j_obs_alert_events WHERE id = ?", rowMapper, id);
            return Optional.ofNullable(event);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find alert event {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<AlertEvent> findAll() {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events ORDER BY fired_at DESC", rowMapper);
        } catch (DataAccessException e) {
            log.error("Failed to find all alert events: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<AlertEvent> findByStatus(AlertEventStatus status) {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE status = ? ORDER BY fired_at DESC",
                    rowMapper, status.name());
        } catch (DataAccessException e) {
            log.error("Failed to find alert events by status {}: {}", status, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<AlertEvent> findActive() {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE status != ? ORDER BY fired_at DESC",
                    rowMapper, AlertEventStatus.RESOLVED.name());
        } catch (DataAccessException e) {
            log.error("Failed to find active alert events: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<AlertEvent> findByAlertId(String alertId) {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE alert_id = ? ORDER BY fired_at DESC",
                    rowMapper, alertId);
        } catch (DataAccessException e) {
            log.error("Failed to find alert events for alert {}: {}", alertId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<AlertEvent> findLatestByAlertId(String alertId) {
        try {
            List<AlertEvent> events = jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE alert_id = ? ORDER BY fired_at DESC LIMIT 1",
                    rowMapper, alertId);
            return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
        } catch (DataAccessException e) {
            log.error("Failed to find latest alert event for alert {}: {}", alertId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<AlertEvent> findFiredAfter(Instant since) {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE fired_at > ? ORDER BY fired_at DESC",
                    rowMapper, Timestamp.from(since));
        } catch (DataAccessException e) {
            log.error("Failed to find alert events fired after {}: {}", since, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<AlertEvent> findInTimeRange(Instant start, Instant end) {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_events WHERE fired_at >= ? AND fired_at <= ? ORDER BY fired_at DESC",
                    rowMapper, Timestamp.from(start), Timestamp.from(end));
        } catch (DataAccessException e) {
            log.error("Failed to find alert events in time range: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM j_obs_alert_events WHERE id = ?", id);
            return deleted > 0;
        } catch (DataAccessException e) {
            log.error("Failed to delete alert event {}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        try {
            return jdbcTemplate.update(
                    "DELETE FROM j_obs_alert_events WHERE fired_at < ?",
                    Timestamp.from(cutoff));
        } catch (DataAccessException e) {
            log.error("Failed to delete old alert events: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long count() {
        try {
            Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_alert_events", Long.class);
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count alert events: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long countActive() {
        try {
            Long result = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM j_obs_alert_events WHERE status != ?",
                    Long.class, AlertEventStatus.RESOLVED.name());
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count active alert events: {}", e.getMessage());
            return 0;
        }
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize labels: {}", e.getMessage());
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
            log.warn("Failed to deserialize labels: {}", e.getMessage());
            return Map.of();
        }
    }

    private class AlertEventRowMapper implements RowMapper<AlertEvent> {
        @Override
        public AlertEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp acknowledgedAt = rs.getTimestamp("acknowledged_at");
            Timestamp resolvedAt = rs.getTimestamp("resolved_at");

            return AlertEvent.builder()
                    .id(rs.getString("id"))
                    .alertId(rs.getString("alert_id"))
                    .alertName(rs.getString("alert_name"))
                    .severity(AlertSeverity.valueOf(rs.getString("severity")))
                    .status(AlertEventStatus.valueOf(rs.getString("status")))
                    .message(rs.getString("message"))
                    .labels(deserializeMap(rs.getString("labels")))
                    .firedAt(rs.getTimestamp("fired_at").toInstant())
                    .acknowledgedAt(acknowledgedAt != null ? acknowledgedAt.toInstant() : null)
                    .acknowledgedBy(rs.getString("acknowledged_by"))
                    .resolvedAt(resolvedAt != null ? resolvedAt.toInstant() : null)
                    .resolvedBy(rs.getString("resolved_by"))
                    .build();
        }
    }
}
