package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SloEvaluationTest {

    @Test
    void shouldCreateEvaluationWithBuilder() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.95)
                .events(9995, 10000)
                .evaluatedAt(Instant.now())
                .build();

        assertEquals("test", evaluation.sloName());
        assertEquals(99.95, evaluation.currentValue());
        assertEquals(SloStatus.HEALTHY, evaluation.status());
        assertEquals(9995, evaluation.goodEvents());
        assertEquals(10000, evaluation.totalEvents());
        assertNotNull(evaluation.errorBudget());
    }

    @Test
    void shouldCreateNoDataEvaluation() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation evaluation = SloEvaluation.noData(slo);

        assertEquals("test", evaluation.sloName());
        assertTrue(Double.isNaN(evaluation.currentValue()));
        assertEquals(SloStatus.NO_DATA, evaluation.status());
        assertEquals(0, evaluation.goodEvents());
        assertEquals(0, evaluation.totalEvents());
    }

    @Test
    void shouldCheckIfMet() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation met = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.95)
                .build();
        assertTrue(met.isMet());

        SloEvaluation notMet = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.0)
                .build();
        assertFalse(notMet.isMet());
    }

    @Test
    void shouldGetMaxBurnRate() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        List<BurnRate> burnRates = List.of(
                new BurnRate(1.5, Duration.ofHours(1), Duration.ZERO, Instant.now()),
                new BurnRate(3.0, Duration.ofHours(6), Duration.ZERO, Instant.now())
        );

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.9)
                .burnRates(burnRates)
                .build();

        assertEquals(3.0, evaluation.getMaxBurnRate());
    }

    @Test
    void shouldReturnZeroMaxBurnRateWhenEmpty() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.9)
                .burnRates(List.of())
                .build();

        assertEquals(0.0, evaluation.getMaxBurnRate());
    }

    @Test
    void shouldFormatCurrentValue() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.95)
                .build();

        assertEquals("99.95%", evaluation.formatCurrentValue());
    }

    @Test
    void shouldFormatCurrentValueAsNAWhenNaN() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));
        SloEvaluation evaluation = SloEvaluation.noData(slo);

        assertEquals("N/A", evaluation.formatCurrentValue());
    }

    @Test
    void shouldFormatObjective() {
        Slo slo = Slo.availability("test", "desc", "m", 99.9, Duration.ofDays(30));

        SloEvaluation evaluation = SloEvaluation.builder()
                .slo(slo)
                .currentValue(99.9)
                .build();

        assertEquals("99.90%", evaluation.formatObjective());
    }

    @Test
    void shouldRequireSloName() {
        assertThrows(NullPointerException.class, () ->
                new SloEvaluation(null, null, 99.0, SloStatus.HEALTHY,
                        ErrorBudget.empty(99.9, Duration.ofDays(30)), null, 0, 0, null));
    }
}
