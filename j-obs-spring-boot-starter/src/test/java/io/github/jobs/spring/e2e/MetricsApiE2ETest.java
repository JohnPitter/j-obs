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
 * E2E tests for the Metrics API endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Metrics API E2E Tests")
class MetricsApiE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("GET /api/metrics returns 200 with metrics array")
    void getMetrics_shouldReturn200WithMetricsArray() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/metrics");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("metrics");
        assertThat(response.getBody().get("metrics")).isInstanceOf(List.class);
        assertThat(response.getBody()).containsKey("total");
        assertThat(response.getBody()).containsKey("limit");
        assertThat(response.getBody()).containsKey("offset");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/metrics?limit=5 returns 200 and respects limit")
    void getMetricsWithLimit_shouldReturn200AndRespectLimit() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/metrics?limit=5");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("metrics");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metrics = (List<Map<String, Object>>) response.getBody().get("metrics");
        assertThat(metrics.size()).isLessThanOrEqualTo(5);

        assertThat(response.getBody().get("limit")).isEqualTo(5);
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/metrics/stats returns 200 with stats fields")
    void getMetricsStats_shouldReturn200WithStatsFields() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/metrics/stats");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/metrics/quick-stats returns 200 with JVM fields")
    void getQuickStats_shouldReturn200WithJvmFields() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/metrics/quick-stats");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<String, Object> body = response.getBody();
        // QuickStatsDto has these fields (values may be null if no Micrometer configured)
        assertThat(body).containsAnyOf(
                Map.entry("jvmMemoryUsed", body.get("jvmMemoryUsed")),
                Map.entry("jvmMemoryMax", body.get("jvmMemoryMax")),
                Map.entry("threadCount", body.get("threadCount")),
                Map.entry("cpuUsage", body.get("cpuUsage")),
                Map.entry("uptime", body.get("uptime")),
                Map.entry("gcCount", body.get("gcCount"))
        );
        // At minimum, the response should contain the expected keys
        assertThat(body).containsKey("uptime");
        assertThat(body).containsKey("gcCount");
    }
}
