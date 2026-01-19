package io.github.jobs.domain.slo;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Burn Rate calculation for an SLO.
 * Burn rate indicates how fast the error budget is being consumed.
 * A burn rate of 1.0 means the budget is consumed exactly at the expected rate.
 * A burn rate of 2.0 means the budget is consumed twice as fast.
 */
public record BurnRate(
        double rate,
        Duration window,
        Duration projectedExhaustion,
        Instant calculatedAt
) {
    /**
     * Safe burn rate threshold (budget consumed at expected rate).
     */
    public static final double SAFE_THRESHOLD = 1.0;

    /**
     * Elevated burn rate threshold (budget consumed faster than expected).
     */
    public static final double ELEVATED_THRESHOLD = 2.0;

    /**
     * Critical burn rate threshold (budget consumed much faster).
     */
    public static final double CRITICAL_THRESHOLD = 10.0;

    /**
     * Calculates burn rate from error budget consumption.
     *
     * @param consumedBudget       budget consumed so far
     * @param totalBudget          total available budget
     * @param elapsedTime          time elapsed in the SLO window
     * @param totalWindowDuration  total SLO window duration
     * @param window               the window for this burn rate calculation
     * @return the burn rate
     */
    public static BurnRate calculate(
            double consumedBudget,
            double totalBudget,
            Duration elapsedTime,
            Duration totalWindowDuration,
            Duration window
    ) {
        if (totalBudget <= 0 || elapsedTime.isZero() || totalWindowDuration.isZero()) {
            return new BurnRate(0.0, window, Duration.ZERO, Instant.now());
        }

        // Expected consumption rate = totalBudget / totalWindow
        double expectedRate = totalBudget / totalWindowDuration.toMillis();

        // Actual consumption rate = consumedBudget / elapsedTime
        double actualRate = consumedBudget / elapsedTime.toMillis();

        // Burn rate = actual / expected
        double burnRate = expectedRate > 0 ? actualRate / expectedRate : 0.0;

        // Calculate projected exhaustion time
        Duration projectedExhaustion;
        if (actualRate > 0) {
            double remainingBudget = totalBudget - consumedBudget;
            long exhaustionMillis = (long) (remainingBudget / actualRate);
            projectedExhaustion = Duration.ofMillis(Math.max(0, exhaustionMillis));
        } else {
            projectedExhaustion = totalWindowDuration;
        }

        return new BurnRate(burnRate, window, projectedExhaustion, Instant.now());
    }

    /**
     * Creates a zero burn rate (no consumption).
     *
     * @param window the calculation window
     * @return zero burn rate
     */
    public static BurnRate zero(Duration window) {
        return new BurnRate(0.0, window, Duration.ZERO, Instant.now());
    }

    /**
     * Checks if the burn rate is safe (below 1.0).
     *
     * @return true if safe
     */
    public boolean isSafe() {
        return rate < SAFE_THRESHOLD;
    }

    /**
     * Checks if the burn rate is elevated (between 1.0 and 2.0).
     *
     * @return true if elevated
     */
    public boolean isElevated() {
        return rate >= SAFE_THRESHOLD && rate < ELEVATED_THRESHOLD;
    }

    /**
     * Checks if the burn rate is high (between 2.0 and 10.0).
     *
     * @return true if high
     */
    public boolean isHigh() {
        return rate >= ELEVATED_THRESHOLD && rate < CRITICAL_THRESHOLD;
    }

    /**
     * Checks if the burn rate is critical (above 10.0).
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return rate >= CRITICAL_THRESHOLD;
    }

    /**
     * Returns the severity level based on burn rate.
     *
     * @return severity string
     */
    public String severity() {
        if (isCritical()) return "critical";
        if (isHigh()) return "high";
        if (isElevated()) return "elevated";
        return "safe";
    }

    /**
     * Formats the burn rate as a string.
     *
     * @return formatted burn rate
     */
    public String format() {
        return String.format(Locale.US, "%.1fx", rate);
    }

    /**
     * Formats the projected exhaustion time.
     *
     * @return formatted exhaustion time
     */
    public String formatProjectedExhaustion() {
        if (projectedExhaustion.isZero()) {
            return "N/A";
        }

        long hours = projectedExhaustion.toHours();
        long minutes = projectedExhaustion.toMinutesPart();

        if (hours > 24) {
            long days = hours / 24;
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
}
