package io.github.jobs.domain.alert;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Result of evaluating an alert condition.
 */
public record AlertEvaluationResult(
        String alertId,
        boolean triggered,
        String message,
        Map<String, String> labels,
        Object currentValue,
        Object threshold,
        Instant evaluatedAt
) {
    public AlertEvaluationResult {
        Objects.requireNonNull(alertId, "alertId cannot be null");
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
        if (labels == null) {
            labels = Map.of();
        }
    }

    public static AlertEvaluationResult triggered(String alertId, String message, Object currentValue, Object threshold) {
        return new AlertEvaluationResult(alertId, true, message, Map.of(), currentValue, threshold, Instant.now());
    }

    public static AlertEvaluationResult triggered(String alertId, String message, Object currentValue, Object threshold, Map<String, String> labels) {
        return new AlertEvaluationResult(alertId, true, message, labels, currentValue, threshold, Instant.now());
    }

    public static AlertEvaluationResult notTriggered(String alertId) {
        return new AlertEvaluationResult(alertId, false, null, Map.of(), null, null, Instant.now());
    }

    public static AlertEvaluationResult notTriggered(String alertId, Object currentValue, Object threshold) {
        return new AlertEvaluationResult(alertId, false, null, Map.of(), currentValue, threshold, Instant.now());
    }
}
