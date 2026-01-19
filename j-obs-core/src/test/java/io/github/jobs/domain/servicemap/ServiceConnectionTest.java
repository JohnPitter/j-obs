package io.github.jobs.domain.servicemap;

import io.github.jobs.domain.servicemap.ServiceConnection.ConnectionStats;
import io.github.jobs.domain.servicemap.ServiceConnection.ConnectionType;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceHealth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceConnectionTest {

    @Test
    void shouldCreateConnectionWithAllFields() {
        ConnectionStats stats = new ConnectionStats(100, 0.5, 50, 100, 1000, 5);
        ServiceConnection conn = new ServiceConnection(
                "api->db",
                "api-service",
                "postgres",
                ConnectionType.DATABASE,
                stats
        );

        assertEquals("api->db", conn.id());
        assertEquals("api-service", conn.sourceId());
        assertEquals("postgres", conn.targetId());
        assertEquals(ConnectionType.DATABASE, conn.type());
        assertEquals(stats, conn.stats());
    }

    @Test
    void shouldGenerateIdIfNotProvided() {
        ServiceConnection conn = new ServiceConnection(
                null,
                "api-service",
                "postgres",
                ConnectionType.DATABASE,
                null
        );

        assertEquals("api-service->postgres", conn.id());
    }

    @Test
    void shouldDefaultTypeToHttp() {
        ServiceConnection conn = new ServiceConnection(
                null,
                "api-service",
                "user-service",
                null,
                null
        );

        assertEquals(ConnectionType.HTTP, conn.type());
    }

    @Test
    void shouldDefaultStatsToEmpty() {
        ServiceConnection conn = new ServiceConnection(
                null,
                "api-service",
                "user-service",
                null,
                null
        );

        assertNotNull(conn.stats());
        assertEquals(0, conn.stats().totalRequests());
    }

    @Test
    void shouldCreateConnectionWithFactoryMethod() {
        ServiceConnection conn = ServiceConnection.of("api", "db");

        assertEquals("api->db", conn.id());
        assertEquals("api", conn.sourceId());
        assertEquals("db", conn.targetId());
        assertEquals(ConnectionType.HTTP, conn.type());
    }

    @Test
    void shouldCreateConnectionWithType() {
        ServiceConnection conn = ServiceConnection.of("api", "postgres", ConnectionType.DATABASE);

        assertEquals(ConnectionType.DATABASE, conn.type());
    }

    @Test
    void shouldRequireSourceAndTarget() {
        assertThrows(NullPointerException.class, () ->
                new ServiceConnection(null, null, "target", null, null)
        );
        assertThrows(NullPointerException.class, () ->
                new ServiceConnection(null, "source", null, null, null)
        );
    }

    @Test
    void connectionTypeShouldHaveProperties() {
        for (ConnectionType type : ConnectionType.values()) {
            assertNotNull(type.displayName());
            assertNotNull(type.lineStyle());
            assertNotNull(type.color());
        }
    }

    @Test
    void connectionStatsShouldCalculateHealth() {
        ConnectionStats healthy = new ConnectionStats(100, 0.5, 50, 100, 1000, 5);
        assertEquals(ServiceHealth.HEALTHY, healthy.calculateHealth(5.0));

        ConnectionStats degraded = new ConnectionStats(100, 3.0, 50, 100, 1000, 30);
        assertEquals(ServiceHealth.DEGRADED, degraded.calculateHealth(5.0));

        ConnectionStats unhealthy = new ConnectionStats(100, 10.0, 50, 100, 1000, 100);
        assertEquals(ServiceHealth.UNHEALTHY, unhealthy.calculateHealth(5.0));

        ConnectionStats unknown = ConnectionStats.empty();
        assertEquals(ServiceHealth.UNKNOWN, unknown.calculateHealth(5.0));
    }

    @Test
    void connectionStatsShouldCalculateLineThickness() {
        ConnectionStats highTraffic = new ConnectionStats(100, 0, 0, 0, 0, 0);
        assertEquals(8, highTraffic.lineThickness(100));

        ConnectionStats mediumTraffic = new ConnectionStats(50, 0, 0, 0, 0, 0);
        assertEquals(4, mediumTraffic.lineThickness(100));

        ConnectionStats lowTraffic = new ConnectionStats(10, 0, 0, 0, 0, 0);
        assertEquals(1, lowTraffic.lineThickness(100));

        ConnectionStats noTraffic = ConnectionStats.empty();
        assertEquals(1, noTraffic.lineThickness(100));
    }

    @Test
    void emptyStatsShouldHaveZeroValues() {
        ConnectionStats empty = ConnectionStats.empty();

        assertEquals(0, empty.requestsPerSecond());
        assertEquals(0, empty.errorRate());
        assertEquals(0, empty.avgLatencyMs());
        assertEquals(0, empty.p99LatencyMs());
        assertEquals(0, empty.totalRequests());
        assertEquals(0, empty.totalErrors());
    }
}
