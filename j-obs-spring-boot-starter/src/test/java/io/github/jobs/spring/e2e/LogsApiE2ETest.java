package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Logs API endpoints.
 */
class LogsApiE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /api/logs should return 200 with logs array and pagination fields")
    void getLogs_returnsLogsWithPagination() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/logs");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("logs");
        assertThat(response.getBody().get("logs")).isInstanceOf(List.class);
        assertThat(response.getBody()).containsKey("total");
        assertThat(response.getBody()).containsKey("limit");
        assertThat(response.getBody()).containsKey("offset");
    }

    @Test
    @DisplayName("GET /api/logs?level=ERROR should return 200 with filtered logs")
    void getLogs_withLevelFilter_returnsFilteredLogs() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/logs?level=ERROR");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("logs");
        assertThat(response.getBody().get("logs")).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("GET /api/logs?limit=5 should return 200 and respect limit parameter")
    void getLogs_withLimit_respectsLimitParameter() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/logs?limit=5");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("logs");
        assertThat(response.getBody().get("logs")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Object> logs = (List<Object>) response.getBody().get("logs");
        assertThat(logs.size()).isLessThanOrEqualTo(5);

        assertThat(((Number) response.getBody().get("limit")).intValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /api/logs/stats should return 200 with level count fields")
    void getLogStats_returnsLevelCounts() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/logs/stats");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("totalEntries");
        assertThat(response.getBody()).containsKey("errorCount");
        assertThat(response.getBody()).containsKey("warnCount");
        assertThat(response.getBody()).containsKey("infoCount");
        assertThat(response.getBody()).containsKey("debugCount");
        assertThat(response.getBody()).containsKey("traceCount");
    }

    @Test
    @DisplayName("GET /api/logs/levels should return 200 with available log levels")
    void getLogLevels_returnsAvailableLevels() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/logs/levels"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("TRACE");
        assertThat(response.getBody()).contains("DEBUG");
        assertThat(response.getBody()).contains("INFO");
        assertThat(response.getBody()).contains("WARN");
        assertThat(response.getBody()).contains("ERROR");
    }
}
