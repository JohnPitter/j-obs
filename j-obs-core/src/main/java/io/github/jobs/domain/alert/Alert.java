package io.github.jobs.domain.alert;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an alert configuration.
 */
public record Alert(
        String id,
        String name,
        String description,
        AlertType type,
        AlertCondition condition,
        AlertSeverity severity,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public Alert {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(condition, "condition cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy with updated values.
     */
    public Alert withEnabled(boolean enabled) {
        return new Alert(id, name, description, type, condition, severity, enabled, createdAt, Instant.now());
    }

    public static class Builder {
        private String id;
        private String name;
        private String description;
        private AlertType type;
        private AlertCondition condition;
        private AlertSeverity severity = AlertSeverity.WARNING;
        private boolean enabled = true;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(AlertType type) {
            this.type = type;
            return this;
        }

        public Builder condition(AlertCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Alert build() {
            return new Alert(id, name, description, type, condition, severity, enabled, createdAt, updatedAt);
        }
    }
}
