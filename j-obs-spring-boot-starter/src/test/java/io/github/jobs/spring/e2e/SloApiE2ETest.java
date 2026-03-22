package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the SLO API CRUD lifecycle.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SLO API E2E Tests")
class SloApiE2ETest extends E2ETestBase {

    private static final String SLO_NAME = "test-api-availability";

    @Test
    @Order(1)
    @DisplayName("GET /api/slos returns 200 with SLO list")
    void getSlos_shouldReturn200() {
        ResponseEntity<String> response = getString("/api/slos");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Response is a JSON array
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/slos returns 200 and creates SLO")
    void createSlo_shouldReturn200() {
        Map<String, Object> body = Map.of(
                "name", SLO_NAME,
                "description", "API should be 99.9% available",
                "sliType", "AVAILABILITY",
                "metric", "http_server_requests_seconds_count",
                "objective", 99.9,
                "goodCondition", "status < 500",
                "totalCondition", "status >= 0",
                "windowDays", 30
        );

        ResponseEntity<Map<String, Object>> response = postJson("/api/slos", body);

        assertThat(response.getStatusCode().value()).isIn(200, 201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("created");
        assertThat(response.getBody().get("name")).isEqualTo(SLO_NAME);
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/slos/test-api-availability returns 200 with name and objective")
    void getSlo_shouldReturn200WithDetails() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/slos/" + SLO_NAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("name")).isEqualTo(SLO_NAME);
        assertThat(response.getBody()).containsKey("objective");
        assertThat(((Number) response.getBody().get("objective")).doubleValue()).isEqualTo(99.9);
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/slos/evaluate returns 200 and evaluates all SLOs")
    void evaluateAll_shouldReturn200() {
        ResponseEntity<String> response = getString("/api/slos");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The evaluate endpoint returns a JSON array (List<SloDto>), not a Map
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> evalResponse = restTemplate.exchange(
                url("/api/slos/evaluate"),
                HttpMethod.POST,
                request,
                String.class
        );

        assertThat(evalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/slos/summary returns 200 with summary data")
    void getSummary_shouldReturn200WithSummary() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/slos/summary");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/slos/test-api-availability returns 200 with deleted status")
    void deleteSlo_shouldReturn200() {
        ResponseEntity<Void> response = delete("/api/slos/" + SLO_NAME);

        // SloApiController returns 200 with body on success, not 204
        assertThat(response.getStatusCode().value()).isIn(200, 204);
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/slos returns 200 with SLO removed after deletion")
    void getSlos_afterDelete_shouldNotContainDeletedSlo() {
        ResponseEntity<String> response = getString("/api/slos");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).doesNotContain("\"name\":\"" + SLO_NAME + "\"");
    }
}
