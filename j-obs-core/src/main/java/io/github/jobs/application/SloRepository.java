package io.github.jobs.application;

import io.github.jobs.domain.slo.Slo;
import io.github.jobs.domain.slo.SloEvaluation;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing SLO definitions and evaluations.
 */
public interface SloRepository {

    /**
     * Saves or updates an SLO definition.
     *
     * @param slo the SLO to save
     */
    void save(Slo slo);

    /**
     * Finds an SLO by name.
     *
     * @param name the SLO name
     * @return the SLO if found
     */
    Optional<Slo> findByName(String name);

    /**
     * Returns all configured SLOs.
     *
     * @return list of all SLOs
     */
    List<Slo> findAll();

    /**
     * Deletes an SLO by name.
     *
     * @param name the SLO name
     * @return true if deleted
     */
    boolean delete(String name);

    /**
     * Returns the count of configured SLOs.
     *
     * @return SLO count
     */
    int count();

    /**
     * Saves an SLO evaluation result.
     *
     * @param evaluation the evaluation to save
     */
    void saveEvaluation(SloEvaluation evaluation);

    /**
     * Gets the latest evaluation for an SLO.
     *
     * @param sloName the SLO name
     * @return the latest evaluation if available
     */
    Optional<SloEvaluation> getLatestEvaluation(String sloName);

    /**
     * Gets all latest evaluations.
     *
     * @return list of latest evaluations for all SLOs
     */
    List<SloEvaluation> getAllLatestEvaluations();

    /**
     * Gets evaluation history for an SLO.
     *
     * @param sloName the SLO name
     * @param limit   maximum number of evaluations to return
     * @return list of evaluations, most recent first
     */
    List<SloEvaluation> getEvaluationHistory(String sloName, int limit);

    /**
     * Clears all evaluation history.
     */
    void clearEvaluations();
}
