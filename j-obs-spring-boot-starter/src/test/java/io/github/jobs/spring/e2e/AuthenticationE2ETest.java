package io.github.jobs.spring.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for J-Obs authentication mechanisms.
 * Tests Basic Auth, API Key auth, and public path exemptions.
 */
@SpringBootTest(
        classes = E2ETestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "j-obs.enabled=true",
        "j-obs.path=/j-obs",
        "j-obs.security.enabled=true",
        "j-obs.security.type=both",
        "j-obs.security.users[0].username=admin",
        "j-obs.security.users[0].password=secret123",
        "j-obs.security.api-keys[0]=test-api-key-12345",
        "j-obs.security.api-key-header=X-API-Key",
        "j-obs.rate-limiting.enabled=false",
        "spring.main.allow-bean-definition-overriding=true",
        "j-obs.persistence.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
class AuthenticationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/j-obs" + path;
    }

    @Test
    @DisplayName("Unauthenticated access to API endpoint returns 401")
    void unauthenticatedAccessReturns401ForApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/status"),
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Basic auth with valid credentials succeeds")
    void basicAuthWithValidCredentialsSucceeds() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = Base64.getEncoder()
                .encodeToString("admin:secret123".getBytes());
        headers.set("Authorization", "Basic " + credentials);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/status"),
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Basic auth with wrong password returns 401")
    void basicAuthWithWrongPasswordFails() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = Base64.getEncoder()
                .encodeToString("admin:wrongpassword".getBytes());
        headers.set("Authorization", "Basic " + credentials);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/status"),
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("API key auth with valid key succeeds")
    void apiKeyAuthWithValidKeySucceeds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "test-api-key-12345");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/status"),
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("API key auth with wrong key returns 401")
    void apiKeyAuthWithWrongKeyFails() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "invalid-key-99999");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/status"),
                HttpMethod.GET,
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Login page is accessible without authentication")
    void loginPageAccessibleWithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/login"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Static resources are exempt from authentication")
    void staticResourcesExemptFromAuth() {
        // Static asset paths under /j-obs/static/ should be exempt.
        // Even if the resource does not exist, the response should not be 401
        // (it would be 404 or 200, but never 401).
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/static/css/main.css"),
                String.class
        );

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
