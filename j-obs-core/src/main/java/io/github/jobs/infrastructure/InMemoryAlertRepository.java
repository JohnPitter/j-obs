package io.github.jobs.infrastructure;

import io.github.jobs.application.AlertRepository;
import io.github.jobs.domain.alert.Alert;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of AlertRepository.
 */
public class InMemoryAlertRepository implements AlertRepository {

    private final Map<String, Alert> alerts = new ConcurrentHashMap<>();

    @Override
    public Alert save(Alert alert) {
        alerts.put(alert.id(), alert);
        return alert;
    }

    @Override
    public Optional<Alert> findById(String id) {
        return Optional.ofNullable(alerts.get(id));
    }

    @Override
    public List<Alert> findAll() {
        return List.copyOf(alerts.values());
    }

    @Override
    public List<Alert> findEnabled() {
        return alerts.values().stream()
                .filter(Alert::enabled)
                .toList();
    }

    @Override
    public boolean delete(String id) {
        return alerts.remove(id) != null;
    }

    @Override
    public long count() {
        return alerts.size();
    }
}
