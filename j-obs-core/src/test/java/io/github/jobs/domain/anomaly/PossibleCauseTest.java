package io.github.jobs.domain.anomaly;

import io.github.jobs.domain.anomaly.PossibleCause.CauseType;
import io.github.jobs.domain.anomaly.PossibleCause.Confidence;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PossibleCauseTest {

    @Test
    void shouldCreatePossibleCause() {
        PossibleCause cause = new PossibleCause(
                "Database query taking too long",
                CauseType.SLOW_QUERIES,
                Confidence.HIGH,
                "Query X taking 5s average"
        );

        assertEquals("Database query taking too long", cause.description());
        assertEquals(CauseType.SLOW_QUERIES, cause.type());
        assertEquals(Confidence.HIGH, cause.confidence());
        assertEquals("Query X taking 5s average", cause.details());
    }

    @Test
    void shouldCreateWithSimpleConstructor() {
        PossibleCause cause = new PossibleCause("Test cause", Confidence.MEDIUM);

        assertEquals("Test cause", cause.description());
        assertEquals(CauseType.UNKNOWN, cause.type());
        assertEquals(Confidence.MEDIUM, cause.confidence());
        assertNull(cause.details());
    }

    @Test
    void shouldRequireDescription() {
        assertThrows(NullPointerException.class, () ->
                new PossibleCause(null, Confidence.HIGH)
        );
    }

    @Test
    void shouldDefaultToUnknownType() {
        PossibleCause cause = new PossibleCause("Test", null, Confidence.HIGH, null);
        assertEquals(CauseType.UNKNOWN, cause.type());
    }

    @Test
    void shouldDefaultToLowConfidence() {
        PossibleCause cause = new PossibleCause("Test", CauseType.SLOW_QUERIES, null, null);
        assertEquals(Confidence.LOW, cause.confidence());
    }

    @Test
    void shouldIdentifyHighConfidenceCause() {
        PossibleCause high = new PossibleCause("Test", Confidence.HIGH);
        PossibleCause medium = new PossibleCause("Test", Confidence.MEDIUM);
        PossibleCause low = new PossibleCause("Test", Confidence.LOW);

        assertTrue(high.isHighConfidence());
        assertFalse(medium.isHighConfidence());
        assertFalse(low.isHighConfidence());
    }

    @Test
    void shouldIdentifyLikelyCause() {
        PossibleCause high = new PossibleCause("Test", Confidence.HIGH);
        PossibleCause medium = new PossibleCause("Test", Confidence.MEDIUM);
        PossibleCause low = new PossibleCause("Test", Confidence.LOW);

        assertTrue(high.isLikelyCause());
        assertTrue(medium.isLikelyCause());
        assertFalse(low.isLikelyCause());
    }

    @Test
    void shouldHaveCauseTypeDisplayNames() {
        assertEquals("Dependency Degradation", CauseType.DEPENDENCY_DEGRADATION.displayName());
        assertEquals("Recent Deploy", CauseType.RECENT_DEPLOY.displayName());
        assertEquals("Slow Database Queries", CauseType.SLOW_QUERIES.displayName());
        assertEquals("Traffic Change", CauseType.TRAFFIC_CHANGE.displayName());
        assertEquals("Resource Exhaustion", CauseType.RESOURCE_EXHAUSTION.displayName());
        assertEquals("External Service Issue", CauseType.EXTERNAL_SERVICE.displayName());
        assertEquals("Configuration Change", CauseType.CONFIGURATION_CHANGE.displayName());
        assertEquals("Unknown", CauseType.UNKNOWN.displayName());
    }

    @Test
    void shouldHaveConfidenceProperties() {
        assertEquals("High", Confidence.HIGH.displayName());
        assertEquals("Medium", Confidence.MEDIUM.displayName());
        assertEquals("Low", Confidence.LOW.displayName());

        assertNotNull(Confidence.HIGH.cssClass());
        assertTrue(Confidence.HIGH.cssClass().contains("green"));
        assertTrue(Confidence.MEDIUM.cssClass().contains("amber"));
        assertTrue(Confidence.LOW.cssClass().contains("gray"));
    }

    @Test
    void shouldCalculateConfidenceFromScore() {
        assertEquals(Confidence.HIGH, Confidence.fromScore(0.9));
        assertEquals(Confidence.HIGH, Confidence.fromScore(0.8));
        assertEquals(Confidence.MEDIUM, Confidence.fromScore(0.6));
        assertEquals(Confidence.MEDIUM, Confidence.fromScore(0.5));
        assertEquals(Confidence.LOW, Confidence.fromScore(0.3));
        assertEquals(Confidence.LOW, Confidence.fromScore(0.1));
    }
}
