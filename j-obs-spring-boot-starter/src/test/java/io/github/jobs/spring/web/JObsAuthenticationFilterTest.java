package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JObsAuthenticationFilterTest {

    private JObsProperties.Security securityConfig;
    private JObsAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        securityConfig = new JObsProperties.Security();
        securityConfig.setEnabled(true);
        securityConfig.setType("basic");
        securityConfig.setSessionTimeout(Duration.ofHours(8));

        JObsProperties.Security.User user = new JObsProperties.Security.User();
        user.setUsername("admin");
        user.setPassword("secret");
        user.setRole("ADMIN");
        securityConfig.setUsers(List.of(user));

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldAllowExemptPaths() throws Exception {
        request.setRequestURI("/j-obs/login");

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    void shouldAllowStaticResources() throws Exception {
        securityConfig.setExemptPaths(List.of("/static/**"));
        request.setRequestURI("/j-obs/static/js/app.js");

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    void shouldAuthenticateWithBasicAuth() throws Exception {
        securityConfig.setType("basic");
        request.setRequestURI("/j-obs");
        String credentials = Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    void shouldRejectInvalidBasicAuth() throws Exception {
        securityConfig.setType("basic");
        request.setRequestURI("/j-obs/api/logs");
        String credentials = Base64.getEncoder().encodeToString("admin:wrong".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAuthenticateWithApiKey() throws Exception {
        securityConfig.setType("api-key");
        securityConfig.setApiKeys(List.of("my-secret-key"));
        securityConfig.setApiKeyHeader("X-API-Key");

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("X-API-Key", "my-secret-key");

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    void shouldRejectInvalidApiKey() throws Exception {
        securityConfig.setType("api-key");
        securityConfig.setApiKeys(List.of("my-secret-key"));
        securityConfig.setApiKeyHeader("X-API-Key");

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("X-API-Key", "invalid-key");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldRejectApiKeyQueryParam() throws Exception {
        // API keys via query parameters are rejected for security reasons:
        // - Credentials exposed in server logs
        // - Credentials exposed in browser history
        // - Credentials leaked via Referer header
        securityConfig.setType("api-key");
        securityConfig.setApiKeys(List.of("my-secret-key"));

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        request.setRequestURI("/j-obs/api/traces");
        request.setParameter("api_key", "my-secret-key");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result); // Query param API keys are no longer accepted
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAuthenticateWithBothMethods() throws Exception {
        securityConfig.setType("both");
        securityConfig.setApiKeys(List.of("my-secret-key"));

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        // First try with API key
        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("X-API-Key", "my-secret-key");

        assertTrue(filter.preHandle(request, response, null));

        // Reset for Basic Auth test
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/j-obs/api/logs");
        String credentials = Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        assertTrue(filter.preHandle(request, response, null));
    }

    @Test
    void shouldHandleLoginSuccess() throws Exception {
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "secret");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result); // Returns false because it handles the response itself
        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().contains("/j-obs"));

        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertTrue((Boolean) session.getAttribute("j-obs-authenticated"));
        assertEquals("admin", session.getAttribute("j-obs-username"));
    }

    @Test
    void shouldHandleLoginFailure() throws Exception {
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "wrong");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().contains("/login?error=true"));
    }

    @Test
    void shouldHandleLogout() throws Exception {
        // First login
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "secret");
        filter.preHandle(request, response, null);

        HttpSession session = request.getSession();
        assertNotNull(session);

        // Now logout
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setSession(session);
        request.setRequestURI("/j-obs/logout");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertTrue(response.getRedirectedUrl().contains("/login?logout=true"));
    }

    @Test
    void shouldHandleApiLogout() throws Exception {
        // First login
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "secret");
        filter.preHandle(request, response, null);

        HttpSession session = request.getSession();

        // Now API logout
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setSession(session);
        request.setRequestURI("/j-obs/api/logout");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("Logged out successfully"));
    }

    @Test
    void shouldUseSessionAuthentication() throws Exception {
        // Login first
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "secret");
        filter.preHandle(request, response, null);

        HttpSession session = request.getSession();

        // Access protected endpoint
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setSession(session);
        request.setRequestURI("/j-obs");

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    void shouldRedirectBrowserToLogin() throws Exception {
        request.setRequestURI("/j-obs");
        request.addHeader("Accept", "text/html,application/xhtml+xml");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl().contains("/j-obs/login?redirect="));
    }

    @Test
    void shouldReturn401ForApiRequests() throws Exception {
        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("Accept", "application/json");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Authentication required"));
    }

    @Test
    void shouldHandleLoginWithRedirect() throws Exception {
        request.setMethod("POST");
        request.setRequestURI("/j-obs/login");
        request.setParameter("username", "admin");
        request.setParameter("password", "secret");
        request.setParameter("redirect", "/j-obs/traces");

        filter.preHandle(request, response, null);

        assertEquals(302, response.getStatus());
        assertEquals("/j-obs/traces", response.getRedirectedUrl());
    }

    @Test
    void shouldIgnoreApiKeyWhenTypeIsBasicOnly() throws Exception {
        securityConfig.setType("basic");
        securityConfig.setApiKeys(List.of("my-secret-key"));

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("X-API-Key", "my-secret-key");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result); // API key ignored when type is "basic"
    }

    @Test
    void shouldIgnoreBasicAuthWhenTypeIsApiKeyOnly() throws Exception {
        securityConfig.setType("api-key");
        securityConfig.setApiKeys(List.of("my-secret-key"));

        filter = new JObsAuthenticationFilter(securityConfig, "/j-obs");

        request.setRequestURI("/j-obs/api/traces");
        String credentials = Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result); // Basic auth ignored when type is "api-key"
    }

    @Test
    void shouldCreateSessionForBrowserWithBasicAuth() throws Exception {
        securityConfig.setType("basic");
        request.setRequestURI("/j-obs");
        request.addHeader("Accept", "text/html");
        String credentials = Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        boolean result = filter.preHandle(request, response, null);

        assertTrue(result);
        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertTrue((Boolean) session.getAttribute("j-obs-authenticated"));
    }

    @Test
    void shouldHandleInvalidBase64InBasicAuth() throws Exception {
        securityConfig.setType("basic");
        request.setRequestURI("/j-obs/api/traces");
        request.addHeader("Authorization", "Basic not-valid-base64!!!");

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldHandleMalformedCredentialsInBasicAuth() throws Exception {
        securityConfig.setType("basic");
        request.setRequestURI("/j-obs/api/traces");
        // Credentials without colon
        String credentials = Base64.getEncoder().encodeToString("adminwithnocolon".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);

        boolean result = filter.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(401, response.getStatus());
    }
}
