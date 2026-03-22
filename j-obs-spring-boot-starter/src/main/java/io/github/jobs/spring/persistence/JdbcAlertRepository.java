package io.github.jobs.spring.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.application.AlertRepository;
import io.github.jobs.domain.alert.Alert;
import io.github.jobs.domain.alert.AlertCondition;
import io.github.jobs.domain.alert.AlertSeverity;
import io.github.jobs.domain.alert.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed implementation of AlertRepository.
 */
public class JdbcAlertRepository implements AlertRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcAlertRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAlertRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Alert save(Alert alert) {
        try {
            String conditionJson = objectMapper.writeValueAsString(alert.condition());

            int updated = jdbcTemplate.update(
                    "UPDATE j_obs_alert_rules SET name = ?, description = ?, type = ?, severity = ?, " +
                    "enabled = ?, condition_json = ?, updated_at = ? WHERE id = ?",
                    alert.name(),
                    alert.description(),
                    alert.type().name(),
                    alert.severity().name(),
                    alert.enabled(),
                    conditionJson,
                    Timestamp.from(alert.updatedAt()),
                    alert.id()
            );

            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO j_obs_alert_rules (id, name, description, type, severity, enabled, condition_json, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        alert.id(),
                        alert.name(),
                        alert.description(),
                        alert.type().name(),
                        alert.severity().name(),
                        alert.enabled(),
                        conditionJson,
                        Timestamp.from(alert.createdAt()),
                        Timestamp.from(alert.updatedAt())
                );
            }

            return alert;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize alert condition: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize alert condition", e);
        } catch (DataAccessException e) {
            log.error("Failed to save alert {}: {}", alert.id(), e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Alert> findById(String id) {
        try {
            Alert alert = jdbcTemplate.queryForObject(
                    "SELECT * FROM j_obs_alert_rules WHERE id = ?",
                    new AlertRowMapper(),
                    id
            );
            return Optional.ofNullable(alert);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find alert {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Alert> findAll() {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_rules ORDER BY created_at DESC",
                    new AlertRowMapper()
            );
        } catch (DataAccessException e) {
            log.error("Failed to find all alerts: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Alert> findEnabled() {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_alert_rules WHERE enabled = TRUE ORDER BY created_at DESC",
                    new AlertRowMapper()
            );
        } catch (DataAccessException e) {
            log.error("Failed to find enabled alerts: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            int deleted = jdbcTemplate.update("DELETE FROM j_obs_alert_rules WHERE id = ?", id);
            return deleted > 0;
        } catch (DataAccessException e) {
            log.error("Failed to delete alert {}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public long count() {
        try {
            Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_alert_rules", Long.class);
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count alerts: {}", e.getMessage());
            return 0;
        }
    }

    private class AlertRowMapper implements RowMapper<Alert> {
        @Override
        public Alert mapRow(ResultSet rs, int rowNum) throws SQLException {
            AlertCondition condition;
            try {
                condition = objectMapper.readValue(rs.getString("condition_json"), AlertCondition.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize alert condition for alert {}: {}", rs.getString("id"), e.getMessage());
                condition = AlertCondition.of("unknown", AlertCondition.ComparisonOperator.GREATER_THAN, 0);
            }

            return Alert.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .type(AlertType.valueOf(rs.getString("type")))
                    .severity(AlertSeverity.valueOf(rs.getString("severity")))
                    .enabled(rs.getBoolean("enabled"))
                    .condition(condition)
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .build();
        }
    }
}
