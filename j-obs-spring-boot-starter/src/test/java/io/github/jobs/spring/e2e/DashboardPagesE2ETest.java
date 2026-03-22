package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for J-Obs HTML pages.
 * Verifies that all dashboard pages are accessible and return expected HTML content.
 */
class DashboardPagesE2ETest extends E2ETestBase {

    @Test
    @DisplayName("GET /j-obs should return 200 with HTML containing J-Obs")
    void rootPage_returnsHtmlWithJObs() {
        ResponseEntity<String> response = getString("");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toLowerCase()).containsAnyOf("j-obs", "j-obs");
    }

    @Test
    @DisplayName("GET /j-obs/dashboard should return 200 with HTML content")
    void dashboardPage_returnsHtml() {
        ResponseEntity<String> response = getString("/dashboard");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/requirements should return 200 with HTML content")
    void requirementsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/requirements");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/login should return 200 with login HTML")
    void loginPage_returnsHtmlWithLoginContent() {
        ResponseEntity<String> response = getString("/login");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().toLowerCase()).contains("login");
    }

    @Test
    @DisplayName("GET /j-obs/api-docs should return 200 with HTML content")
    void apiDocsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/api-docs");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/logs should return 200 with HTML content")
    void logsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/logs");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/traces should return 200 with HTML content")
    void tracesPage_returnsHtml() {
        ResponseEntity<String> response = getString("/traces");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/health should return 200 with HTML content")
    void healthPage_returnsHtml() {
        ResponseEntity<String> response = getString("/health");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/metrics should return 200 with HTML content")
    void metricsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/metrics");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/alerts should return 200 with HTML content")
    void alertsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/alerts");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/sql should return 200 with HTML content")
    void sqlAnalyzerPage_returnsHtml() {
        ResponseEntity<String> response = getString("/sql");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/service-map should return 200 with HTML content")
    void serviceMapPage_returnsHtml() {
        ResponseEntity<String> response = getString("/service-map");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/anomalies should return 200 with HTML content")
    void anomaliesPage_returnsHtml() {
        ResponseEntity<String> response = getString("/anomalies");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }

    @Test
    @DisplayName("GET /j-obs/tools should return 200 with HTML content")
    void toolsPage_returnsHtml() {
        ResponseEntity<String> response = getString("/tools");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsIgnoringCase("html");
    }
}
