package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the SQL Analyzer API endpoints.
 */
@DisplayName("SQL Analyzer API E2E Tests")
class SqlAnalyzerApiE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /api/sql/analyze returns analysis result")
    void analyzeWithWindow() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/sql/analyze?window=1h");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("issues");
        assertThat(body).containsKey("total");
        assertThat(body).containsKey("criticalCount");
        assertThat(body).containsKey("warningCount");
    }

    @Test
    @DisplayName("GET /api/sql/issues returns issues list")
    void getIssues() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/sql/issues");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("issues");
        assertThat(body).containsKey("total");
    }

    @Test
    @DisplayName("GET /api/sql/slow-queries returns slow queries list")
    void getSlowQueries() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/sql/slow-queries?threshold=1s&window=1h");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("queries");
        assertThat(body).containsKey("total");
        assertThat(body).containsKey("thresholdMs");
    }

    @Test
    @DisplayName("GET /api/sql/top-queries returns top queries")
    void getTopQueries() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/sql/top-queries?limit=10&window=1h");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("queries");
        assertThat(body).containsKey("total");
    }

    @Test
    @DisplayName("GET /api/sql/stats returns SQL analysis statistics")
    void getStats() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/sql/stats");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("totalQueries");
        assertThat(body).containsKey("totalIssues");
        assertThat(body).containsKey("criticalIssues");
        assertThat(body).containsKey("warningIssues");
        assertThat(body).containsKey("nPlusOneDetected");
        assertThat(body).containsKey("slowQueriesDetected");
        assertThat(body).containsKey("avgQueryDurationMs");
    }
}
