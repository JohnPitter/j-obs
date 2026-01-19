package io.github.jobs.domain.health;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate result of health checks with overall status and component details.
 */
public record HealthCheckResult(
        HealthStatus status,
        List<HealthComponent> components,
        Instant timestamp
) {
    public HealthCheckResult {
        Objects.requireNonNull(status, "status cannot be null");
        components = components != null ? List.copyOf(components) : List.of();
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Creates a new result using the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a healthy result with no components.
     */
    public static HealthCheckResult healthy() {
        return builder().status(HealthStatus.UP).build();
    }

    /**
     * Creates a result from a list of components.
     */
    public static HealthCheckResult from(List<HealthComponent> components) {
        HealthStatus overall = components.stream()
                .map(HealthComponent::status)
                .reduce(HealthStatus.UP, HealthStatus::combine);
        return new HealthCheckResult(overall, components, Instant.now());
    }

    /**
     * Gets the number of components.
     */
    public int componentCount() {
        return components.size();
    }

    /**
     * Gets the number of healthy components.
     */
    public long healthyCount() {
        return components.stream().filter(HealthComponent::isHealthy).count();
    }

    /**
     * Gets the number of unhealthy components.
     */
    public long unhealthyCount() {
        return components.stream().filter(c -> !c.isHealthy()).count();
    }

    /**
     * Checks if the overall status is healthy.
     */
    public boolean isHealthy() {
        return status.isHealthy();
    }

    /**
     * Gets components filtered by status.
     */
    public List<HealthComponent> componentsByStatus(HealthStatus filterStatus) {
        return components.stream()
                .filter(c -> c.status() == filterStatus)
                .toList();
    }

    /**
     * Gets unhealthy components.
     */
    public List<HealthComponent> unhealthyComponents() {
        return components.stream()
                .filter(c -> !c.isHealthy())
                .toList();
    }

    /**
     * Gets a component by name.
     */
    public Optional<HealthComponent> findComponent(String name) {
        return components.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Gets components grouped by status.
     */
    public Map<HealthStatus, List<HealthComponent>> groupByStatus() {
        return components.stream()
                .collect(Collectors.groupingBy(HealthComponent::status));
    }

    /**
     * Gets a summary string for display.
     */
    public String summary() {
        if (components.isEmpty()) {
            return status.displayName();
        }
        return status.displayName() + " (" + healthyCount() + "/" + componentCount() + " healthy)";
    }

    /**
     * Gets the formatted overall status for display.
     */
    public String formattedStatus() {
        return status.emoji() + " " + summary();
    }

    public static class Builder {
        private HealthStatus status = HealthStatus.UP;
        private final List<HealthComponent> components = new ArrayList<>();
        private Instant timestamp;

        public Builder status(HealthStatus status) {
            this.status = status;
            return this;
        }

        public Builder component(HealthComponent component) {
            if (component != null) {
                this.components.add(component);
            }
            return this;
        }

        public Builder components(List<HealthComponent> components) {
            if (components != null) {
                this.components.addAll(components);
            }
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the result, automatically calculating overall status from components.
         */
        public HealthCheckResult build() {
            HealthStatus calculatedStatus = components.isEmpty() ? status :
                    components.stream()
                            .map(HealthComponent::status)
                            .reduce(HealthStatus.UP, HealthStatus::combine);
            return new HealthCheckResult(calculatedStatus, components, timestamp);
        }
    }
}
