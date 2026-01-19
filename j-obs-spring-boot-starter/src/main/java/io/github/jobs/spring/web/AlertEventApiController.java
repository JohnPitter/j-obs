package io.github.jobs.spring.web;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.AlertService;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertEventStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for alert event management.
 *
 * <p>Provides endpoints to query, acknowledge, and resolve alert events.
 * Alert events are instances of fired alerts that can be tracked through
 * their lifecycle (FIRING → ACKNOWLEDGED → RESOLVED).</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/alert-events} - List events with optional filters</li>
 *   <li>{@code GET /api/alert-events/active} - List currently active events</li>
 *   <li>{@code GET /api/alert-events/{id}} - Get specific event</li>
 *   <li>{@code POST /api/alert-events/{id}/acknowledge} - Acknowledge an event</li>
 *   <li>{@code POST /api/alert-events/{id}/resolve} - Resolve an event</li>
 *   <li>{@code GET /api/alert-events/statistics} - Get alert statistics</li>
 *   <li>{@code GET /api/alert-events/statuses} - List status types</li>
 * </ul>
 *
 * <h2>Event Lifecycle</h2>
 * <pre>
 * FIRING → ACKNOWLEDGED → RESOLVED
 *    └──────────────────────┘
 * </pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertApiController
 * @see AlertProviderApiController
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/alert-events")
public class AlertEventApiController {

    private final AlertEventRepository alertEventRepository;
    private final AlertService alertService;

    public AlertEventApiController(AlertEventRepository alertEventRepository, AlertService alertService) {
        this.alertEventRepository = alertEventRepository;
        this.alertService = alertService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertEventsResponse getEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<AlertEvent> events;

        if (status != null && !status.isEmpty()) {
            AlertEventStatus eventStatus = AlertEventStatus.valueOf(status.toUpperCase());
            events = alertEventRepository.findByStatus(eventStatus);
        } else if (alertId != null && !alertId.isEmpty()) {
            events = alertEventRepository.findByAlertId(alertId);
        } else {
            events = alertEventRepository.findAll();
        }

        // Apply limit
        if (events.size() > limit) {
            events = events.subList(0, limit);
        }

        List<AlertEventDto> dtos = events.stream().map(AlertEventDto::from).toList();

        return new AlertEventsResponse(
                dtos,
                alertEventRepository.count(),
                alertEventRepository.countActive()
        );
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertEventDto> getActiveEvents() {
        return alertEventRepository.findActive().stream()
                .map(AlertEventDto::from)
                .toList();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertEventDto> getEvent(@PathVariable String id) {
        return alertEventRepository.findById(id)
                .map(AlertEventDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/acknowledge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertEventDto> acknowledgeEvent(
            @PathVariable String id,
            @RequestBody(required = false) AcknowledgeRequest request
    ) {
        try {
            String acknowledgedBy = request != null && request.acknowledgedBy() != null
                    ? request.acknowledgedBy()
                    : "user";
            AlertEvent acknowledged = alertService.acknowledgeEvent(id, acknowledgedBy);
            return ResponseEntity.ok(AlertEventDto.from(acknowledged));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{id}/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertEventDto> resolveEvent(
            @PathVariable String id,
            @RequestBody(required = false) ResolveRequest request
    ) {
        try {
            String resolvedBy = request != null && request.resolvedBy() != null
                    ? request.resolvedBy()
                    : "user";
            AlertEvent resolved = alertService.resolveEvent(id, resolvedBy);
            return ResponseEntity.ok(AlertEventDto.from(resolved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertService.AlertStatistics getStatistics() {
        return alertService.getStatistics();
    }

    @GetMapping(value = "/statuses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertEventStatusDto> getStatuses() {
        return java.util.Arrays.stream(AlertEventStatus.values())
                .map(s -> new AlertEventStatusDto(s.name(), s.displayName(), s.textCssClass(), s.bgCssClass()))
                .toList();
    }

    // DTOs

    public record AlertEventsResponse(
            List<AlertEventDto> events,
            long total,
            long active
    ) {}

    public record AlertEventDto(
            String id,
            String alertId,
            String alertName,
            String severity,
            String severityDisplayName,
            String severityEmoji,
            String status,
            String statusDisplayName,
            String statusTextCss,
            String statusBgCss,
            String message,
            Map<String, String> labels,
            Instant firedAt,
            Instant acknowledgedAt,
            String acknowledgedBy,
            Instant resolvedAt,
            String resolvedBy
    ) {
        public static AlertEventDto from(AlertEvent event) {
            return new AlertEventDto(
                    event.id(),
                    event.alertId(),
                    event.alertName(),
                    event.severity().name(),
                    event.severity().displayName(),
                    event.severity().emoji(),
                    event.status().name(),
                    event.status().displayName(),
                    event.status().textCssClass(),
                    event.status().bgCssClass(),
                    event.message(),
                    event.labels(),
                    event.firedAt(),
                    event.acknowledgedAt(),
                    event.acknowledgedBy(),
                    event.resolvedAt(),
                    event.resolvedBy()
            );
        }
    }

    public record AlertEventStatusDto(String name, String displayName, String textCss, String bgCss) {}

    public record AcknowledgeRequest(String acknowledgedBy) {}

    public record ResolveRequest(String resolvedBy) {}
}
