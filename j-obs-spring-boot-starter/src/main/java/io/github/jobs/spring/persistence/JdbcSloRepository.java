package io.github.jobs.spring.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.application.SloRepository;
import io.github.jobs.domain.slo.BurnRate;
import io.github.jobs.domain.slo.ErrorBudget;
import io.github.jobs.domain.slo.Sli;
import io.github.jobs.domain.slo.SliType;
import io.github.jobs.domain.slo.Slo;
import io.github.jobs.domain.slo.SloEvaluation;
import io.github.jobs.domain.slo.SloStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed implementation of SloRepository.
 */
public class JdbcSloRepository implements SloRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcSloRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SloRowMapper sloRowMapper = new SloRowMapper();

    public JdbcSloRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(Slo slo) {
        try {
            String configJson = serializeSloConfig(slo);

            int updated = jdbcTemplate.update(
                    "UPDATE j_obs_slos SET description = ?, sli_type = ?, metric = ?, objective = ?, " +
                    "window_days = ?, config_json = ? WHERE name = ?",
                    slo.description(),
                    slo.sli().type().name(),
                    slo.sli().metric(),
                    slo.objective(),
                    (int) slo.window().toDays(),
                    configJson,
                    slo.name()
            );

            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO j_obs_slos (name, description, sli_type, metric, objective, window_days, config_json) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        slo.name(),
                        slo.description(),
                        slo.sli().type().name(),
                        slo.sli().metric(),
                        slo.objective(),
                        (int) slo.window().toDays(),
                        configJson
                );
            }
        } catch (DataAccessException e) {
            log.error("Failed to save SLO {}: {}", slo.name(), e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<Slo> findByName(String name) {
        try {
            Slo slo = jdbcTemplate.queryForObject(
                    "SELECT * FROM j_obs_slos WHERE name = ?", sloRowMapper, name);
            return Optional.ofNullable(slo);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find SLO {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Slo> findAll() {
        try {
            return jdbcTemplate.query("SELECT * FROM j_obs_slos ORDER BY name", sloRowMapper);
        } catch (DataAccessException e) {
            log.error("Failed to find all SLOs: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean delete(String name) {
        try {
            // Evaluations are cascade-deleted via FK
            int deleted = jdbcTemplate.update("DELETE FROM j_obs_slos WHERE name = ?", name);
            return deleted > 0;
        } catch (DataAccessException e) {
            log.error("Failed to delete SLO {}: {}", name, e.getMessage());
            return false;
        }
    }

    @Override
    public int count() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM j_obs_slos", Integer.class);
            return result != null ? result : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count SLOs: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public void saveEvaluation(SloEvaluation evaluation) {
        try {
            double maxBurnRate = evaluation.getMaxBurnRate();

            jdbcTemplate.update(
                    "INSERT INTO j_obs_slo_evaluations (slo_name, status, current_value, error_budget_remaining, " +
                    "burn_rate, evaluated_at) VALUES (?, ?, ?, ?, ?, ?)",
                    evaluation.sloName(),
                    evaluation.status().name(),
                    evaluation.currentValue(),
                    evaluation.errorBudget().remainingPercentage(),
                    maxBurnRate,
                    Timestamp.from(evaluation.evaluatedAt())
            );
        } catch (DataAccessException e) {
            log.error("Failed to save SLO evaluation for {}: {}", evaluation.sloName(), e.getMessage());
        }
    }

    @Override
    public Optional<SloEvaluation> getLatestEvaluation(String sloName) {
        try {
            Optional<Slo> sloOpt = findByName(sloName);
            if (sloOpt.isEmpty()) {
                return Optional.empty();
            }

            Slo slo = sloOpt.get();
            List<SloEvaluation> evaluations = jdbcTemplate.query(
                    "SELECT * FROM j_obs_slo_evaluations WHERE slo_name = ? ORDER BY evaluated_at DESC LIMIT 1",
                    new SloEvaluationRowMapper(slo),
                    sloName
            );

            return evaluations.isEmpty() ? Optional.empty() : Optional.of(evaluations.get(0));
        } catch (DataAccessException e) {
            log.error("Failed to get latest evaluation for SLO {}: {}", sloName, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<SloEvaluation> getAllLatestEvaluations() {
        List<Slo> allSlos = findAll();
        List<SloEvaluation> results = new ArrayList<>();

        for (Slo slo : allSlos) {
            try {
                List<SloEvaluation> evaluations = jdbcTemplate.query(
                        "SELECT * FROM j_obs_slo_evaluations WHERE slo_name = ? ORDER BY evaluated_at DESC LIMIT 1",
                        new SloEvaluationRowMapper(slo),
                        slo.name()
                );
                if (!evaluations.isEmpty()) {
                    results.add(evaluations.get(0));
                }
            } catch (DataAccessException e) {
                log.error("Failed to get latest evaluation for SLO {}: {}", slo.name(), e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<SloEvaluation> getEvaluationHistory(String sloName, int limit) {
        try {
            Optional<Slo> sloOpt = findByName(sloName);
            if (sloOpt.isEmpty()) {
                return List.of();
            }

            return jdbcTemplate.query(
                    "SELECT * FROM j_obs_slo_evaluations WHERE slo_name = ? ORDER BY evaluated_at DESC LIMIT ?",
                    new SloEvaluationRowMapper(sloOpt.get()),
                    sloName, limit
            );
        } catch (DataAccessException e) {
            log.error("Failed to get evaluation history for SLO {}: {}", sloName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void clearEvaluations() {
        try {
            jdbcTemplate.update("DELETE FROM j_obs_slo_evaluations");
        } catch (DataAccessException e) {
            log.error("Failed to clear SLO evaluations: {}", e.getMessage());
        }
    }

    private String serializeSloConfig(Slo slo) {
        try {
            SloConfigDto dto = new SloConfigDto(
                    slo.sli().goodCondition(),
                    slo.sli().totalCondition(),
                    slo.sli().threshold(),
                    slo.sli().percentile(),
                    slo.burnRateAlerts()
            );
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize SLO config: {}", e.getMessage());
            return null;
        }
    }

    private SloConfigDto deserializeSloConfig(String json) {
        if (json == null || json.isBlank()) {
            return new SloConfigDto(null, null, null, null, null);
        }
        try {
            return objectMapper.readValue(json, SloConfigDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize SLO config: {}", e.getMessage());
            return new SloConfigDto(null, null, null, null, null);
        }
    }

    /**
     * DTO for serializing SLO configuration details to JSON.
     */
    record SloConfigDto(
            String goodCondition,
            String totalCondition,
            Double threshold,
            Integer percentile,
            Slo.BurnRateAlerts burnRateAlerts
    ) {}

    private class SloRowMapper implements RowMapper<Slo> {
        @Override
        public Slo mapRow(ResultSet rs, int rowNum) throws SQLException {
            SliType sliType = SliType.valueOf(rs.getString("sli_type"));
            String metric = rs.getString("metric");
            SloConfigDto config = deserializeSloConfig(rs.getString("config_json"));

            Sli sli = new Sli(
                    sliType,
                    metric,
                    config.goodCondition(),
                    config.totalCondition(),
                    config.threshold(),
                    config.percentile()
            );

            return new Slo(
                    rs.getString("name"),
                    rs.getString("description"),
                    sli,
                    rs.getDouble("objective"),
                    Duration.ofDays(rs.getInt("window_days")),
                    config.burnRateAlerts()
            );
        }
    }

    private static class SloEvaluationRowMapper implements RowMapper<SloEvaluation> {
        private final Slo slo;

        SloEvaluationRowMapper(Slo slo) {
            this.slo = slo;
        }

        @Override
        public SloEvaluation mapRow(ResultSet rs, int rowNum) throws SQLException {
            double currentValue = rs.getDouble("current_value");
            SloStatus status = SloStatus.valueOf(rs.getString("status"));
            double errorBudgetRemaining = rs.getDouble("error_budget_remaining");

            ErrorBudget errorBudget = ErrorBudget.calculate(slo.objective(), currentValue, slo.window());

            double burnRateValue = rs.getDouble("burn_rate");
            List<BurnRate> burnRates = burnRateValue > 0
                    ? List.of(BurnRate.zero(Duration.ofHours(1)))
                    : List.of();

            return new SloEvaluation(
                    rs.getString("slo_name"),
                    slo,
                    currentValue,
                    status,
                    errorBudget,
                    burnRates,
                    0,
                    0,
                    rs.getTimestamp("evaluated_at").toInstant()
            );
        }
    }
}
