package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Service Map API endpoints.
 */
@DisplayName("Service Map API E2E Tests")
class ServiceMapApiE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /api/service-map returns service map data")
    void getServiceMap() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/service-map?window=1h");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("nodes");
        assertThat(body).containsKey("connections");
        assertThat(body).containsKey("stats");
    }

    @Test
    @DisplayName("GET /api/service-map/stats returns service map statistics")
    void getStats() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/service-map/stats");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("totalNodes");
        assertThat(body).containsKey("totalConnections");
        assertThat(body).containsKey("healthyNodes");
        assertThat(body).containsKey("degradedNodes");
    }

    @Test
    @DisplayName("GET /api/service-map/nodes returns nodes list")
    void getNodes() {
        ResponseEntity<String> response = getString("/api/service-map/nodes");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    @DisplayName("GET /api/service-map/connections returns connections list")
    void getConnections() {
        ResponseEntity<String> response = getString("/api/service-map/connections");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    @DisplayName("POST /api/service-map/refresh rebuilds service map")
    void refreshServiceMap() {
        ResponseEntity<Map<String, Object>> response = postJson("/api/service-map/refresh", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("nodes");
        assertThat(body).containsKey("connections");
        assertThat(body).containsKey("stats");
    }
}
