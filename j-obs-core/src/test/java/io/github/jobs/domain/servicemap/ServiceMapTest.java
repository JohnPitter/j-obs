package io.github.jobs.domain.servicemap;

import io.github.jobs.domain.servicemap.ServiceConnection.ConnectionType;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceHealth;
import io.github.jobs.domain.servicemap.ServiceNode.ServiceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceMapTest {

    @Test
    void shouldCreateEmptyServiceMap() {
        ServiceMap map = ServiceMap.empty();

        assertEquals(0, map.nodeCount());
        assertEquals(0, map.connectionCount());
        assertTrue(map.getNodes().isEmpty());
        assertTrue(map.getConnections().isEmpty());
    }

    @Test
    void shouldBuildServiceMap() {
        ServiceNode api = ServiceNode.of("api-service", ServiceType.SERVICE);
        ServiceNode db = ServiceNode.of("postgres", ServiceType.DATABASE);
        ServiceConnection conn = ServiceConnection.of("api-service", "postgres", ConnectionType.DATABASE);

        ServiceMap map = ServiceMap.builder()
                .addNode(api)
                .addNode(db)
                .addConnection(conn)
                .tracesAnalyzed(100)
                .build();

        assertEquals(2, map.nodeCount());
        assertEquals(1, map.connectionCount());
        assertEquals(100, map.getTracesAnalyzed());
    }

    @Test
    void shouldFindNodeById() {
        ServiceNode api = ServiceNode.of("api-service");
        ServiceMap map = ServiceMap.builder().addNode(api).build();

        assertTrue(map.getNode("api-service").isPresent());
        assertEquals("api-service", map.getNode("api-service").get().id());
        assertFalse(map.getNode("unknown").isPresent());
    }

    @Test
    void shouldFindConnectionById() {
        ServiceConnection conn = ServiceConnection.of("api", "db");
        ServiceMap map = ServiceMap.builder().addConnection(conn).build();

        assertTrue(map.getConnection("api->db").isPresent());
        assertFalse(map.getConnection("unknown").isPresent());
    }

    @Test
    void shouldGetOutgoingConnections() {
        ServiceConnection conn1 = ServiceConnection.of("api", "db");
        ServiceConnection conn2 = ServiceConnection.of("api", "cache");
        ServiceConnection conn3 = ServiceConnection.of("worker", "db");

        ServiceMap map = ServiceMap.builder()
                .addConnection(conn1)
                .addConnection(conn2)
                .addConnection(conn3)
                .build();

        List<ServiceConnection> apiOutgoing = map.getOutgoingConnections("api");
        assertEquals(2, apiOutgoing.size());

        List<ServiceConnection> workerOutgoing = map.getOutgoingConnections("worker");
        assertEquals(1, workerOutgoing.size());
    }

    @Test
    void shouldGetIncomingConnections() {
        ServiceConnection conn1 = ServiceConnection.of("api", "db");
        ServiceConnection conn2 = ServiceConnection.of("worker", "db");

        ServiceMap map = ServiceMap.builder()
                .addConnection(conn1)
                .addConnection(conn2)
                .build();

        List<ServiceConnection> dbIncoming = map.getIncomingConnections("db");
        assertEquals(2, dbIncoming.size());

        List<ServiceConnection> apiIncoming = map.getIncomingConnections("api");
        assertEquals(0, apiIncoming.size());
    }

    @Test
    void shouldGetNodesWithIssues() {
        ServiceNode healthy = new ServiceNode("healthy", "Healthy", ServiceType.SERVICE,
                ServiceHealth.HEALTHY, null);
        ServiceNode degraded = new ServiceNode("degraded", "Degraded", ServiceType.SERVICE,
                ServiceHealth.DEGRADED, null);
        ServiceNode unhealthy = new ServiceNode("unhealthy", "Unhealthy", ServiceType.SERVICE,
                ServiceHealth.UNHEALTHY, null);

        ServiceMap map = ServiceMap.builder()
                .addNode(healthy)
                .addNode(degraded)
                .addNode(unhealthy)
                .build();

        List<ServiceNode> issues = map.getNodesWithIssues();
        assertEquals(2, issues.size());
        assertTrue(issues.stream().anyMatch(n -> n.id().equals("degraded")));
        assertTrue(issues.stream().anyMatch(n -> n.id().equals("unhealthy")));
    }

    @Test
    void shouldCalculateStats() {
        ServiceNode healthy1 = new ServiceNode("h1", "Healthy 1", ServiceType.SERVICE,
                ServiceHealth.HEALTHY, null);
        ServiceNode healthy2 = new ServiceNode("h2", "Healthy 2", ServiceType.SERVICE,
                ServiceHealth.HEALTHY, null);
        ServiceNode degraded = new ServiceNode("d1", "Degraded", ServiceType.SERVICE,
                ServiceHealth.DEGRADED, null);
        ServiceNode unhealthy = new ServiceNode("u1", "Unhealthy", ServiceType.SERVICE,
                ServiceHealth.UNHEALTHY, null);

        ServiceMap map = ServiceMap.builder()
                .addNode(healthy1)
                .addNode(healthy2)
                .addNode(degraded)
                .addNode(unhealthy)
                .tracesAnalyzed(500)
                .build();

        ServiceMap.ServiceMapStats stats = map.getStats();
        assertEquals(4, stats.totalNodes());
        assertEquals(2, stats.healthyNodes());
        assertEquals(1, stats.degradedNodes());
        assertEquals(1, stats.unhealthyNodes());
        assertEquals(500, stats.tracesAnalyzed());
        assertNotNull(stats.generatedAt());
    }

    @Test
    void shouldHaveGeneratedAtTimestamp() {
        ServiceMap map = ServiceMap.empty();

        assertNotNull(map.getGeneratedAt());
    }

    @Test
    void shouldGetMaxRequestsPerSecond() {
        ServiceConnection.ConnectionStats stats1 = new ServiceConnection.ConnectionStats(100, 0, 0, 0, 0, 0);
        ServiceConnection.ConnectionStats stats2 = new ServiceConnection.ConnectionStats(50, 0, 0, 0, 0, 0);

        ServiceConnection conn1 = new ServiceConnection("c1", "a", "b", ConnectionType.HTTP, stats1);
        ServiceConnection conn2 = new ServiceConnection("c2", "b", "c", ConnectionType.HTTP, stats2);

        ServiceMap map = ServiceMap.builder()
                .addConnection(conn1)
                .addConnection(conn2)
                .build();

        assertEquals(100, map.getMaxRequestsPerSecond());
    }

    @Test
    void shouldReturnZeroMaxRpsForEmptyMap() {
        ServiceMap map = ServiceMap.empty();
        assertEquals(0, map.getMaxRequestsPerSecond());
    }
}
