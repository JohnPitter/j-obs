package io.github.jobs.domain.health;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a health indicator component with its status and details.
 */
public record HealthComponent(
        String name,
        String displayName,
        HealthStatus status,
        Map<String, Object> details,
        Instant checkedAt,
        String error
) {
    public HealthComponent {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        details = details != null ? Map.copyOf(details) : Map.of();
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }

    /**
     * Creates a new component using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a healthy component.
     */
    public static HealthComponent up(String name) {
        return builder().name(name).status(HealthStatus.UP).build();
    }

    /**
     * Creates a healthy component with details.
     */
    public static HealthComponent up(String name, Map<String, Object> details) {
        return builder().name(name).status(HealthStatus.UP).details(details).build();
    }

    /**
     * Creates a down component.
     */
    public static HealthComponent down(String name, String error) {
        return builder().name(name).status(HealthStatus.DOWN).error(error).build();
    }

    /**
     * Creates a down component with details.
     */
    public static HealthComponent down(String name, String error, Map<String, Object> details) {
        return builder().name(name).status(HealthStatus.DOWN).error(error).details(details).build();
    }

    /**
     * Gets the component identifier (lowercase name).
     */
    public String id() {
        return name.toLowerCase().replace(" ", "-");
    }

    /**
     * Gets the display name, falling back to name if not set.
     */
    public String getDisplayName() {
        return displayName != null ? displayName : formatDisplayName(name);
    }

    private static String formatDisplayName(String name) {
        if (name == null) return "";
        return name.substring(0, 1).toUpperCase() +
               name.substring(1).replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Checks if this component is healthy.
     */
    public boolean isHealthy() {
        return status.isHealthy();
    }

    /**
     * Checks if this component has an error.
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Gets a detail value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return (T) details.get(key);
    }

    /**
     * Gets a detail value with a default.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key, T defaultValue) {
        Object value = details.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Gets the formatted status for display.
     */
    public String formattedStatus() {
        return status.emoji() + " " + status.displayName();
    }

    public static class Builder {
        private String name;
        private String displayName;
        private HealthStatus status = HealthStatus.UNKNOWN;
        private final HashMap<String, Object> details = new HashMap<>();
        private Instant checkedAt;
        private String error;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder status(HealthStatus status) {
            this.status = status;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (key != null && value != null) {
                this.details.put(key, value);
            }
            return this;
        }

        public Builder details(Map<String, Object> details) {
            if (details != null) {
                this.details.putAll(details);
            }
            return this;
        }

        public Builder checkedAt(Instant checkedAt) {
            this.checkedAt = checkedAt;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public HealthComponent build() {
            return new HealthComponent(name, displayName, status, details, checkedAt, error);
        }
    }
}
