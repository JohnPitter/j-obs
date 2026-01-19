package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SloStatusTest {

    @Test
    void shouldHaveDisplayNames() {
        assertEquals("Healthy", SloStatus.HEALTHY.displayName());
        assertEquals("At Risk", SloStatus.AT_RISK.displayName());
        assertEquals("Breached", SloStatus.BREACHED.displayName());
        assertEquals("No Data", SloStatus.NO_DATA.displayName());
    }

    @Test
    void shouldHaveCssClasses() {
        for (SloStatus status : SloStatus.values()) {
            assertNotNull(status.textCssClass());
            assertNotNull(status.bgCssClass());
            assertFalse(status.textCssClass().isEmpty());
            assertFalse(status.bgCssClass().isEmpty());
        }
    }

    @Test
    void shouldHaveColors() {
        for (SloStatus status : SloStatus.values()) {
            assertNotNull(status.color());
            assertTrue(status.color().startsWith("#"));
        }
    }

    @Test
    void shouldIdentifyNeedsAttention() {
        assertFalse(SloStatus.HEALTHY.needsAttention());
        assertTrue(SloStatus.AT_RISK.needsAttention());
        assertTrue(SloStatus.BREACHED.needsAttention());
        assertFalse(SloStatus.NO_DATA.needsAttention());
    }

    @Test
    void shouldIdentifyHealthy() {
        assertTrue(SloStatus.HEALTHY.isHealthy());
        assertFalse(SloStatus.AT_RISK.isHealthy());
        assertFalse(SloStatus.BREACHED.isHealthy());
        assertFalse(SloStatus.NO_DATA.isHealthy());
    }
}
