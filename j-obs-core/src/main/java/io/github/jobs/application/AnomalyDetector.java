package io.github.jobs.application;

import io.github.jobs.domain.anomaly.Anomaly;
import io.github.jobs.domain.anomaly.AnomalyStatus;
import io.github.jobs.domain.anomaly.AnomalyType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Interface for anomaly detection and management.
 */
public interface AnomalyDetector {

    /**
     * Runs anomaly detection on the specified time window.
     *
     * @param window the time window to analyze
     * @return list of detected anomalies
     */
    List<Anomaly> detect(Duration window);

    /**
     * Gets all anomalies, optionally filtered by status.
     *
     * @param status filter by status, or null for all
     * @return list of anomalies
     */
    List<Anomaly> getAnomalies(AnomalyStatus status);

    /**
     * Gets anomalies of a specific type.
     *
     * @param type the anomaly type to filter by
     * @return list of anomalies of the specified type
     */
    List<Anomaly> getAnomaliesByType(AnomalyType type);

    /**
     * Gets a specific anomaly by ID.
     *
     * @param id the anomaly ID
     * @return the anomaly, or null if not found
     */
    Anomaly getAnomaly(String id);

    /**
     * Updates the status of an anomaly.
     *
     * @param id the anomaly ID
     * @param status the new status
     * @return the updated anomaly, or null if not found
     */
    Anomaly updateStatus(String id, AnomalyStatus status);

    /**
     * Clears all resolved and ignored anomalies.
     */
    void clearResolved();

    /**
     * Clears all anomalies.
     */
    void clearAll();

    /**
     * Gets anomaly detection statistics.
     *
     * @return detection statistics
     */
    AnomalyStats getStats();

    /**
     * Statistics about detected anomalies.
     */
    record AnomalyStats(
            long totalAnomalies,
            long activeAnomalies,
            long criticalAnomalies,
            long warningAnomalies,
            long resolvedAnomalies,
            Instant lastDetectionRun,
            Duration detectionDuration
    ) {
        public static AnomalyStats empty() {
            return new AnomalyStats(0, 0, 0, 0, 0, null, Duration.ZERO);
        }
    }

    /**
     * Configuration for anomaly detection thresholds.
     */
    record DetectionConfig(
            double latencyZScoreThreshold,
            double errorRateZScoreThreshold,
            double trafficZScoreThreshold,
            int minSamplesForBaseline,
            Duration baselineWindow,
            boolean alertOnTrafficDecrease
    ) {
        public static DetectionConfig defaults() {
            return new DetectionConfig(
                    3.0,    // 3 standard deviations for latency
                    2.5,    // 2.5 standard deviations for error rate
                    3.0,    // 3 standard deviations for traffic
                    100,    // Minimum 100 samples for baseline
                    Duration.ofDays(7), // 7-day baseline window
                    true    // Alert on traffic decrease
            );
        }
    }

    /**
     * Baseline statistics for a metric.
     */
    record BaselineStats(
            String metric,
            double mean,
            double stdDev,
            double min,
            double max,
            long sampleCount,
            Instant calculatedAt
    ) {
        public double zScore(double value) {
            if (stdDev == 0) return 0;
            return (value - mean) / stdDev;
        }

        public boolean isAnomaly(double value, double threshold) {
            return Math.abs(zScore(value)) > threshold;
        }

        public double percentageChange(double value) {
            if (mean == 0) return value > 0 ? 100 : 0;
            return ((value - mean) / mean) * 100;
        }
    }
}
