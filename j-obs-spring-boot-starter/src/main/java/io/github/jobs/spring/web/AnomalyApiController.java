package io.github.jobs.spring.web;

import io.github.jobs.application.AnomalyDetector;
import io.github.jobs.application.AnomalyDetector.AnomalyStats;
import io.github.jobs.domain.anomaly.Anomaly;
import io.github.jobs.domain.anomaly.AnomalyStatus;
import io.github.jobs.domain.anomaly.AnomalyType;
import io.github.jobs.spring.anomaly.AnomalyDetectionConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for anomaly detection.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/anomalies")
public class AnomalyApiController {

    private final AnomalyDetector anomalyDetector;
    private final AnomalyDetectionConfig config;

    public AnomalyApiController(AnomalyDetector anomalyDetector, AnomalyDetectionConfig config) {
        this.anomalyDetector = anomalyDetector;
        this.config = config;
    }

    /**
     * Run anomaly detection on traces from the specified time window.
     */
    @PostMapping("/detect")
    public ResponseEntity<List<Anomaly>> detect(
            @RequestParam(defaultValue = "5m") String window) {

        Duration duration = parseDuration(window);
        List<Anomaly> anomalies = anomalyDetector.detect(duration);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Get all anomalies, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<List<Anomaly>> getAnomalies(
            @RequestParam(required = false) AnomalyStatus status) {

        List<Anomaly> anomalies = anomalyDetector.getAnomalies(status);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Get anomalies by type.
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Anomaly>> getAnomaliesByType(
            @PathVariable AnomalyType type) {

        List<Anomaly> anomalies = anomalyDetector.getAnomaliesByType(type);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Get a specific anomaly by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Anomaly> getAnomaly(@PathVariable String id) {
        Anomaly anomaly = anomalyDetector.getAnomaly(id);
        if (anomaly == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(anomaly);
    }

    /**
     * Update the status of an anomaly.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Anomaly> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }

        AnomalyStatus status;
        try {
            status = AnomalyStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Anomaly anomaly = anomalyDetector.updateStatus(id, status);
        if (anomaly == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(anomaly);
    }

    /**
     * Acknowledge an anomaly.
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Anomaly> acknowledge(@PathVariable String id) {
        Anomaly anomaly = anomalyDetector.updateStatus(id, AnomalyStatus.ACKNOWLEDGED);
        if (anomaly == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(anomaly);
    }

    /**
     * Resolve an anomaly.
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Anomaly> resolve(@PathVariable String id) {
        Anomaly anomaly = anomalyDetector.updateStatus(id, AnomalyStatus.RESOLVED);
        if (anomaly == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(anomaly);
    }

    /**
     * Ignore an anomaly.
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Anomaly> ignore(@PathVariable String id) {
        Anomaly anomaly = anomalyDetector.updateStatus(id, AnomalyStatus.IGNORED);
        if (anomaly == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(anomaly);
    }

    /**
     * Clear resolved and ignored anomalies.
     */
    @DeleteMapping("/resolved")
    public ResponseEntity<Void> clearResolved() {
        anomalyDetector.clearResolved();
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all anomalies.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        anomalyDetector.clearAll();
        return ResponseEntity.noContent().build();
    }

    /**
     * Get anomaly detection statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<AnomalyStats> getStats() {
        return ResponseEntity.ok(anomalyDetector.getStats());
    }

    /**
     * Get all anomaly types.
     */
    @GetMapping("/types")
    public ResponseEntity<List<AnomalyTypeInfo>> getAnomalyTypes() {
        List<AnomalyTypeInfo> types = Arrays.stream(AnomalyType.values())
                .map(type -> new AnomalyTypeInfo(
                        type.name(),
                        type.displayName(),
                        type.severity(),
                        type.description(),
                        type.cssClass(),
                        type.bgCssClass()
                ))
                .toList();
        return ResponseEntity.ok(types);
    }

    /**
     * Get detection configuration.
     */
    @GetMapping("/config")
    public ResponseEntity<ConfigInfo> getConfig() {
        ConfigInfo info = new ConfigInfo(
                config.isEnabled(),
                config.getDetectionInterval().toSeconds(),
                config.getBaselineWindow().toDays(),
                config.getMinSamplesForBaseline(),
                config.getLatencyZScoreThreshold(),
                config.getErrorRateZScoreThreshold(),
                config.getTrafficZScoreThreshold()
        );
        return ResponseEntity.ok(info);
    }

    private Duration parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return Duration.ofMinutes(5);
        }

        String value = duration.replaceAll("[^0-9]", "");
        String unit = duration.replaceAll("[0-9]", "").toLowerCase();

        int amount;
        try {
            amount = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Duration.ofMinutes(5);
        }

        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> Duration.ofMinutes(5);
        };
    }

    public record AnomalyTypeInfo(
            String name,
            String displayName,
            String severity,
            String description,
            String cssClass,
            String bgCssClass
    ) {}

    public record ConfigInfo(
            boolean enabled,
            long detectionIntervalSeconds,
            long baselineWindowDays,
            int minSamplesForBaseline,
            double latencyZScoreThreshold,
            double errorRateZScoreThreshold,
            double trafficZScoreThreshold
    ) {}
}
