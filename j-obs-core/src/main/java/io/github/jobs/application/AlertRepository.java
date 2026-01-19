package io.github.jobs.application;

import io.github.jobs.domain.alert.Alert;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing alert configurations.
 */
public interface AlertRepository {

    /**
     * Saves an alert configuration.
     */
    Alert save(Alert alert);

    /**
     * Finds an alert by ID.
     */
    Optional<Alert> findById(String id);

    /**
     * Returns all configured alerts.
     */
    List<Alert> findAll();

    /**
     * Returns all enabled alerts.
     */
    List<Alert> findEnabled();

    /**
     * Deletes an alert by ID.
     */
    boolean delete(String id);

    /**
     * Returns the count of all alerts.
     */
    long count();
}
