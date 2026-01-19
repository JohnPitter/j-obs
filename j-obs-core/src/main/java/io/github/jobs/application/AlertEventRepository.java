package io.github.jobs.application;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertEventStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing alert events.
 */
public interface AlertEventRepository {

    /**
     * Saves an alert event.
     */
    AlertEvent save(AlertEvent event);

    /**
     * Finds an alert event by ID.
     */
    Optional<AlertEvent> findById(String id);

    /**
     * Returns all alert events.
     */
    List<AlertEvent> findAll();

    /**
     * Returns alert events by status.
     */
    List<AlertEvent> findByStatus(AlertEventStatus status);

    /**
     * Returns active (non-resolved) alert events.
     */
    List<AlertEvent> findActive();

    /**
     * Returns alert events for a specific alert ID.
     */
    List<AlertEvent> findByAlertId(String alertId);

    /**
     * Returns the most recent event for a specific alert ID.
     */
    Optional<AlertEvent> findLatestByAlertId(String alertId);

    /**
     * Returns alert events fired after a specific time.
     */
    List<AlertEvent> findFiredAfter(Instant since);

    /**
     * Returns alert events within a time range.
     */
    List<AlertEvent> findInTimeRange(Instant start, Instant end);

    /**
     * Deletes an alert event by ID.
     */
    boolean delete(String id);

    /**
     * Deletes events older than a specified time.
     */
    int deleteOlderThan(Instant cutoff);

    /**
     * Returns the count of all events.
     */
    long count();

    /**
     * Returns the count of active events.
     */
    long countActive();
}
