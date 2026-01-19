package io.github.jobs.domain.alert;

import io.github.jobs.domain.alert.AlertCondition.ComparisonOperator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertConditionTest {

    @Test
    void shouldCreateConditionWithBuilder() {
        AlertCondition condition = AlertCondition.builder()
                .metric("error_rate")
                .operator(ComparisonOperator.GREATER_THAN)
                .threshold(5.0)
                .window(Duration.ofMinutes(10))
                .filters(Map.of("service", "api"))
                .build();

        assertEquals("error_rate", condition.metric());
        assertEquals(ComparisonOperator.GREATER_THAN, condition.operator());
        assertEquals(5.0, condition.threshold());
        assertEquals(Duration.ofMinutes(10), condition.window());
        assertEquals(Map.of("service", "api"), condition.filters());
    }

    @Test
    void shouldCreateSimpleCondition() {
        AlertCondition condition = AlertCondition.of("latency_p99", ComparisonOperator.GREATER_THAN, 200);

        assertEquals("latency_p99", condition.metric());
        assertEquals(ComparisonOperator.GREATER_THAN, condition.operator());
        assertEquals(200.0, condition.threshold());
        assertEquals(Duration.ofMinutes(5), condition.window());
        assertTrue(condition.filters().isEmpty());
    }

    @Test
    void shouldDefaultWindowToFiveMinutes() {
        AlertCondition condition = AlertCondition.builder()
                .metric("test")
                .operator(ComparisonOperator.GREATER_THAN)
                .threshold(1.0)
                .build();

        assertEquals(Duration.ofMinutes(5), condition.window());
    }

    @Test
    void shouldDefaultFiltersToEmptyMap() {
        AlertCondition condition = AlertCondition.builder()
                .metric("test")
                .operator(ComparisonOperator.GREATER_THAN)
                .threshold(1.0)
                .build();

        assertNotNull(condition.filters());
        assertTrue(condition.filters().isEmpty());
    }

    @Test
    void shouldRequireMetric() {
        assertThrows(NullPointerException.class, () ->
                AlertCondition.builder()
                        .operator(ComparisonOperator.GREATER_THAN)
                        .threshold(1.0)
                        .build()
        );
    }

    @Test
    void shouldRequireOperator() {
        assertThrows(NullPointerException.class, () ->
                AlertCondition.builder()
                        .metric("test")
                        .threshold(1.0)
                        .build()
        );
    }

    @Test
    void shouldEvaluateGreaterThan() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.GREATER_THAN, 10);

        assertTrue(condition.evaluate(15));
        assertFalse(condition.evaluate(10));
        assertFalse(condition.evaluate(5));
    }

    @Test
    void shouldEvaluateGreaterThanOrEqual() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.GREATER_THAN_OR_EQUAL, 10);

        assertTrue(condition.evaluate(15));
        assertTrue(condition.evaluate(10));
        assertFalse(condition.evaluate(5));
    }

    @Test
    void shouldEvaluateLessThan() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.LESS_THAN, 10);

        assertTrue(condition.evaluate(5));
        assertFalse(condition.evaluate(10));
        assertFalse(condition.evaluate(15));
    }

    @Test
    void shouldEvaluateLessThanOrEqual() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.LESS_THAN_OR_EQUAL, 10);

        assertTrue(condition.evaluate(5));
        assertTrue(condition.evaluate(10));
        assertFalse(condition.evaluate(15));
    }

    @Test
    void shouldEvaluateEquals() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.EQUALS, 10);

        assertTrue(condition.evaluate(10));
        assertTrue(condition.evaluate(10.00001)); // Within tolerance
        assertFalse(condition.evaluate(10.001)); // Outside tolerance
        assertFalse(condition.evaluate(5));
    }

    @Test
    void shouldEvaluateNotEquals() {
        AlertCondition condition = AlertCondition.of("test", ComparisonOperator.NOT_EQUALS, 10);

        assertFalse(condition.evaluate(10));
        assertFalse(condition.evaluate(10.00001)); // Within tolerance
        assertTrue(condition.evaluate(10.001)); // Outside tolerance
        assertTrue(condition.evaluate(5));
    }

    @Test
    void shouldDescribeCondition() {
        AlertCondition condition = AlertCondition.of("error_rate", ComparisonOperator.GREATER_THAN, 5);

        String description = condition.describe();
        assertEquals("error_rate > 5.0", description);
    }

    @Test
    void comparisonOperatorsShouldHaveSymbols() {
        assertEquals(">", ComparisonOperator.GREATER_THAN.symbol());
        assertEquals(">=", ComparisonOperator.GREATER_THAN_OR_EQUAL.symbol());
        assertEquals("<", ComparisonOperator.LESS_THAN.symbol());
        assertEquals("<=", ComparisonOperator.LESS_THAN_OR_EQUAL.symbol());
        assertEquals("==", ComparisonOperator.EQUALS.symbol());
        assertEquals("!=", ComparisonOperator.NOT_EQUALS.symbol());
    }

    @Test
    void comparisonOperatorsShouldHaveDescriptions() {
        for (ComparisonOperator op : ComparisonOperator.values()) {
            assertNotNull(op.description(), "description should not be null for " + op.name());
            assertFalse(op.description().isEmpty(), "description should not be empty for " + op.name());
        }
    }
}
