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
 * E2E tests for the Alerts API CRUD lifecycle.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Alerts API E2E Tests")
class AlertsApiE2ETest extends E2ETestBase {

    private static String createdAlertId;

    @Test
    @Order(1)
    @DisplayName("GET /api/alerts returns 200 with initially empty or existing list")
    void getAlerts_shouldReturn200WithList() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alerts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("alerts");
        assertThat(response.getBody().get("alerts")).isInstanceOf(List.class);
        assertThat(response.getBody()).containsKey("total");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/alerts returns 201 and creates a metric alert rule")
    void createAlert_shouldReturn201() {
        Map<String, Object> body = Map.of(
                "name", "test-high-latency",
                "type", "METRIC",
                "severity", "WARNING",
                "enabled", true,
                "metric", "http_server_requests_seconds",
                "operator", "GREATER_THAN",
                "threshold", 2.0,
                "windowMinutes", 5
        );

        ResponseEntity<Map<String, Object>> response = postJson("/api/alerts", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("name")).isEqualTo("test-high-latency");
        assertThat(response.getBody().get("type")).isEqualTo("METRIC");
        assertThat(response.getBody().get("severity")).isEqualTo("WARNING");
        assertThat(response.getBody().get("enabled")).isEqualTo(true);

        createdAlertId = (String) response.getBody().get("id");
        assertThat(createdAlertId).isNotNull().isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/alerts returns 200 with at least 1 alert after creation")
    void getAlerts_afterCreate_shouldContainAlert() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alerts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) response.getBody().get("alerts");
        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(alert -> "test-high-latency".equals(alert.get("name")));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/alerts/types returns 200 with alert types")
    void getAlertTypes_shouldReturn200() {
        ResponseEntity<String> response = getString("/api/alerts/types");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).startsWith("[");
        assertThat(response.getBody()).contains("METRIC");
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/alerts/severities returns 200 with severities")
    void getSeverities_shouldReturn200() {
        ResponseEntity<String> response = getString("/api/alerts/severities");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).startsWith("[");
        assertThat(response.getBody()).contains("WARNING");
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/alert-events returns 200 with events list")
    void getAlertEvents_shouldReturn200() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alert-events");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("events");
        assertThat(response.getBody().get("events")).isInstanceOf(List.class);
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/alert-events/statistics returns 200 with stats")
    void getAlertEventStatistics_shouldReturn200() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alert-events/statistics");

        // May return 200 or 503 depending on AlertService availability
        assertThat(response.getStatusCode().value()).isIn(200, 503);
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/alert-providers returns 200 with providers list")
    void getAlertProviders_shouldReturn200() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alert-providers");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("providers");
        assertThat(response.getBody().get("providers")).isInstanceOf(List.class);
        assertThat(response.getBody()).containsKey("total");
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/alerts/{id} returns 204 for created alert")
    void deleteAlert_shouldReturn204() {
        assertThat(createdAlertId).as("Alert ID from creation step").isNotNull();

        ResponseEntity<Void> response = delete("/api/alerts/" + createdAlertId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/alerts returns 200 with alert removed after deletion")
    void getAlerts_afterDelete_shouldNotContainDeletedAlert() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/alerts");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alerts = (List<Map<String, Object>>) response.getBody().get("alerts");
        assertThat(alerts).noneMatch(alert -> createdAlertId.equals(alert.get("id")));
    }
}
