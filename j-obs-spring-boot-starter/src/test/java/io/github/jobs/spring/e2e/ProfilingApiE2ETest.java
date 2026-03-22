package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Profiling API endpoints.
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Profiling API E2E Tests")
class ProfilingApiE2ETest extends E2ETestBase {

    private static String cpuSessionId;

    @Test
    @Order(1)
    @DisplayName("GET /api/profiling/stats returns profiling statistics")
    void getProfilingStats() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/profiling/stats");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("totalSessions");
        assertThat(body).containsKey("activeSessions");
        assertThat(body).containsKey("completedSessions");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/profiling/sessions returns session list")
    void getProfilingSessions() {
        ResponseEntity<String> response = getString("/api/profiling/sessions");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/profiling/cpu/start starts CPU profiling session")
    void startCpuProfile() {
        ResponseEntity<Map<String, Object>> response = postJson(
                "/api/profiling/cpu/start?durationSeconds=5&intervalMs=100",
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("id");
        assertThat(body).containsKey("type");
        assertThat(body).containsKey("status");
        assertThat(body.get("type")).isEqualTo("CPU");

        cpuSessionId = (String) body.get("id");
        assertThat(cpuSessionId).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/profiling/cpu/running returns running CPU session")
    void getRunningCpuProfile() {
        assertThat(cpuSessionId).as("CPU session must have been started in previous test").isNotNull();

        ResponseEntity<Map<String, Object>> response = getJson("/api/profiling/cpu/running");

        // May be 200 (running) or 204 (already completed)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            assertThat(response.getBody()).containsKey("id");
            assertThat(response.getBody()).containsKey("status");
        }
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/profiling/cpu/{id}/stop stops CPU profiling session")
    void stopCpuProfile() {
        assertThat(cpuSessionId).as("CPU session must have been started in previous test").isNotNull();

        ResponseEntity<Map<String, Object>> response = postJson(
                "/api/profiling/cpu/" + cpuSessionId + "/stop",
                null
        );

        // May be 200 (stopped successfully) or 404 (already completed/stopped)
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            assertThat(response.getBody()).containsKey("id");
            assertThat(response.getBody().get("id")).isEqualTo(cpuSessionId);
        }
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/profiling/memory captures memory snapshot")
    void captureMemorySnapshot() {
        ResponseEntity<Map<String, Object>> response = postJson("/api/profiling/memory", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("heap");
        assertThat(body).containsKey("nonHeap");
        assertThat(body).containsKey("capturedAt");

        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) body.get("heap");
        assertThat(heap).containsKey("used");
        assertThat(heap).containsKey("max");
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/profiling/memory returns latest memory snapshot")
    void getLatestMemorySnapshot() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/profiling/memory");

        // 200 if snapshot exists, 204 if no snapshot captured yet
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            assertThat(response.getBody()).containsKey("heap");
            assertThat(response.getBody()).containsKey("nonHeap");
        }
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/profiling/threads captures thread dump")
    void captureThreadDump() {
        ResponseEntity<Map<String, Object>> response = postJson("/api/profiling/threads", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("threadCount");
        assertThat(body).containsKey("stateCounts");
        assertThat(body).containsKey("threads");
        assertThat(body).containsKey("capturedAt");
        assertThat((Integer) body.get("threadCount")).isGreaterThan(0);
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/profiling/threads returns latest thread dump")
    void getLatestThreadDump() {
        ResponseEntity<Map<String, Object>> response = getJson("/api/profiling/threads");

        // 200 if dump exists, 204 if none captured
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            assertThat(response.getBody()).containsKey("threadCount");
            assertThat(response.getBody()).containsKey("threads");
        }
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/profiling/sessions clears completed sessions")
    void clearCompletedSessions() {
        ResponseEntity<Void> response = delete("/api/profiling/sessions");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
