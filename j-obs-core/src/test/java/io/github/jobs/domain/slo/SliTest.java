package io.github.jobs.domain.slo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SliTest {

    @Test
    void shouldCreateAvailabilitySli() {
        Sli sli = Sli.availability("http_requests_total", "status < 500", "status >= 0");

        assertEquals(SliType.AVAILABILITY, sli.type());
        assertEquals("http_requests_total", sli.metric());
        assertEquals("status < 500", sli.goodCondition());
        assertEquals("status >= 0", sli.totalCondition());
        assertNull(sli.threshold());
        assertNull(sli.percentile());
    }

    @Test
    void shouldCreateLatencySli() {
        Sli sli = Sli.latency("http_request_duration", 200.0, 99);

        assertEquals(SliType.LATENCY, sli.type());
        assertEquals("http_request_duration", sli.metric());
        assertEquals(200.0, sli.threshold());
        assertEquals(99, sli.percentile());
        assertNull(sli.goodCondition());
        assertNull(sli.totalCondition());
    }

    @Test
    void shouldCreateErrorRateSli() {
        Sli sli = Sli.errorRate("http_requests_total", "status < 500", "status >= 0");

        assertEquals(SliType.ERROR_RATE, sli.type());
        assertEquals("http_requests_total", sli.metric());
        assertEquals("status < 500", sli.goodCondition());
    }

    @Test
    void shouldCreateThroughputSli() {
        Sli sli = Sli.throughput("requests_per_second", 100.0);

        assertEquals(SliType.THROUGHPUT, sli.type());
        assertEquals("requests_per_second", sli.metric());
        assertEquals(100.0, sli.threshold());
    }

    @Test
    void shouldIdentifyRatioBasedSli() {
        assertTrue(Sli.availability("m", "g", "t").isRatioBased());
        assertTrue(Sli.errorRate("m", "g", "t").isRatioBased());
        assertFalse(Sli.latency("m", 200, 99).isRatioBased());
        assertFalse(Sli.throughput("m", 100).isRatioBased());
    }

    @Test
    void shouldIdentifyThresholdBasedSli() {
        assertFalse(Sli.availability("m", "g", "t").isThresholdBased());
        assertFalse(Sli.errorRate("m", "g", "t").isThresholdBased());
        assertTrue(Sli.latency("m", 200, 99).isThresholdBased());
        assertTrue(Sli.throughput("m", 100).isThresholdBased());
    }

    @Test
    void shouldRequireType() {
        assertThrows(NullPointerException.class, () ->
                new Sli(null, "metric", null, null, null, null));
    }

    @Test
    void shouldRequireMetric() {
        assertThrows(NullPointerException.class, () ->
                new Sli(SliType.AVAILABILITY, null, null, null, null, null));
    }
}
