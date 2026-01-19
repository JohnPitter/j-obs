package io.github.jobs.spring.health;

import io.github.jobs.application.HealthRepository;
import io.github.jobs.domain.health.HealthCheckResult;
import io.github.jobs.domain.health.HealthComponent;
import io.github.jobs.domain.health.HealthStatus;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.SystemHealth;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HealthRepository implementation backed by Spring Boot Actuator HealthEndpoint.
 */
public class ActuatorHealthRepository implements HealthRepository {

    private final HealthEndpoint healthEndpoint;
    private final Map<String, List<HealthHistoryEntry>> history = new ConcurrentHashMap<>();
    private final int maxHistoryEntries;
    private volatile Instant lastCheckTime = Instant.MIN;
    private volatile HealthCheckResult cachedResult;

    public ActuatorHealthRepository(HealthEndpoint healthEndpoint) {
        this(healthEndpoint, 100);
    }

    public ActuatorHealthRepository(HealthEndpoint healthEndpoint, int maxHistoryEntries) {
        this.healthEndpoint = healthEndpoint;
        this.maxHistoryEntries = maxHistoryEntries;
    }

    @Override
    public HealthCheckResult getHealth() {
        if (cachedResult != null && Duration.between(lastCheckTime, Instant.now()).toSeconds() < 5) {
            return cachedResult;
        }
        return refresh();
    }

    @Override
    public Optional<HealthComponent> getComponentHealth(String componentName) {
        return getComponents().stream()
                .filter(c -> c.name().equalsIgnoreCase(componentName))
                .findFirst();
    }

    @Override
    public List<HealthComponent> getComponents() {
        return getHealth().components();
    }

    @Override
    public HealthCheckResult refresh() {
        try {
            SystemHealth systemHealth = (SystemHealth) healthEndpoint.health();
            List<HealthComponent> components = extractComponents(systemHealth);

            // Record history
            Instant now = Instant.now();
            for (HealthComponent component : components) {
                recordHistory(component.name(), component.status(), component.error(), now);
            }

            lastCheckTime = now;
            cachedResult = HealthCheckResult.from(components);
            return cachedResult;
        } catch (Exception e) {
            lastCheckTime = Instant.now();
            cachedResult = HealthCheckResult.builder()
                    .status(HealthStatus.UNKNOWN)
                    .component(HealthComponent.down("actuator", e.getMessage()))
                    .build();
            return cachedResult;
        }
    }

    @Override
    public Instant getLastCheckTime() {
        return lastCheckTime;
    }

    @Override
    public List<HealthHistoryEntry> getHistory(String componentName, Duration duration) {
        List<HealthHistoryEntry> entries = history.getOrDefault(componentName.toLowerCase(), List.of());
        Instant cutoff = Instant.now().minus(duration);
        return entries.stream()
                .filter(e -> e.timestamp().isAfter(cutoff))
                .toList();
    }

    private List<HealthComponent> extractComponents(SystemHealth systemHealth) {
        List<HealthComponent> components = new ArrayList<>();

        Map<String, org.springframework.boot.actuate.health.HealthComponent> details = systemHealth.getComponents();
        if (details != null) {
            for (Map.Entry<String, org.springframework.boot.actuate.health.HealthComponent> entry : details.entrySet()) {
                String name = entry.getKey();
                org.springframework.boot.actuate.health.HealthComponent value = entry.getValue();

                if (value instanceof Health health) {
                    components.add(convertHealth(name, health));
                } else if (value instanceof SystemHealth nestedHealth) {
                    // Handle nested health groups
                    components.add(HealthComponent.builder()
                            .name(name)
                            .status(convertStatus(nestedHealth.getStatus()))
                            .build());
                }
            }
        }

        // Add overall component
        components.add(0, HealthComponent.builder()
                .name("overall")
                .displayName("Overall Status")
                .status(convertStatus(systemHealth.getStatus()))
                .build());

        return components;
    }

    private HealthComponent convertHealth(String name, Health health) {
        HealthStatus status = convertStatus(health.getStatus());
        Map<String, Object> details = health.getDetails();

        String error = null;
        if (details.containsKey("error")) {
            error = String.valueOf(details.get("error"));
        }

        return HealthComponent.builder()
                .name(name)
                .status(status)
                .details(details)
                .error(error)
                .build();
    }

    private HealthComponent convertHealthMap(String name, Map<String, Object> healthMap) {
        String statusStr = String.valueOf(healthMap.getOrDefault("status", "UNKNOWN"));
        HealthStatus status = convertStatusString(statusStr);

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) healthMap.getOrDefault("details", Map.of());

        String error = null;
        if (details.containsKey("error")) {
            error = String.valueOf(details.get("error"));
        }

        return HealthComponent.builder()
                .name(name)
                .status(status)
                .details(details)
                .error(error)
                .build();
    }

    private HealthStatus convertStatus(Status status) {
        if (status == null) {
            return HealthStatus.UNKNOWN;
        }
        return convertStatusString(status.getCode());
    }

    private HealthStatus convertStatusString(String statusCode) {
        if (statusCode == null) {
            return HealthStatus.UNKNOWN;
        }
        return switch (statusCode.toUpperCase()) {
            case "UP" -> HealthStatus.UP;
            case "DEGRADED", "WARNING", "WARN" -> HealthStatus.DEGRADED;
            case "DOWN" -> HealthStatus.DOWN;
            case "OUT_OF_SERVICE" -> HealthStatus.OUT_OF_SERVICE;
            default -> HealthStatus.UNKNOWN;
        };
    }

    private void recordHistory(String componentName, HealthStatus status, String error, Instant timestamp) {
        String key = componentName.toLowerCase();
        history.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new HealthHistoryEntry(timestamp, status, error));

        // Trim history
        List<HealthHistoryEntry> entries = history.get(key);
        if (entries.size() > maxHistoryEntries) {
            history.put(key, new ArrayList<>(entries.subList(
                    entries.size() - maxHistoryEntries, entries.size())));
        }
    }
}
