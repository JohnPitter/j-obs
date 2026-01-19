package io.github.jobs.spring.web;

import io.github.jobs.application.SloService;
import io.github.jobs.domain.slo.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for Service Level Objective (SLO) management.
 *
 * <p>Provides endpoints to manage and evaluate SLOs based on Service Level Indicators (SLIs).
 * Tracks error budgets and burn rates for reliability monitoring.</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/slos} - List all SLOs with evaluations</li>
 *   <li>{@code GET /api/slos/{name}} - Get specific SLO</li>
 *   <li>{@code POST /api/slos} - Create or update SLO</li>
 *   <li>{@code DELETE /api/slos/{name}} - Delete SLO</li>
 *   <li>{@code POST /api/slos/evaluate} - Evaluate all SLOs</li>
 *   <li>{@code POST /api/slos/{name}/evaluate} - Evaluate specific SLO</li>
 *   <li>{@code GET /api/slos/{name}/history} - Get evaluation history</li>
 *   <li>{@code GET /api/slos/summary} - Get summary statistics</li>
 * </ul>
 *
 * <h2>SLI Types</h2>
 * <ul>
 *   <li>AVAILABILITY - Ratio of successful requests</li>
 *   <li>LATENCY - Response time percentile</li>
 *   <li>ERROR_RATE - Ratio of failed requests</li>
 *   <li>THROUGHPUT - Request rate threshold</li>
 * </ul>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see io.github.jobs.domain.slo.Slo
 * @see io.github.jobs.domain.slo.ErrorBudget
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/slos")
@ConditionalOnBean(SloService.class)
public class SloApiController {

    private final SloService sloService;

    public SloApiController(SloService sloService) {
        this.sloService = sloService;
    }

    /**
     * Get all configured SLOs with their latest evaluations.
     */
    @GetMapping
    public ResponseEntity<List<SloDto>> getAllSlos() {
        List<SloEvaluation> evaluations = sloService.getAllLatestEvaluations();
        List<SloDto> dtos = evaluations.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific SLO by name.
     */
    @GetMapping("/{name}")
    public ResponseEntity<SloDto> getSlo(@PathVariable String name) {
        return sloService.getLatestEvaluation(name)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> sloService.getSlo(name)
                        .map(slo -> toDto(SloEvaluation.noData(slo)))
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    /**
     * Evaluate all SLOs.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<List<SloDto>> evaluateAll() {
        List<SloEvaluation> evaluations = sloService.evaluateAll();
        List<SloDto> dtos = evaluations.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Evaluate a specific SLO.
     */
    @PostMapping("/{name}/evaluate")
    public ResponseEntity<SloDto> evaluate(@PathVariable String name) {
        try {
            SloEvaluation evaluation = sloService.evaluate(name);
            return ResponseEntity.ok(toDto(evaluation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get evaluation history for an SLO.
     */
    @GetMapping("/{name}/history")
    public ResponseEntity<List<SloDto>> getHistory(
            @PathVariable String name,
            @RequestParam(defaultValue = "24") int limit
    ) {
        List<SloEvaluation> history = sloService.getEvaluationHistory(name, limit);
        if (history.isEmpty() && sloService.getSlo(name).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<SloDto> dtos = history.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get summary statistics for all SLOs.
     */
    @GetMapping("/summary")
    public ResponseEntity<SloService.SloSummary> getSummary() {
        return ResponseEntity.ok(sloService.getSummary());
    }

    /**
     * Create or update an SLO.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createSlo(@RequestBody CreateSloRequest request) {
        try {
            Slo slo = buildSlo(request);
            sloService.register(slo);
            return ResponseEntity.ok(Map.of("status", "created", "name", slo.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an SLO.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> deleteSlo(@PathVariable String name) {
        boolean deleted = sloService.unregister(name);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "deleted", "name", name));
        }
        return ResponseEntity.notFound().build();
    }

    private SloDto toDto(SloEvaluation evaluation) {
        Slo slo = evaluation.slo();
        ErrorBudget budget = evaluation.errorBudget();

        return new SloDto(
                slo.name(),
                slo.description(),
                slo.sli().type().displayName(),
                slo.objective(),
                evaluation.currentValue(),
                evaluation.status().name().toLowerCase(),
                evaluation.status().displayName(),
                new ErrorBudgetDto(
                        budget.totalBudget(),
                        budget.consumedBudget(),
                        budget.remainingBudget(),
                        budget.remainingPercentage(),
                        budget.formatRemainingTime()
                ),
                evaluation.burnRates().stream()
                        .map(br -> new BurnRateDto(
                                br.rate(),
                                br.format(),
                                br.severity(),
                                br.window().toHours() + "h",
                                br.formatProjectedExhaustion()
                        ))
                        .toList(),
                evaluation.goodEvents(),
                evaluation.totalEvents(),
                slo.window().toDays() + "d",
                evaluation.evaluatedAt().toString()
        );
    }

    private Slo buildSlo(CreateSloRequest request) {
        Sli sli = switch (request.sliType().toUpperCase()) {
            case "AVAILABILITY" -> Sli.availability(
                    request.metric(),
                    request.goodCondition(),
                    request.totalCondition()
            );
            case "LATENCY" -> Sli.latency(
                    request.metric(),
                    request.threshold() != null ? request.threshold() : 200.0,
                    request.percentile() != null ? request.percentile() : 99
            );
            case "ERROR_RATE" -> Sli.errorRate(
                    request.metric(),
                    request.goodCondition(),
                    request.totalCondition()
            );
            case "THROUGHPUT" -> Sli.throughput(
                    request.metric(),
                    request.threshold() != null ? request.threshold() : 100.0
            );
            default -> throw new IllegalArgumentException("Unknown SLI type: " + request.sliType());
        };

        Duration window = request.windowDays() != null
                ? Duration.ofDays(request.windowDays())
                : Duration.ofDays(30);

        return Slo.builder()
                .name(request.name())
                .description(request.description())
                .sli(sli)
                .objective(request.objective())
                .window(window)
                .build();
    }

    // DTOs
    public record SloDto(
            String name,
            String description,
            String sliType,
            double objective,
            double current,
            String status,
            String statusDisplay,
            ErrorBudgetDto errorBudget,
            List<BurnRateDto> burnRates,
            long goodEvents,
            long totalEvents,
            String window,
            String evaluatedAt
    ) {}

    public record ErrorBudgetDto(
            double total,
            double consumed,
            double remaining,
            double remainingPercentage,
            String remainingTime
    ) {}

    public record BurnRateDto(
            double rate,
            String formatted,
            String severity,
            String window,
            String projectedExhaustion
    ) {}

    public record CreateSloRequest(
            String name,
            String description,
            String sliType,
            String metric,
            double objective,
            String goodCondition,
            String totalCondition,
            Double threshold,
            Integer percentile,
            Integer windowDays
    ) {}
}
