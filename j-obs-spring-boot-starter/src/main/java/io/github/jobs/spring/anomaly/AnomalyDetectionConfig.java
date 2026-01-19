package io.github.jobs.spring.anomaly;

import java.time.Duration;

/**
 * Configuration for anomaly detection.
 */
public class AnomalyDetectionConfig {

    /**
     * Whether anomaly detection is enabled.
     */
    private boolean enabled = true;

    /**
     * How often to run anomaly detection.
     */
    private Duration detectionInterval = Duration.ofMinutes(1);

    /**
     * Window for baseline calculation.
     */
    private Duration baselineWindow = Duration.ofDays(7);

    /**
     * Minimum samples required for baseline calculation.
     */
    private int minSamplesForBaseline = 100;

    /**
     * Z-score threshold for latency anomalies.
     */
    private double latencyZScoreThreshold = 3.0;

    /**
     * Minimum percentage increase to be considered a latency anomaly.
     */
    private double latencyMinIncreasePercent = 100.0;

    /**
     * Z-score threshold for error rate anomalies.
     */
    private double errorRateZScoreThreshold = 2.5;

    /**
     * Minimum absolute error rate to trigger anomaly.
     */
    private double errorRateMinAbsolute = 1.0;

    /**
     * Z-score threshold for traffic anomalies.
     */
    private double trafficZScoreThreshold = 3.0;

    /**
     * Whether to alert on traffic decrease.
     */
    private boolean alertOnTrafficDecrease = true;

    /**
     * How long to retain resolved anomalies.
     */
    private Duration retentionPeriod = Duration.ofDays(7);

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDetectionInterval() {
        return detectionInterval;
    }

    public void setDetectionInterval(Duration detectionInterval) {
        this.detectionInterval = detectionInterval;
    }

    public Duration getBaselineWindow() {
        return baselineWindow;
    }

    public void setBaselineWindow(Duration baselineWindow) {
        this.baselineWindow = baselineWindow;
    }

    public int getMinSamplesForBaseline() {
        return minSamplesForBaseline;
    }

    public void setMinSamplesForBaseline(int minSamplesForBaseline) {
        this.minSamplesForBaseline = minSamplesForBaseline;
    }

    public double getLatencyZScoreThreshold() {
        return latencyZScoreThreshold;
    }

    public void setLatencyZScoreThreshold(double latencyZScoreThreshold) {
        this.latencyZScoreThreshold = latencyZScoreThreshold;
    }

    public double getLatencyMinIncreasePercent() {
        return latencyMinIncreasePercent;
    }

    public void setLatencyMinIncreasePercent(double latencyMinIncreasePercent) {
        this.latencyMinIncreasePercent = latencyMinIncreasePercent;
    }

    public double getErrorRateZScoreThreshold() {
        return errorRateZScoreThreshold;
    }

    public void setErrorRateZScoreThreshold(double errorRateZScoreThreshold) {
        this.errorRateZScoreThreshold = errorRateZScoreThreshold;
    }

    public double getErrorRateMinAbsolute() {
        return errorRateMinAbsolute;
    }

    public void setErrorRateMinAbsolute(double errorRateMinAbsolute) {
        this.errorRateMinAbsolute = errorRateMinAbsolute;
    }

    public double getTrafficZScoreThreshold() {
        return trafficZScoreThreshold;
    }

    public void setTrafficZScoreThreshold(double trafficZScoreThreshold) {
        this.trafficZScoreThreshold = trafficZScoreThreshold;
    }

    public boolean isAlertOnTrafficDecrease() {
        return alertOnTrafficDecrease;
    }

    public void setAlertOnTrafficDecrease(boolean alertOnTrafficDecrease) {
        this.alertOnTrafficDecrease = alertOnTrafficDecrease;
    }

    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(Duration retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }
}
