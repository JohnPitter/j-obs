package io.github.jobs.infrastructure;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertEventStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of AlertEventRepository.
 */
public class InMemoryAlertEventRepository implements AlertEventRepository {

    private static final int DEFAULT_MAX_EVENTS = 10000;

    private final Map<String, AlertEvent> events = new ConcurrentHashMap<>();
    private final int maxEvents;

    public InMemoryAlertEventRepository() {
        this(DEFAULT_MAX_EVENTS);
    }

    public InMemoryAlertEventRepository(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    @Override
    public AlertEvent save(AlertEvent event) {
        events.put(event.id(), event);
        evictOldEvents();
        return event;
    }

    @Override
    public Optional<AlertEvent> findById(String id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override
    public List<AlertEvent> findAll() {
        return events.values().stream()
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public List<AlertEvent> findByStatus(AlertEventStatus status) {
        return events.values().stream()
                .filter(e -> e.status() == status)
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public List<AlertEvent> findActive() {
        return events.values().stream()
                .filter(AlertEvent::isActive)
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public List<AlertEvent> findByAlertId(String alertId) {
        return events.values().stream()
                .filter(e -> e.alertId().equals(alertId))
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public Optional<AlertEvent> findLatestByAlertId(String alertId) {
        return events.values().stream()
                .filter(e -> e.alertId().equals(alertId))
                .max(Comparator.comparing(AlertEvent::firedAt));
    }

    @Override
    public List<AlertEvent> findFiredAfter(Instant since) {
        return events.values().stream()
                .filter(e -> e.firedAt().isAfter(since))
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public List<AlertEvent> findInTimeRange(Instant start, Instant end) {
        return events.values().stream()
                .filter(e -> !e.firedAt().isBefore(start) && !e.firedAt().isAfter(end))
                .sorted(Comparator.comparing(AlertEvent::firedAt).reversed())
                .toList();
    }

    @Override
    public boolean delete(String id) {
        return events.remove(id) != null;
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        List<String> toDelete = events.values().stream()
                .filter(e -> e.firedAt().isBefore(cutoff))
                .map(AlertEvent::id)
                .toList();
        toDelete.forEach(events::remove);
        return toDelete.size();
    }

    @Override
    public long count() {
        return events.size();
    }

    @Override
    public long countActive() {
        return events.values().stream()
                .filter(AlertEvent::isActive)
                .count();
    }

    private void evictOldEvents() {
        if (events.size() > maxEvents) {
            events.values().stream()
                    .filter(e -> !e.isActive())
                    .sorted(Comparator.comparing(AlertEvent::firedAt))
                    .limit(events.size() - maxEvents)
                    .map(AlertEvent::id)
                    .toList()
                    .forEach(events::remove);
        }
    }
}
