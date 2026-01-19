package io.github.jobs.infrastructure;

import io.github.jobs.domain.slo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySloRepositoryTest {

    private InMemorySloRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySloRepository(10);
    }

    @Test
    void shouldSaveAndFindSlo() {
        Slo slo = Slo.availability("test-slo", "Test SLO", "m", 99.9, Duration.ofDays(30));

        repository.save(slo);

        Optional<Slo> found = repository.findByName("test-slo");
        assertTrue(found.isPresent());
        assertEquals("test-slo", found.get().name());
    }

    @Test
    void shouldReturnEmptyForUnknownSlo() {
        Optional<Slo> found = repository.findByName("unknown");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindAllSlos() {
        repository.save(Slo.availability("slo1", "desc", "m", 99.9, Duration.ofDays(30)));
        repository.save(Slo.availability("slo2", "desc", "m", 99.5, Duration.ofDays(7)));

        List<Slo> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void shouldDeleteSlo() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);

        assertTrue(repository.delete("test-slo"));
        assertTrue(repository.findByName("test-slo").isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistent() {
        assertFalse(repository.delete("unknown"));
    }

    @Test
    void shouldCountSlos() {
        assertEquals(0, repository.count());

        repository.save(Slo.availability("slo1", "desc", "m", 99.9, Duration.ofDays(30)));
        assertEquals(1, repository.count());

        repository.save(Slo.availability("slo2", "desc", "m", 99.5, Duration.ofDays(7)));
        assertEquals(2, repository.count());
    }

    @Test
    void shouldSaveAndRetrieveEvaluation() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.95)
                .build();

        repository.saveEvaluation(evaluation);

        Optional<SloEvaluation> latest = repository.getLatestEvaluation("test-slo");
        assertTrue(latest.isPresent());
        assertEquals(99.95, latest.get().currentValue());
    }

    @Test
    void shouldReturnLatestEvaluation() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);

        // Save multiple evaluations
        repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.0).build());
        repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.5).build());
        repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.9).build());

        Optional<SloEvaluation> latest = repository.getLatestEvaluation("test-slo");
        assertTrue(latest.isPresent());
        assertEquals(99.9, latest.get().currentValue());
    }

    @Test
    void shouldGetAllLatestEvaluations() {
        Slo slo1 = Slo.availability("slo1", "desc", "m", 99.9, Duration.ofDays(30));
        Slo slo2 = Slo.availability("slo2", "desc", "m", 99.5, Duration.ofDays(7));
        repository.save(slo1);
        repository.save(slo2);

        repository.saveEvaluation(SloEvaluation.builder().slo(slo1).currentValue(99.95).build());
        repository.saveEvaluation(SloEvaluation.builder().slo(slo2).currentValue(99.6).build());

        List<SloEvaluation> all = repository.getAllLatestEvaluations();
        assertEquals(2, all.size());
    }

    @Test
    void shouldGetEvaluationHistory() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);

        for (int i = 0; i < 5; i++) {
            repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.0 + i * 0.1).build());
        }

        List<SloEvaluation> history = repository.getEvaluationHistory("test-slo", 3);
        assertEquals(3, history.size());
        // Most recent first
        assertEquals(99.4, history.get(0).currentValue(), 0.01);
    }

    @Test
    void shouldLimitEvaluationHistory() {
        InMemorySloRepository smallRepo = new InMemorySloRepository(3);
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        smallRepo.save(slo);

        for (int i = 0; i < 10; i++) {
            smallRepo.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.0 + i * 0.1).build());
        }

        List<SloEvaluation> history = smallRepo.getEvaluationHistory("test-slo", 100);
        assertEquals(3, history.size());
    }

    @Test
    void shouldClearEvaluations() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);
        repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.9).build());

        repository.clearEvaluations();

        assertTrue(repository.getLatestEvaluation("test-slo").isEmpty());
        // SLO definition should still exist
        assertTrue(repository.findByName("test-slo").isPresent());
    }

    @Test
    void shouldDeleteEvaluationsWhenDeletingSlo() {
        Slo slo = Slo.availability("test-slo", "desc", "m", 99.9, Duration.ofDays(30));
        repository.save(slo);
        repository.saveEvaluation(SloEvaluation.builder().slo(slo).currentValue(99.9).build());

        repository.delete("test-slo");

        assertTrue(repository.getLatestEvaluation("test-slo").isEmpty());
    }
}
