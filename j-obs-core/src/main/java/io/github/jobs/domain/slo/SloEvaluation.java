package io.github.jobs.domain.slo;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Result of evaluating an SLO at a point in time.
 */
public record SloEvaluation(
        String sloName,
        Slo slo,
        double currentValue,
        SloStatus status,
        ErrorBudget errorBudget,
        List<BurnRate> burnRates,
        long goodEvents,
        long totalEvents,
        Instant evaluatedAt
) {
    public SloEvaluation {
        Objects.requireNonNull(sloName, "SLO name cannot be null");
        Objects.requireNonNull(slo, "SLO cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(errorBudget, "Error budget cannot be null");
        if (burnRates == null) {
            burnRates = List.of();
        }
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }

    /**
     * Creates an evaluation with no data.
     *
     * @param slo the SLO
     * @return evaluation with NO_DATA status
     */
    public static SloEvaluation noData(Slo slo) {
        return new SloEvaluation(
                slo.name(),
                slo,
                Double.NaN,
                SloStatus.NO_DATA,
                ErrorBudget.empty(slo.objective(), slo.window()),
                List.of(),
                0,
                0,
                Instant.now()
        );
    }

    /**
     * Builder for creating evaluations.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the SLO is currently being met.
     *
     * @return true if objective is being met
     */
    public boolean isMet() {
        return currentValue >= slo.objective();
    }

    /**
     * Gets the highest burn rate among all windows.
     *
     * @return the highest burn rate, or 0 if none
     */
    public double getMaxBurnRate() {
        return burnRates.stream()
                .mapToDouble(BurnRate::rate)
                .max()
                .orElse(0.0);
    }

    /**
     * Checks if any burn rate alert should fire.
     *
     * @return true if any burn rate exceeds threshold
     */
    public boolean hasBurnRateAlert() {
        if (burnRates.isEmpty()) {
            return false;
        }

        var alerts = slo.burnRateAlerts();
        if (alerts == null) {
            return false;
        }

        return burnRates.stream().anyMatch(br -> {
            if (br.window().equals(alerts.shortWindow().window())) {
                return br.rate() >= alerts.shortWindow().burnRate();
            }
            if (br.window().equals(alerts.longWindow().window())) {
                return br.rate() >= alerts.longWindow().burnRate();
            }
            return false;
        });
    }

    /**
     * Formats the current value with appropriate precision.
     *
     * @return formatted current value
     */
    public String formatCurrentValue() {
        if (Double.isNaN(currentValue)) {
            return "N/A";
        }
        return String.format(Locale.US, "%.2f%%", currentValue);
    }

    /**
     * Formats the objective with appropriate precision.
     *
     * @return formatted objective
     */
    public String formatObjective() {
        return String.format(Locale.US, "%.2f%%", slo.objective());
    }

    /**
     * Builder for SloEvaluation.
     */
    public static class Builder {
        private String sloName;
        private Slo slo;
        private double currentValue;
        private SloStatus status;
        private ErrorBudget errorBudget;
        private List<BurnRate> burnRates = List.of();
        private long goodEvents;
        private long totalEvents;
        private Instant evaluatedAt;

        public Builder sloName(String sloName) {
            this.sloName = sloName;
            return this;
        }

        public Builder slo(Slo slo) {
            this.slo = slo;
            this.sloName = slo.name();
            return this;
        }

        public Builder currentValue(double currentValue) {
            this.currentValue = currentValue;
            return this;
        }

        public Builder status(SloStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorBudget(ErrorBudget errorBudget) {
            this.errorBudget = errorBudget;
            return this;
        }

        public Builder burnRates(List<BurnRate> burnRates) {
            this.burnRates = burnRates;
            return this;
        }

        public Builder events(long goodEvents, long totalEvents) {
            this.goodEvents = goodEvents;
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder evaluatedAt(Instant evaluatedAt) {
            this.evaluatedAt = evaluatedAt;
            return this;
        }

        public SloEvaluation build() {
            if (status == null && slo != null) {
                status = slo.determineStatus(currentValue);
            }
            if (errorBudget == null && slo != null) {
                errorBudget = slo.calculateErrorBudget(currentValue);
            }
            return new SloEvaluation(
                    sloName, slo, currentValue, status, errorBudget,
                    burnRates, goodEvents, totalEvents, evaluatedAt
            );
        }
    }
}
