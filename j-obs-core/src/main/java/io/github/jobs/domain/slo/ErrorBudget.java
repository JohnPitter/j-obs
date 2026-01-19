package io.github.jobs.domain.slo;

import java.time.Duration;
import java.time.Instant;

/**
 * Error Budget tracking for an SLO.
 * The error budget represents the maximum amount of unreliability allowed.
 */
public record ErrorBudget(
        double totalBudget,
        double consumedBudget,
        double remainingBudget,
        double remainingPercentage,
        Duration remainingTime,
        Duration windowDuration,
        Instant calculatedAt
) {
    /**
     * Creates an error budget from objective and current values.
     *
     * @param objective      the SLO objective (e.g., 99.9)
     * @param current        the current SLI value
     * @param windowDuration the SLO window duration
     * @return the error budget
     */
    public static ErrorBudget calculate(double objective, double current, Duration windowDuration) {
        // Total budget is (100 - objective)%
        double totalBudget = 100.0 - objective;

        // Consumed budget is (objective - current)% if current < objective, else 0
        double consumedBudget = current < objective ? (objective - current) : 0.0;

        // Remaining budget
        double remainingBudget = Math.max(0, totalBudget - consumedBudget);

        // Remaining percentage of total budget
        double remainingPercentage = totalBudget > 0 ? (remainingBudget / totalBudget) * 100.0 : 0.0;

        // Calculate remaining time based on remaining percentage
        long remainingMillis = (long) (windowDuration.toMillis() * (remainingPercentage / 100.0));
        Duration remainingTime = Duration.ofMillis(remainingMillis);

        return new ErrorBudget(
                totalBudget,
                consumedBudget,
                remainingBudget,
                remainingPercentage,
                remainingTime,
                windowDuration,
                Instant.now()
        );
    }

    /**
     * Creates an empty error budget when no data is available.
     *
     * @param objective      the SLO objective
     * @param windowDuration the SLO window duration
     * @return empty error budget
     */
    public static ErrorBudget empty(double objective, Duration windowDuration) {
        double totalBudget = 100.0 - objective;
        return new ErrorBudget(
                totalBudget,
                0.0,
                totalBudget,
                100.0,
                windowDuration,
                windowDuration,
                Instant.now()
        );
    }

    /**
     * Checks if the error budget is exhausted.
     *
     * @return true if no budget remaining
     */
    public boolean isExhausted() {
        return remainingBudget <= 0;
    }

    /**
     * Checks if the error budget is at risk (below 25%).
     *
     * @return true if at risk
     */
    public boolean isAtRisk() {
        return remainingPercentage > 0 && remainingPercentage <= 25;
    }

    /**
     * Checks if the error budget is healthy (above 25%).
     *
     * @return true if healthy
     */
    public boolean isHealthy() {
        return remainingPercentage > 25;
    }

    /**
     * Formats the remaining time as a human-readable string.
     *
     * @return formatted remaining time
     */
    public String formatRemainingTime() {
        long hours = remainingTime.toHours();
        long minutes = remainingTime.toMinutesPart();

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
