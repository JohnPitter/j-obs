package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for requirements, status, and capabilities API endpoints.
 */
class RequirementsApiE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /api/requirements should return 200 with status and dependencies")
    void getRequirements_returnsStatusAndDependencies() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/requirements");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status").toString())
                .isIn("COMPLETE", "INCOMPLETE");
        assertThat(response.getBody()).containsKey("dependencies");
        assertThat(response.getBody().get("dependencies")).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("POST /api/requirements/refresh should return 200 with refreshed status")
    void refreshRequirements_returnsStatus() {
        ResponseEntity<Map<String, Object>> response = postJson("/api/requirements/refresh", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status").toString())
                .isIn("COMPLETE", "INCOMPLETE");
    }

    @Test
    @DisplayName("GET /api/status should return 200 with status field")
    void getStatus_returnsStatus() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/status");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status").toString())
                .isIn("UP", "DEGRADED");
    }

    @Test
    @DisplayName("GET /api/capabilities should return 200 with capability flags")
    void getCapabilities_returnsCapabilityFlags() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/capabilities");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("websocketEnabled");
        assertThat(response.getBody()).containsKey("logsEnabled");
        assertThat(response.getBody()).containsKey("tracesEnabled");
        assertThat(response.getBody()).containsKey("metricsEnabled");
    }
}
