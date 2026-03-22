package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Traces API endpoints.
 */
class TracesApiE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /api/traces should return 200 with traces array")
    void getTraces_returnsTracesArray() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/traces");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("traces");
        assertThat(response.getBody().get("traces")).isInstanceOf(List.class);
        assertThat(response.getBody()).containsKey("total");
        assertThat(response.getBody()).containsKey("limit");
        assertThat(response.getBody()).containsKey("offset");
    }

    @Test
    @DisplayName("GET /api/traces?limit=5 should return 200 and respect limit")
    void getTraces_withLimit_respectsLimitParameter() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/traces?limit=5");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("traces");

        @SuppressWarnings("unchecked")
        List<Object> traces = (List<Object>) response.getBody().get("traces");
        assertThat(traces.size()).isLessThanOrEqualTo(5);

        assertThat(((Number) response.getBody().get("limit")).intValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/traces/{nonexistent} should return 404")
    void getTraceById_nonexistent_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/traces/nonexistent-trace-id"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /api/traces/stats should return 200 with statistics fields")
    void getTraceStats_returnsStatistics() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/traces/stats");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("totalTraces");
        assertThat(response.getBody()).containsKey("totalSpans");
        assertThat(response.getBody()).containsKey("errorTraces");
        assertThat(response.getBody()).containsKey("avgDurationMs");
    }
}
