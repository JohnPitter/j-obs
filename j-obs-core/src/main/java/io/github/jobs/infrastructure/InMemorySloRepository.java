package io.github.jobs.infrastructure;

import io.github.jobs.application.SloRepository;
import io.github.jobs.domain.slo.Slo;
import io.github.jobs.domain.slo.SloEvaluation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory implementation of SloRepository.
 */
public class InMemorySloRepository implements SloRepository {

    private final Map<String, Slo> slos = new ConcurrentHashMap<>();
    private final Map<String, Deque<SloEvaluation>> evaluationHistory = new ConcurrentHashMap<>();
    private final int maxHistoryPerSlo;

    public InMemorySloRepository() {
        this(100);
    }

    public InMemorySloRepository(int maxHistoryPerSlo) {
        this.maxHistoryPerSlo = maxHistoryPerSlo;
    }

    @Override
    public void save(Slo slo) {
        slos.put(slo.name(), slo);
    }

    @Override
    public Optional<Slo> findByName(String name) {
        return Optional.ofNullable(slos.get(name));
    }

    @Override
    public List<Slo> findAll() {
        return new ArrayList<>(slos.values());
    }

    @Override
    public boolean delete(String name) {
        evaluationHistory.remove(name);
        return slos.remove(name) != null;
    }

    @Override
    public int count() {
        return slos.size();
    }

    @Override
    public void saveEvaluation(SloEvaluation evaluation) {
        Deque<SloEvaluation> history = evaluationHistory.computeIfAbsent(
                evaluation.sloName(),
                k -> new ConcurrentLinkedDeque<>()
        );

        history.addFirst(evaluation);

        // Trim history if needed
        while (history.size() > maxHistoryPerSlo) {
            history.removeLast();
        }
    }

    @Override
    public Optional<SloEvaluation> getLatestEvaluation(String sloName) {
        Deque<SloEvaluation> history = evaluationHistory.get(sloName);
        if (history == null || history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(history.peekFirst());
    }

    @Override
    public List<SloEvaluation> getAllLatestEvaluations() {
        List<SloEvaluation> results = new ArrayList<>();
        for (String sloName : slos.keySet()) {
            getLatestEvaluation(sloName).ifPresent(results::add);
        }
        return results;
    }

    @Override
    public List<SloEvaluation> getEvaluationHistory(String sloName, int limit) {
        Deque<SloEvaluation> history = evaluationHistory.get(sloName);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        List<SloEvaluation> results = new ArrayList<>();
        int count = 0;
        for (SloEvaluation eval : history) {
            if (count >= limit) break;
            results.add(eval);
            count++;
        }
        return results;
    }

    @Override
    public void clearEvaluations() {
        evaluationHistory.clear();
    }
}
