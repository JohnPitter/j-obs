package io.github.jobs.application;

import io.github.jobs.domain.health.HealthCheckResult;
import io.github.jobs.domain.health.HealthComponent;
import io.github.jobs.domain.health.HealthStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for health check operations.
 */
public interface HealthRepository {

    /**
     * Gets the overall health status with all components.
     */
    HealthCheckResult getHealth();

    /**
     * Gets the health status of a specific component.
     */
    Optional<HealthComponent> getComponentHealth(String componentName);

    /**
     * Gets all health components.
     */
    List<HealthComponent> getComponents();

    /**
     * Forces a refresh of all health checks.
     */
    HealthCheckResult refresh();

    /**
     * Gets the timestamp of the last health check.
     */
    Instant getLastCheckTime();

    /**
     * Gets health history for a component (if available).
     */
    List<HealthHistoryEntry> getHistory(String componentName, Duration duration);

    /**
     * Represents a historical health check entry.
     */
    record HealthHistoryEntry(
            Instant timestamp,
            HealthStatus status,
            String error
    ) {}
}
