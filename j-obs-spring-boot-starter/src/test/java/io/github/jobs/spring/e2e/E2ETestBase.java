package io.github.jobs.spring.e2e;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

/**
 * Base class for E2E tests.
 * Boots full Spring context with J-Obs enabled and security/rate-limiting disabled.
 */
@SpringBootTest(
        classes = E2ETestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "j-obs.enabled=true",
        "j-obs.path=/j-obs",
        "j-obs.security.enabled=false",
        "j-obs.rate-limiting.enabled=false",
        "j-obs.logs.enabled=true",
        "j-obs.traces.enabled=true",
        "j-obs.metrics.enabled=true",
        "j-obs.health.enabled=true",
        "j-obs.alerts.enabled=true",
        "j-obs.profiling.enabled=true",
        "j-obs.sql-analyzer.enabled=true",
        "j-obs.anomaly-detection.enabled=true",
        "j-obs.service-map.enabled=true",
        "j-obs.open-api.enabled=false",
        "management.endpoints.web.exposure.include=health,info,metrics",
        "spring.main.allow-bean-definition-overriding=true"
})
abstract class E2ETestBase {

    private static final String BASE_PATH = "/j-obs";

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Builds full URL for a J-Obs path.
     */
    protected String url(String path) {
        return "http://localhost:" + port + BASE_PATH + path;
    }

    /**
     * Builds full URL for an arbitrary path (no /j-obs prefix).
     */
    protected String rawUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * GET request returning JSON as Map.
     */
    protected ResponseEntity<Map<String, Object>> getJson(String path) {
        return restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    /**
     * GET request returning raw String (for HTML pages).
     */
    protected ResponseEntity<String> getString(String path) {
        return restTemplate.getForEntity(url(path), String.class);
    }

    /**
     * POST request with JSON body.
     */
    protected ResponseEntity<Map<String, Object>> postJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    /**
     * PUT request with JSON body.
     */
    protected ResponseEntity<Map<String, Object>> putJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                url(path),
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    /**
     * PATCH request with JSON body.
     */
    protected ResponseEntity<Map<String, Object>> patchJson(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                url(path),
                HttpMethod.PATCH,
                request,
                new ParameterizedTypeReference<>() {}
        );
    }

    /**
     * DELETE request.
     */
    protected ResponseEntity<Void> delete(String path) {
        return restTemplate.exchange(
                url(path),
                HttpMethod.DELETE,
                null,
                Void.class
        );
    }

    /**
     * GET request with custom headers (e.g., for API key auth).
     */
    protected ResponseEntity<String> getWithHeaders(String path, HttpHeaders headers) {
        HttpEntity<Void> request = new HttpEntity<>(headers);
        return restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                request,
                String.class
        );
    }
}
