package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SliTypeTest {

    @Test
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Availability", SliType.AVAILABILITY.displayName());
        assertEquals("Latency", SliType.LATENCY.displayName());
        assertEquals("Error Rate", SliType.ERROR_RATE.displayName());
        assertEquals("Throughput", SliType.THROUGHPUT.displayName());
    }

    @Test
    void shouldHaveDescriptions() {
        for (SliType type : SliType.values()) {
            assertNotNull(type.description());
            assertFalse(type.description().isEmpty());
        }
    }

    @Test
    void shouldHaveUnits() {
        assertEquals("%", SliType.AVAILABILITY.unit());
        assertEquals("ms", SliType.LATENCY.unit());
        assertEquals("%", SliType.ERROR_RATE.unit());
        assertEquals("req/s", SliType.THROUGHPUT.unit());
    }

    @Test
    void shouldHaveAllTypes() {
        assertEquals(4, SliType.values().length);
    }
}
