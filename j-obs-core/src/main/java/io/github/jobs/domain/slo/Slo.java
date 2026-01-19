package io.github.jobs.domain.slo;

import java.time.Duration;
import java.util.Objects;

/**
 * Service Level Objective (SLO) definition.
 * An SLO is a target value or range for a service level measured by an SLI.
 */
public record Slo(
        String name,
        String description,
        Sli sli,
        double objective,
        Duration window,
        BurnRateAlerts burnRateAlerts
) {
    public Slo {
        Objects.requireNonNull(name, "SLO name cannot be null");
        Objects.requireNonNull(sli, "SLI cannot be null");
        if (objective <= 0 || objective > 100) {
            throw new IllegalArgumentException("Objective must be between 0 and 100");
        }
        if (window == null) {
            window = Duration.ofDays(30);
        }
        if (burnRateAlerts == null) {
            burnRateAlerts = BurnRateAlerts.defaults();
        }
    }

    /**
     * Creates an availability SLO.
     *
     * @param name        SLO name
     * @param description SLO description
     * @param metric      metric name
     * @param objective   target availability percentage (e.g., 99.9)
     * @param window      SLO window duration
     * @return the SLO
     */
    public static Slo availability(String name, String description, String metric, double objective, Duration window) {
        Sli sli = Sli.availability(metric, "status < 500", "status >= 0");
        return new Slo(name, description, sli, objective, window, null);
    }

    /**
     * Creates a latency SLO.
     *
     * @param name        SLO name
     * @param description SLO description
     * @param metric      metric name
     * @param threshold   latency threshold in ms
     * @param percentile  percentile (e.g., 99)
     * @param objective   target percentage meeting threshold
     * @param window      SLO window duration
     * @return the SLO
     */
    public static Slo latency(String name, String description, String metric,
                              double threshold, int percentile, double objective, Duration window) {
        Sli sli = Sli.latency(metric, threshold, percentile);
        return new Slo(name, description, sli, objective, window, null);
    }

    /**
     * Calculates the error budget for this SLO.
     *
     * @param currentValue current SLI value
     * @return the error budget
     */
    public ErrorBudget calculateErrorBudget(double currentValue) {
        return ErrorBudget.calculate(objective, currentValue, window);
    }

    /**
     * Determines the status based on current SLI value.
     *
     * @param currentValue current SLI value
     * @return the SLO status
     */
    public SloStatus determineStatus(double currentValue) {
        if (Double.isNaN(currentValue)) {
            return SloStatus.NO_DATA;
        }

        ErrorBudget budget = calculateErrorBudget(currentValue);

        if (budget.isExhausted()) {
            return SloStatus.BREACHED;
        } else if (budget.isAtRisk()) {
            return SloStatus.AT_RISK;
        } else {
            return SloStatus.HEALTHY;
        }
    }

    /**
     * Builder for creating SLOs with custom configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Burn rate alert configuration.
     */
    public record BurnRateAlerts(
            BurnRateAlert shortWindow,
            BurnRateAlert longWindow
    ) {
        /**
         * Default burn rate alerts based on Google SRE recommendations.
         */
        public static BurnRateAlerts defaults() {
            return new BurnRateAlerts(
                    new BurnRateAlert(14.4, Duration.ofHours(1), Duration.ofMinutes(5)),
                    new BurnRateAlert(6.0, Duration.ofHours(6), Duration.ofMinutes(30))
            );
        }
    }

    /**
     * Single burn rate alert configuration.
     */
    public record BurnRateAlert(
            double burnRate,
            Duration window,
            Duration forDuration
    ) {}

    /**
     * Builder for SLO.
     */
    public static class Builder {
        private String name;
        private String description;
        private Sli sli;
        private double objective;
        private Duration window = Duration.ofDays(30);
        private BurnRateAlerts burnRateAlerts;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sli(Sli sli) {
            this.sli = sli;
            return this;
        }

        public Builder objective(double objective) {
            this.objective = objective;
            return this;
        }

        public Builder window(Duration window) {
            this.window = window;
            return this;
        }

        public Builder burnRateAlerts(BurnRateAlerts burnRateAlerts) {
            this.burnRateAlerts = burnRateAlerts;
            return this;
        }

        public Slo build() {
            return new Slo(name, description, sli, objective, window, burnRateAlerts);
        }
    }
}
