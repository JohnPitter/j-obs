package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Health API endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Health API E2E Tests")
class HealthApiE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("GET /api/health returns 200 with status field")
    void getHealth_shouldReturn200WithStatusField() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/health");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");

        String status = (String) response.getBody().get("status");
        assertThat(status).isIn("UP", "DOWN", "UNKNOWN", "DEGRADED");

        assertThat(response.getBody()).containsKey("healthy");
        assertThat(response.getBody()).containsKey("components");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/health/components returns 200 with components list")
    void getComponents_shouldReturn200WithComponentsList() {
        ResponseEntity<String> response = getString("/api/health/components");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Response is a JSON array or list of component objects
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/health/summary returns 200 with summary fields")
    void getSummary_shouldReturn200WithSummaryFields() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/health/summary");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody()).containsKey("statusDisplayName");
        assertThat(response.getBody()).containsKey("healthy");
        assertThat(response.getBody()).containsKey("totalComponents");
        assertThat(response.getBody()).containsKey("healthyComponents");
        assertThat(response.getBody()).containsKey("unhealthyComponents");
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/health/refresh returns 200 and triggers re-check")
    void refreshHealth_shouldReturn200() {
        ResponseEntity<Map<String, Object>> response = postJson("/api/health/refresh", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");

        String status = (String) response.getBody().get("status");
        assertThat(status).isIn("UP", "DOWN", "UNKNOWN", "DEGRADED");
    }
}
