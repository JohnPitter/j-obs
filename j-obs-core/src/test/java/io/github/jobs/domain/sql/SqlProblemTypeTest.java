package io.github.jobs.domain.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlProblemTypeTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("N+1 Query", SqlProblemType.N_PLUS_ONE.displayName());
        assertEquals("Slow Query", SqlProblemType.SLOW_QUERY.displayName());
        assertEquals("Very Slow Query", SqlProblemType.VERY_SLOW_QUERY.displayName());
        assertEquals("SELECT *", SqlProblemType.SELECT_STAR.displayName());
        assertEquals("Large Result Set", SqlProblemType.LARGE_RESULT_SET.displayName());
        assertEquals("Missing LIMIT", SqlProblemType.MISSING_LIMIT.displayName());
        assertEquals("Cartesian Join", SqlProblemType.CARTESIAN_JOIN.displayName());
        assertEquals("Missing Index", SqlProblemType.MISSING_INDEX.displayName());
    }

    @Test
    void shouldIdentifyCriticalProblems() {
        assertTrue(SqlProblemType.N_PLUS_ONE.isCritical());
        assertTrue(SqlProblemType.VERY_SLOW_QUERY.isCritical());
        assertTrue(SqlProblemType.CARTESIAN_JOIN.isCritical());
    }

    @Test
    void shouldIdentifyNonCriticalProblems() {
        assertFalse(SqlProblemType.SLOW_QUERY.isCritical());
        assertFalse(SqlProblemType.SELECT_STAR.isCritical());
        assertFalse(SqlProblemType.MISSING_LIMIT.isCritical());
        assertFalse(SqlProblemType.MISSING_INDEX.isCritical());
    }

    @Test
    void shouldIdentifyWarnings() {
        assertTrue(SqlProblemType.SLOW_QUERY.isWarning());
        assertTrue(SqlProblemType.LARGE_RESULT_SET.isWarning());
        assertTrue(SqlProblemType.MISSING_LIMIT.isWarning());
        assertTrue(SqlProblemType.MISSING_INDEX.isWarning());
    }

    @Test
    void shouldHaveCorrectSeverity() {
        assertEquals("critical", SqlProblemType.N_PLUS_ONE.severity());
        assertEquals("warning", SqlProblemType.SLOW_QUERY.severity());
        assertEquals("info", SqlProblemType.SELECT_STAR.severity());
    }

    @Test
    void shouldHaveCssClasses() {
        assertNotNull(SqlProblemType.N_PLUS_ONE.cssClass());
        assertNotNull(SqlProblemType.N_PLUS_ONE.bgCssClass());

        assertTrue(SqlProblemType.N_PLUS_ONE.cssClass().contains("red"));
        assertTrue(SqlProblemType.SLOW_QUERY.cssClass().contains("amber"));
        assertTrue(SqlProblemType.SELECT_STAR.cssClass().contains("blue"));
    }

    @Test
    void shouldHaveDescriptions() {
        assertNotNull(SqlProblemType.N_PLUS_ONE.description());
        assertTrue(SqlProblemType.N_PLUS_ONE.description().length() > 0);

        for (SqlProblemType type : SqlProblemType.values()) {
            assertNotNull(type.description(), "Description should not be null for " + type.name());
        }
    }
}
