package io.github.jobs.application;

import io.github.jobs.domain.slo.Slo;
import io.github.jobs.domain.slo.SloEvaluation;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing and evaluating SLOs.
 */
public interface SloService {

    /**
     * Registers a new SLO.
     *
     * @param slo the SLO to register
     */
    void register(Slo slo);

    /**
     * Unregisters an SLO by name.
     *
     * @param name the SLO name
     * @return true if unregistered
     */
    boolean unregister(String name);

    /**
     * Gets an SLO by name.
     *
     * @param name the SLO name
     * @return the SLO if found
     */
    Optional<Slo> getSlo(String name);

    /**
     * Returns all configured SLOs.
     *
     * @return list of all SLOs
     */
    List<Slo> getAllSlos();

    /**
     * Evaluates a specific SLO.
     *
     * @param name the SLO name
     * @return the evaluation result
     */
    SloEvaluation evaluate(String name);

    /**
     * Evaluates all configured SLOs.
     *
     * @return list of all evaluations
     */
    List<SloEvaluation> evaluateAll();

    /**
     * Gets the latest evaluation for an SLO.
     *
     * @param name the SLO name
     * @return the latest evaluation if available
     */
    Optional<SloEvaluation> getLatestEvaluation(String name);

    /**
     * Gets all latest evaluations.
     *
     * @return list of latest evaluations
     */
    List<SloEvaluation> getAllLatestEvaluations();

    /**
     * Gets evaluation history for an SLO.
     *
     * @param name  the SLO name
     * @param limit maximum number of evaluations
     * @return list of evaluations, most recent first
     */
    List<SloEvaluation> getEvaluationHistory(String name, int limit);

    /**
     * Gets summary statistics for all SLOs.
     *
     * @return the summary statistics
     */
    SloSummary getSummary();

    /**
     * Summary statistics for SLOs.
     */
    record SloSummary(
            int totalSlos,
            int healthySlos,
            int atRiskSlos,
            int breachedSlos,
            int noDataSlos,
            double overallHealthPercentage
    ) {
        /**
         * Creates an empty summary.
         */
        public static SloSummary empty() {
            return new SloSummary(0, 0, 0, 0, 0, 100.0);
        }

        /**
         * Creates a summary from evaluations.
         *
         * @param evaluations list of evaluations
         * @return the summary
         */
        public static SloSummary from(List<SloEvaluation> evaluations) {
            int total = evaluations.size();
            if (total == 0) {
                return empty();
            }

            int healthy = 0;
            int atRisk = 0;
            int breached = 0;
            int noData = 0;

            for (SloEvaluation eval : evaluations) {
                switch (eval.status()) {
                    case HEALTHY -> healthy++;
                    case AT_RISK -> atRisk++;
                    case BREACHED -> breached++;
                    case NO_DATA -> noData++;
                }
            }

            double healthPercentage = total > 0 ? (healthy * 100.0 / total) : 100.0;

            return new SloSummary(total, healthy, atRisk, breached, noData, healthPercentage);
        }
    }
}
