-- J-Obs JDBC Schema
-- Compatible with H2 and PostgreSQL

CREATE TABLE IF NOT EXISTS j_obs_traces (
    trace_id VARCHAR(64) PRIMARY KEY,
    service_name VARCHAR(256),
    name VARCHAR(512),
    status VARCHAR(32),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    span_count INT DEFAULT 0,
    has_error BOOLEAN DEFAULT FALSE,
    data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS j_obs_spans (
    span_id VARCHAR(64) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64),
    name VARCHAR(512),
    service_name VARCHAR(256),
    kind VARCHAR(32),
    status VARCHAR(32),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    attributes TEXT,
    events TEXT,
    FOREIGN KEY (trace_id) REFERENCES j_obs_traces(trace_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS j_obs_alert_rules (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    condition_json TEXT NOT NULL,
    providers TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS j_obs_alert_events (
    id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64),
    alert_name VARCHAR(256),
    severity VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    message TEXT,
    labels TEXT,
    fired_at TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(256),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(256),
    FOREIGN KEY (alert_id) REFERENCES j_obs_alert_rules(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS j_obs_slos (
    name VARCHAR(256) PRIMARY KEY,
    description TEXT,
    sli_type VARCHAR(32) NOT NULL,
    metric VARCHAR(512),
    objective DOUBLE PRECISION NOT NULL,
    window_days INT NOT NULL,
    config_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS j_obs_slo_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slo_name VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_value DOUBLE PRECISION,
    error_budget_remaining DOUBLE PRECISION,
    burn_rate DOUBLE PRECISION,
    evaluated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (slo_name) REFERENCES j_obs_slos(name) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_j_obs_spans_trace_id ON j_obs_spans(trace_id);
CREATE INDEX IF NOT EXISTS idx_j_obs_traces_start_time ON j_obs_traces(start_time);
CREATE INDEX IF NOT EXISTS idx_j_obs_traces_service ON j_obs_traces(service_name);
CREATE INDEX IF NOT EXISTS idx_j_obs_alert_events_alert_id ON j_obs_alert_events(alert_id);
CREATE INDEX IF NOT EXISTS idx_j_obs_alert_events_status ON j_obs_alert_events(status);
CREATE INDEX IF NOT EXISTS idx_j_obs_slo_evals_name ON j_obs_slo_evaluations(slo_name);
CREATE INDEX IF NOT EXISTS idx_j_obs_slo_evals_time ON j_obs_slo_evaluations(evaluated_at);
