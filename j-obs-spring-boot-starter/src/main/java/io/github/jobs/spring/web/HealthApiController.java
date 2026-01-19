package io.github.jobs.spring.web;

import io.github.jobs.application.HealthRepository;
import io.github.jobs.application.HealthRepository.HealthHistoryEntry;
import io.github.jobs.domain.health.HealthCheckResult;
import io.github.jobs.domain.health.HealthComponent;
import io.github.jobs.domain.health.HealthStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for health check data.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/health")
public class HealthApiController {

    private final HealthRepository healthRepository;

    public HealthApiController(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse getHealth() {
        HealthCheckResult result = healthRepository.getHealth();
        return HealthResponse.from(result);
    }

    @GetMapping(value = "/components", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HealthComponentDto> getComponents() {
        return healthRepository.getComponents().stream()
                .map(HealthComponentDto::from)
                .toList();
    }

    @GetMapping(value = "/components/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthComponentDto getComponent(@PathVariable String name) {
        return healthRepository.getComponentHealth(name)
                .map(HealthComponentDto::from)
                .orElse(null);
    }

    @GetMapping(value = "/components/{name}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HealthHistoryDto> getComponentHistory(
            @PathVariable String name,
            @RequestParam(defaultValue = "3600") long durationSeconds
    ) {
        return healthRepository.getHistory(name, Duration.ofSeconds(durationSeconds)).stream()
                .map(HealthHistoryDto::from)
                .toList();
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResponse refreshHealth() {
        HealthCheckResult result = healthRepository.refresh();
        return HealthResponse.from(result);
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthSummary getSummary() {
        HealthCheckResult result = healthRepository.getHealth();
        return new HealthSummary(
                result.status().name(),
                result.status().displayName(),
                result.isHealthy(),
                result.componentCount(),
                result.healthyCount(),
                result.unhealthyCount(),
                healthRepository.getLastCheckTime()
        );
    }

    // DTOs

    public record HealthResponse(
            String status,
            String statusDisplayName,
            boolean healthy,
            String summary,
            List<HealthComponentDto> components,
            Instant timestamp
    ) {
        public static HealthResponse from(HealthCheckResult result) {
            return new HealthResponse(
                    result.status().name(),
                    result.status().displayName(),
                    result.isHealthy(),
                    result.summary(),
                    result.components().stream().map(HealthComponentDto::from).toList(),
                    result.timestamp()
            );
        }
    }

    public record HealthComponentDto(
            String name,
            String displayName,
            String status,
            String statusDisplayName,
            String statusEmoji,
            String textCssClass,
            String bgCssClass,
            boolean healthy,
            Map<String, Object> details,
            String error,
            Instant checkedAt
    ) {
        public static HealthComponentDto from(HealthComponent component) {
            return new HealthComponentDto(
                    component.name(),
                    component.getDisplayName(),
                    component.status().name(),
                    component.status().displayName(),
                    component.status().emoji(),
                    component.status().textCssClass(),
                    component.status().bgCssClass(),
                    component.isHealthy(),
                    component.details(),
                    component.error(),
                    component.checkedAt()
            );
        }
    }

    public record HealthHistoryDto(
            Instant timestamp,
            String status,
            String error
    ) {
        public static HealthHistoryDto from(HealthHistoryEntry entry) {
            return new HealthHistoryDto(
                    entry.timestamp(),
                    entry.status().name(),
                    entry.error()
            );
        }
    }

    public record HealthSummary(
            String status,
            String statusDisplayName,
            boolean healthy,
            int totalComponents,
            long healthyComponents,
            long unhealthyComponents,
            Instant lastCheckTime
    ) {}
}
