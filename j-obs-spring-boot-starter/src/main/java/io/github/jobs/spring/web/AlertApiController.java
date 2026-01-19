package io.github.jobs.spring.web;

import io.github.jobs.application.AlertRepository;
import io.github.jobs.domain.alert.*;
import io.github.jobs.domain.alert.AlertCondition.ComparisonOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for alert rule management.
 *
 * <p>Provides CRUD operations for alert rules including creation, modification,
 * deletion, and toggling of alert states. Also exposes metadata endpoints for
 * alert types, severities, and comparison operators.</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/alerts} - List all alert rules</li>
 *   <li>{@code GET /api/alerts/{id}} - Get specific alert</li>
 *   <li>{@code POST /api/alerts} - Create new alert</li>
 *   <li>{@code PUT /api/alerts/{id}} - Update existing alert</li>
 *   <li>{@code PATCH /api/alerts/{id}/toggle} - Toggle alert enabled state</li>
 *   <li>{@code DELETE /api/alerts/{id}} - Delete alert</li>
 *   <li>{@code GET /api/alerts/types} - List available alert types</li>
 *   <li>{@code GET /api/alerts/severities} - List severity levels</li>
 *   <li>{@code GET /api/alerts/operators} - List comparison operators</li>
 * </ul>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertEventApiController
 * @see AlertProviderApiController
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/alerts")
public class AlertApiController {

    private final AlertRepository alertRepository;

    public AlertApiController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertsResponse getAlerts() {
        List<Alert> alerts = alertRepository.findAll();
        return new AlertsResponse(
                alerts.stream().map(AlertDto::from).toList(),
                alerts.size(),
                alerts.stream().filter(Alert::enabled).count()
        );
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertDto> getAlert(@PathVariable String id) {
        return alertRepository.findById(id)
                .map(AlertDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAlert(@RequestBody CreateAlertRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));
        }

        AlertCondition condition = AlertCondition.builder()
                .metric(request.metric().trim())
                .operator(parseOperator(request.operator()))
                .threshold(request.threshold())
                .window(Duration.ofMinutes(request.windowMinutes()))
                .filters(request.filters() != null ? request.filters() : Map.of())
                .build();

        Alert alert = Alert.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .type(parseAlertType(request.type()))
                .condition(condition)
                .severity(parseSeverity(request.severity()))
                .enabled(request.enabled())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(AlertDto.from(alertRepository.save(alert)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateAlert(@PathVariable String id, @RequestBody CreateAlertRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(new ValidationErrorResponse(errors));
        }

        return alertRepository.findById(id)
                .map(existing -> {
                    AlertCondition condition = AlertCondition.builder()
                            .metric(request.metric().trim())
                            .operator(parseOperator(request.operator()))
                            .threshold(request.threshold())
                            .window(Duration.ofMinutes(request.windowMinutes()))
                            .filters(request.filters() != null ? request.filters() : Map.of())
                            .build();

                    Alert updated = Alert.builder()
                            .id(id)
                            .name(request.name().trim())
                            .description(request.description() != null ? request.description().trim() : null)
                            .type(parseAlertType(request.type()))
                            .condition(condition)
                            .severity(parseSeverity(request.severity()))
                            .enabled(request.enabled())
                            .createdAt(existing.createdAt())
                            .updatedAt(Instant.now())
                            .build();

                    return ResponseEntity.ok((Object) AlertDto.from(alertRepository.save(updated)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private List<String> validateRequest(CreateAlertRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.name() == null || request.name().isBlank()) {
            errors.add("Name is required");
        } else if (request.name().length() > 255) {
            errors.add("Name must be less than 255 characters");
        }

        if (request.metric() == null || request.metric().isBlank()) {
            errors.add("Metric is required");
        }

        if (request.type() == null || request.type().isBlank()) {
            errors.add("Type is required");
        } else if (!isValidEnum(request.type(), AlertType.class)) {
            errors.add("Invalid alert type: " + request.type() + ". Valid values: " +
                    java.util.Arrays.toString(AlertType.values()));
        }

        if (request.operator() == null || request.operator().isBlank()) {
            errors.add("Operator is required");
        } else if (!isValidEnum(request.operator(), ComparisonOperator.class)) {
            errors.add("Invalid operator: " + request.operator() + ". Valid values: " +
                    java.util.Arrays.toString(ComparisonOperator.values()));
        }

        if (request.severity() == null || request.severity().isBlank()) {
            errors.add("Severity is required");
        } else if (!isValidEnum(request.severity(), AlertSeverity.class)) {
            errors.add("Invalid severity: " + request.severity() + ". Valid values: " +
                    java.util.Arrays.toString(AlertSeverity.values()));
        }

        if (request.windowMinutes() <= 0) {
            errors.add("Window minutes must be positive");
        } else if (request.windowMinutes() > 1440) {
            errors.add("Window minutes cannot exceed 1440 (24 hours)");
        }

        if (Double.isNaN(request.threshold()) || Double.isInfinite(request.threshold())) {
            errors.add("Threshold must be a valid finite number");
        }

        return errors;
    }

    private <E extends Enum<E>> boolean isValidEnum(String value, Class<E> enumClass) {
        try {
            Enum.valueOf(enumClass, value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private AlertType parseAlertType(String type) {
        return AlertType.valueOf(type.toUpperCase());
    }

    private ComparisonOperator parseOperator(String operator) {
        return ComparisonOperator.valueOf(operator.toUpperCase());
    }

    private AlertSeverity parseSeverity(String severity) {
        return AlertSeverity.valueOf(severity.toUpperCase());
    }

    @PatchMapping(value = "/{id}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertDto> toggleAlert(@PathVariable String id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    Alert toggled = alert.withEnabled(!alert.enabled());
                    return ResponseEntity.ok(AlertDto.from(alertRepository.save(toggled)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable String id) {
        if (alertRepository.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertTypeDto> getAlertTypes() {
        return java.util.Arrays.stream(AlertType.values())
                .map(t -> new AlertTypeDto(t.name(), t.displayName(), t.description(), t.icon()))
                .toList();
    }

    @GetMapping(value = "/severities", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertSeverityDto> getSeverities() {
        return java.util.Arrays.stream(AlertSeverity.values())
                .map(s -> new AlertSeverityDto(s.name(), s.displayName(), s.emoji(), s.textCssClass(), s.bgCssClass()))
                .toList();
    }

    @GetMapping(value = "/operators", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OperatorDto> getOperators() {
        return java.util.Arrays.stream(ComparisonOperator.values())
                .map(o -> new OperatorDto(o.name(), o.symbol(), o.description()))
                .toList();
    }

    // DTOs

    public record AlertsResponse(List<AlertDto> alerts, long total, long enabledCount) {}

    public record AlertDto(
            String id,
            String name,
            String description,
            String type,
            String typeDisplayName,
            String typeIcon,
            ConditionDto condition,
            String severity,
            String severityDisplayName,
            String severityEmoji,
            String severityTextCss,
            String severityBgCss,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static AlertDto from(Alert alert) {
            return new AlertDto(
                    alert.id(),
                    alert.name(),
                    alert.description(),
                    alert.type().name(),
                    alert.type().displayName(),
                    alert.type().icon(),
                    ConditionDto.from(alert.condition()),
                    alert.severity().name(),
                    alert.severity().displayName(),
                    alert.severity().emoji(),
                    alert.severity().textCssClass(),
                    alert.severity().bgCssClass(),
                    alert.enabled(),
                    alert.createdAt(),
                    alert.updatedAt()
            );
        }
    }

    public record ConditionDto(
            String metric,
            String operator,
            String operatorSymbol,
            double threshold,
            long windowMinutes,
            Map<String, String> filters,
            String description
    ) {
        public static ConditionDto from(AlertCondition condition) {
            return new ConditionDto(
                    condition.metric(),
                    condition.operator().name(),
                    condition.operator().symbol(),
                    condition.threshold(),
                    condition.window().toMinutes(),
                    condition.filters(),
                    condition.describe()
            );
        }
    }

    public record CreateAlertRequest(
            String name,
            String description,
            String type,
            String metric,
            String operator,
            double threshold,
            int windowMinutes,
            Map<String, String> filters,
            String severity,
            boolean enabled
    ) {}

    public record AlertTypeDto(String name, String displayName, String description, String icon) {}
    public record AlertSeverityDto(String name, String displayName, String emoji, String textCss, String bgCss) {}
    public record OperatorDto(String name, String symbol, String description) {}
    public record ValidationErrorResponse(List<String> errors) {}
}
