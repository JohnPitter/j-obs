package io.github.jobs.domain.alert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable key used to group similar alerts together.
 *
 * <p>Alerts with matching keys are collected into a single {@link AlertGroup}
 * for batch notification. The key consists of:</p>
 * <ul>
 *   <li>Alert name</li>
 *   <li>Alert severity</li>
 *   <li>Selected label values (configurable)</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <p>With {@code groupByLabels = ["service", "instance"]}:</p>
 * <pre>{@code
 * // These alerts will be grouped together:
 * AlertEvent event1 = AlertEvent.builder()
 *     .alertName("high-cpu")
 *     .severity(WARNING)
 *     .labels(Map.of("service", "api", "instance", "pod-1"))
 *     .build();
 *
 * AlertEvent event2 = AlertEvent.builder()
 *     .alertName("high-cpu")
 *     .severity(WARNING)
 *     .labels(Map.of("service", "api", "instance", "pod-1", "extra", "ignored"))
 *     .build();
 *
 * // This alert will be in a separate group (different service):
 * AlertEvent event3 = AlertEvent.builder()
 *     .alertName("high-cpu")
 *     .severity(WARNING)
 *     .labels(Map.of("service", "web", "instance", "pod-1"))
 *     .build();
 * }</pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertGroup
 */
public record AlertGroupKey(
        String alertName,
        AlertSeverity severity,
        Map<String, String> groupLabels
) {
    public AlertGroupKey {
        Objects.requireNonNull(alertName, "alertName cannot be null");
        Objects.requireNonNull(severity, "severity cannot be null");
        if (groupLabels == null) {
            groupLabels = Map.of();
        }
        // Use TreeMap for consistent ordering in equals/hashCode
        groupLabels = Collections.unmodifiableMap(new TreeMap<>(groupLabels));
    }

    /**
     * Creates a group key from an alert event.
     *
     * @param event the alert event
     * @param groupByLabels list of label keys to include in grouping
     * @return a new AlertGroupKey
     */
    public static AlertGroupKey fromEvent(AlertEvent event, List<String> groupByLabels) {
        Map<String, String> labels = new TreeMap<>();
        if (groupByLabels != null && event.labels() != null) {
            for (String labelKey : groupByLabels) {
                String value = event.labels().get(labelKey);
                if (value != null) {
                    labels.put(labelKey, value);
                }
            }
        }
        return new AlertGroupKey(event.alertName(), event.severity(), labels);
    }

    /**
     * Creates a simple key using only alert name and severity.
     */
    public static AlertGroupKey simple(String alertName, AlertSeverity severity) {
        return new AlertGroupKey(alertName, severity, Map.of());
    }

    /**
     * Returns a human-readable representation of this key.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(alertName).append(" [").append(severity).append("]");
        if (!groupLabels.isEmpty()) {
            sb.append(" {");
            boolean first = true;
            for (Map.Entry<String, String> entry : groupLabels.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Checks if this key matches an event.
     */
    public boolean matches(AlertEvent event, List<String> groupByLabels) {
        AlertGroupKey eventKey = fromEvent(event, groupByLabels);
        return this.equals(eventKey);
    }
}
