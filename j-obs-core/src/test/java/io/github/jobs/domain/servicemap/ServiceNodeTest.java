package io.github.jobs.domain.servicemap;

import io.github.jobs.domain.servicemap.ServiceNode.ServiceHealth;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceStats;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceNodeTest {

    @Test
    void shouldCreateServiceNodeWithAllFields() {
        ServiceStats stats = new ServiceStats(100, 0.5, 50, 100, 1000, 5);
        ServiceNode node = new ServiceNode(
                "user-service",
                "User Service",
                ServiceType.SERVICE,
                ServiceHealth.HEALTHY,
                stats
        );

        assertEquals("user-service", node.id());
        assertEquals("User Service", node.name());
        assertEquals(ServiceType.SERVICE, node.type());
        assertEquals(ServiceHealth.HEALTHY, node.health());
        assertEquals(stats, node.stats());
    }

    @Test
    void shouldCreateServiceNodeWithDefaults() {
        ServiceNode node = new ServiceNode("test", "Test", null, null, null);

        assertEquals(ServiceType.SERVICE, node.type());
        assertEquals(ServiceHealth.UNKNOWN, node.health());
        assertNotNull(node.stats());
    }

    @Test
    void shouldCreateServiceNodeWithFactoryMethod() {
        ServiceNode node = ServiceNode.of("api-service");

        assertEquals("api-service", node.id());
        assertEquals("api-service", node.name());
        assertEquals(ServiceType.SERVICE, node.type());
    }

    @Test
    void shouldCreateServiceNodeWithType() {
        ServiceNode node = ServiceNode.of("postgres", ServiceType.DATABASE);

        assertEquals("postgres", node.id());
        assertEquals(ServiceType.DATABASE, node.type());
    }

    @Test
    void shouldRequireIdAndName() {
        assertThrows(NullPointerException.class, () ->
                new ServiceNode(null, "Test", null, null, null)
        );
        assertThrows(NullPointerException.class, () ->
                new ServiceNode("test", null, null, null, null)
        );
    }

    @Test
    void serviceTypeShouldHaveProperties() {
        for (ServiceType type : ServiceType.values()) {
            assertNotNull(type.displayName());
            assertNotNull(type.shape());
            assertNotNull(type.color());
        }
    }

    @Test
    void serviceHealthShouldHaveProperties() {
        for (ServiceHealth health : ServiceHealth.values()) {
            assertNotNull(health.displayName());
            assertNotNull(health.textCssClass());
            assertNotNull(health.bgCssClass());
            assertNotNull(health.color());
        }
    }

    @Test
    void serviceHealthShouldIdentifyHealthy() {
        assertTrue(ServiceHealth.HEALTHY.isHealthy());
        assertFalse(ServiceHealth.DEGRADED.isHealthy());
        assertFalse(ServiceHealth.UNHEALTHY.isHealthy());
        assertFalse(ServiceHealth.UNKNOWN.isHealthy());
    }

    @Test
    void serviceHealthShouldIdentifyNeedsAttention() {
        assertFalse(ServiceHealth.HEALTHY.needsAttention());
        assertTrue(ServiceHealth.DEGRADED.needsAttention());
        assertTrue(ServiceHealth.UNHEALTHY.needsAttention());
        assertFalse(ServiceHealth.UNKNOWN.needsAttention());
    }

    @Test
    void serviceStatsShouldCalculateHealth() {
        ServiceStats healthy = new ServiceStats(100, 0.5, 50, 100, 1000, 5);
        assertEquals(ServiceHealth.HEALTHY, healthy.calculateHealth(5.0, 1000));

        ServiceStats degraded = new ServiceStats(100, 3.0, 50, 1500, 1000, 30);
        assertEquals(ServiceHealth.DEGRADED, degraded.calculateHealth(5.0, 1000));

        ServiceStats unhealthy = new ServiceStats(100, 10.0, 50, 100, 1000, 100);
        assertEquals(ServiceHealth.UNHEALTHY, unhealthy.calculateHealth(5.0, 1000));

        ServiceStats unknown = ServiceStats.empty();
        assertEquals(ServiceHealth.UNKNOWN, unknown.calculateHealth(5.0, 1000));
    }

    @Test
    void emptyStatsShouldHaveZeroValues() {
        ServiceStats empty = ServiceStats.empty();

        assertEquals(0, empty.requestsPerSecond());
        assertEquals(0, empty.errorRate());
        assertEquals(0, empty.avgLatencyMs());
        assertEquals(0, empty.p99LatencyMs());
        assertEquals(0, empty.totalRequests());
        assertEquals(0, empty.totalErrors());
    }
}
