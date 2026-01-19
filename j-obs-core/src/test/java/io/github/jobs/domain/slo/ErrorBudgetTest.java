package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ErrorBudgetTest {

    @Test
    void shouldCalculateErrorBudget() {
        // 99.9% SLO = 0.1% error budget
        ErrorBudget budget = ErrorBudget.calculate(99.9, 99.95, Duration.ofDays(30));

        assertEquals(0.1, budget.totalBudget(), 0.001);
        assertEquals(0.0, budget.consumedBudget(), 0.001); // Current > objective
        assertEquals(0.1, budget.remainingBudget(), 0.001);
        assertEquals(100.0, budget.remainingPercentage(), 0.001);
    }

    @Test
    void shouldCalculateConsumedBudget() {
        // 99.9% SLO, currently at 99.85% = 0.05% consumed
        ErrorBudget budget = ErrorBudget.calculate(99.9, 99.85, Duration.ofDays(30));

        assertEquals(0.1, budget.totalBudget(), 0.001);
        assertEquals(0.05, budget.consumedBudget(), 0.001);
        assertEquals(0.05, budget.remainingBudget(), 0.001);
        assertEquals(50.0, budget.remainingPercentage(), 0.001);
    }

    @Test
    void shouldIdentifyExhaustedBudget() {
        // Objective 99.9%, current 99.7% = fully consumed
        ErrorBudget exhausted = ErrorBudget.calculate(99.9, 99.7, Duration.ofDays(30));
        assertTrue(exhausted.isExhausted());

        ErrorBudget healthy = ErrorBudget.calculate(99.9, 99.95, Duration.ofDays(30));
        assertFalse(healthy.isExhausted());
    }

    @Test
    void shouldIdentifyAtRiskBudget() {
        // For 99.9% SLO, budget is 0.1%
        // At risk when remaining <= 25% of budget
        // 25% of 0.1% = 0.025%
        // Current = 99.9 - (0.1 - 0.025) = 99.825%
        ErrorBudget atRisk = ErrorBudget.calculate(99.9, 99.82, Duration.ofDays(30));
        assertTrue(atRisk.isAtRisk());

        // 50% remaining is not at risk (current = 99.9 - 0.05 = 99.85)
        ErrorBudget healthy = ErrorBudget.calculate(99.9, 99.85, Duration.ofDays(30));
        assertFalse(healthy.isAtRisk());
    }

    @Test
    void shouldIdentifyHealthyBudget() {
        ErrorBudget healthy = ErrorBudget.calculate(99.9, 99.95, Duration.ofDays(30));
        assertTrue(healthy.isHealthy());

        // At risk (not healthy) when remaining <= 25%
        ErrorBudget atRisk = ErrorBudget.calculate(99.9, 99.82, Duration.ofDays(30));
        assertFalse(atRisk.isHealthy());
    }

    @Test
    void shouldFormatRemainingTime() {
        ErrorBudget budget = ErrorBudget.calculate(99.9, 99.95, Duration.ofDays(30));
        String formatted = budget.formatRemainingTime();
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
    }

    @Test
    void shouldCreateEmptyBudget() {
        ErrorBudget empty = ErrorBudget.empty(99.9, Duration.ofDays(30));

        assertEquals(0.1, empty.totalBudget(), 0.001);
        assertEquals(0.0, empty.consumedBudget(), 0.001);
        assertEquals(0.1, empty.remainingBudget(), 0.001);
        assertEquals(100.0, empty.remainingPercentage(), 0.001);
    }
}
