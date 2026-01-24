package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC interceptor that handles authentication for J-Obs dashboard endpoints.
 * Supports Basic Authentication and API Key authentication.
 */
public class JObsAuthenticationFilter implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JObsAuthenticationFilter.class);
    private static final String SESSION_ATTR_AUTHENTICATED = "j-obs-authenticated";
    private static final String SESSION_ATTR_USERNAME = "j-obs-username";

    private final JObsProperties.Security securityConfig;
    private final String pathPrefix;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Cache for validated API keys (key -> timestamp)
    private final Map<String, Long> validatedApiKeys = new ConcurrentHashMap<>();

    public JObsAuthenticationFilter(JObsProperties.Security securityConfig, String pathPrefix) {
        this.securityConfig = securityConfig;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();

        // Handle login endpoint first (before exempt check to handle POST)
        if (isLoginRequest(request, path)) {
            return handleLoginRequest(request, response);
        }

        // Handle logout endpoint
        if (isLogoutRequest(request, path)) {
            return handleLogoutRequest(request, response);
        }

        // Check if path is exempt from authentication
        if (isExemptPath(path)) {
            return true;
        }

        // Check session authentication first
        if (isSessionAuthenticated(request)) {
            return true;
        }

        // Check API key authentication
        if (isApiKeyAuthenticated(request)) {
            return true;
        }

        // Check Basic Auth header
        if (isBasicAuthAuthenticated(request)) {
            return true;
        }

        // Authentication required - redirect to login for browser requests, 401 for API
        return handleUnauthenticated(request, response);
    }

    private boolean isExemptPath(String path) {
        String relativePath = path.substring(pathPrefix.length());
        List<String> exemptPaths = securityConfig.getExemptPaths();

        // Always exempt login page
        if (relativePath.equals("/login") || relativePath.equals("/login.html")) {
            return true;
        }

        for (String exemptPattern : exemptPaths) {
            if (pathMatcher.match(exemptPattern, relativePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoginRequest(HttpServletRequest request, String path) {
        return "POST".equalsIgnoreCase(request.getMethod()) &&
               (path.equals(pathPrefix + "/login") || path.equals(pathPrefix + "/api/login"));
    }

    private boolean isLogoutRequest(HttpServletRequest request, String path) {
        return path.equals(pathPrefix + "/logout") || path.equals(pathPrefix + "/api/logout");
    }

    private boolean handleLoginRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null) {
            // Try JSON body
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                // For API login, credentials should be in Authorization header
                return handleUnauthenticated(request, response);
            }
        }

        if (validateCredentials(username, password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_ATTR_AUTHENTICATED, true);
            session.setAttribute(SESSION_ATTR_USERNAME, username);
            session.setMaxInactiveInterval((int) securityConfig.getSessionTimeout().toSeconds());

            log.info("User '{}' logged in to J-Obs dashboard", username);

            // Redirect to dashboard (validate to prevent open redirect)
            String redirectUrl = sanitizeRedirectUrl(request.getParameter("redirect"));
            response.sendRedirect(redirectUrl);
            return false;
        }

        // Login failed - redirect back to login with error
        log.warn("Failed login attempt for user '{}' from {}", username, request.getRemoteAddr());
        response.sendRedirect(pathPrefix + "/login?error=true");
        return false;
    }

    private boolean handleLogoutRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute(SESSION_ATTR_USERNAME);
            session.invalidate();
            log.info("User '{}' logged out from J-Obs dashboard", username);
        }

        // For API logout, return 200 OK
        if (request.getRequestURI().contains("/api/")) {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Logged out successfully\"}");
        } else {
            // Redirect to login page
            response.sendRedirect(pathPrefix + "/login?logout=true");
        }
        return false;
    }

    private boolean isSessionAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Boolean authenticated = (Boolean) session.getAttribute(SESSION_ATTR_AUTHENTICATED);
            return Boolean.TRUE.equals(authenticated);
        }
        return false;
    }

    private boolean isApiKeyAuthenticated(HttpServletRequest request) {
        String authType = securityConfig.getType().toLowerCase();
        if (!"api-key".equals(authType) && !"both".equals(authType)) {
            return false;
        }

        String apiKeyHeader = securityConfig.getApiKeyHeader();
        String apiKey = request.getHeader(apiKeyHeader);

        if (apiKey == null || apiKey.isBlank()) {
            // Also check query parameter for convenience
            apiKey = request.getParameter("api_key");
        }

        if (apiKey != null && validateApiKey(apiKey)) {
            return true;
        }

        return false;
    }

    private boolean isBasicAuthAuthenticated(HttpServletRequest request) {
        String authType = securityConfig.getType().toLowerCase();
        if (!"basic".equals(authType) && !"both".equals(authType)) {
            return false;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
            return false;
        }

        try {
            String base64Credentials = authHeader.substring(6);
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);

            int colonIndex = credentials.indexOf(':');
            if (colonIndex == -1) {
                return false;
            }

            String username = credentials.substring(0, colonIndex);
            String password = credentials.substring(colonIndex + 1);

            if (validateCredentials(username, password)) {
                // Create session for browser requests
                String acceptHeader = request.getHeader("Accept");
                if (acceptHeader != null && acceptHeader.contains("text/html")) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute(SESSION_ATTR_AUTHENTICATED, true);
                    session.setAttribute(SESSION_ATTR_USERNAME, username);
                    session.setMaxInactiveInterval((int) securityConfig.getSessionTimeout().toSeconds());
                }
                return true;
            }
        } catch (IllegalArgumentException e) {
            log.debug("Invalid Base64 in Authorization header", e);
        }

        return false;
    }

    private boolean validateCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        List<JObsProperties.Security.User> users = securityConfig.getUsers();
        if (users == null || users.isEmpty()) {
            return false;
        }

        for (JObsProperties.Security.User user : users) {
            if (username.equals(user.getUsername()) && password.equals(user.getPassword())) {
                return true;
            }
        }

        return false;
    }

    private boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        List<String> validKeys = securityConfig.getApiKeys();
        if (validKeys == null || validKeys.isEmpty()) {
            return false;
        }

        // Check cache first for performance
        Long cachedTime = validatedApiKeys.get(apiKey);
        if (cachedTime != null && System.currentTimeMillis() - cachedTime < 60000) {
            return true;
        }

        for (String validKey : validKeys) {
            // Use constant-time comparison to prevent timing attacks
            if (secureCompare(apiKey, validKey)) {
                validatedApiKeys.put(apiKey, System.currentTimeMillis());
                return true;
            }
        }

        return false;
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    /**
     * Validates redirect URL to prevent open redirect attacks.
     * Only allows paths that start with the configured J-Obs path prefix.
     */
    private String sanitizeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return pathPrefix;
        }
        // Block protocol-relative URLs and absolute URLs to external hosts
        if (redirectUrl.contains("://") || redirectUrl.startsWith("//")) {
            return pathPrefix;
        }
        // Must start with the J-Obs path prefix
        if (!redirectUrl.startsWith(pathPrefix)) {
            return pathPrefix;
        }
        // Block newline injection (HTTP response splitting)
        if (redirectUrl.contains("\r") || redirectUrl.contains("\n")) {
            return pathPrefix;
        }
        return redirectUrl;
    }

    private boolean handleUnauthenticated(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String acceptHeader = request.getHeader("Accept");
        boolean isApiRequest = request.getRequestURI().contains("/api/");
        boolean isBrowserRequest = acceptHeader != null && acceptHeader.contains("text/html") && !isApiRequest;

        if (isBrowserRequest) {
            // Redirect to login page
            String currentPath = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null) {
                currentPath += "?" + queryString;
            }
            response.sendRedirect(pathPrefix + "/login?redirect=" + java.net.URLEncoder.encode(currentPath, StandardCharsets.UTF_8));
        } else {
            // Return 401 for API requests
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate", "Basic realm=\"J-Obs Dashboard\"");
            response.getWriter().write("{\"error\":\"Authentication required\",\"message\":\"Please provide valid credentials\"}");
        }

        return false;
    }
}
