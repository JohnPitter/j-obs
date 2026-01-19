package io.github.jobs.domain.alert;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an alert event that has been triggered.
 */
public record AlertEvent(
        String id,
        String alertId,
        String alertName,
        AlertSeverity severity,
        AlertEventStatus status,
        String message,
        Map<String, String> labels,
        Instant firedAt,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant resolvedAt,
        String resolvedBy
) {
    public AlertEvent {
        Objects.requireNonNull(alertId, "alertId cannot be null");
        Objects.requireNonNull(alertName, "alertName cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (labels == null) {
            labels = Map.of();
        }
        if (firedAt == null) {
            firedAt = Instant.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy with acknowledged status.
     */
    public AlertEvent acknowledge(String acknowledgedBy) {
        return new AlertEvent(
                id, alertId, alertName, severity, AlertEventStatus.ACKNOWLEDGED,
                message, labels, firedAt, Instant.now(), acknowledgedBy, resolvedAt, resolvedBy
        );
    }

    /**
     * Creates a copy with resolved status.
     */
    public AlertEvent resolve(String resolvedBy) {
        return new AlertEvent(
                id, alertId, alertName, severity, AlertEventStatus.RESOLVED,
                message, labels, firedAt, acknowledgedAt, acknowledgedBy, Instant.now(), resolvedBy
        );
    }

    /**
     * Check if this event is still active (not resolved).
     */
    public boolean isActive() {
        return status != AlertEventStatus.RESOLVED;
    }

    public static class Builder {
        private String id;
        private String alertId;
        private String alertName;
        private AlertSeverity severity = AlertSeverity.WARNING;
        private AlertEventStatus status = AlertEventStatus.FIRING;
        private String message;
        private Map<String, String> labels;
        private Instant firedAt;
        private Instant acknowledgedAt;
        private String acknowledgedBy;
        private Instant resolvedAt;
        private String resolvedBy;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder alertId(String alertId) {
            this.alertId = alertId;
            return this;
        }

        public Builder alertName(String alertName) {
            this.alertName = alertName;
            return this;
        }

        public Builder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder status(AlertEventStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder firedAt(Instant firedAt) {
            this.firedAt = firedAt;
            return this;
        }

        public Builder acknowledgedAt(Instant acknowledgedAt) {
            this.acknowledgedAt = acknowledgedAt;
            return this;
        }

        public Builder acknowledgedBy(String acknowledgedBy) {
            this.acknowledgedBy = acknowledgedBy;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Builder resolvedBy(String resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        public AlertEvent build() {
            return new AlertEvent(
                    id, alertId, alertName, severity, status, message, labels,
                    firedAt, acknowledgedAt, acknowledgedBy, resolvedAt, resolvedBy
            );
        }
    }
}
