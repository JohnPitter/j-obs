package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import io.github.jobs.spring.security.AuditLogger;
import io.github.jobs.spring.security.ConstantTimeUtils;
import io.github.jobs.spring.security.LoginRateLimiter;
import io.github.jobs.spring.security.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring MVC interceptor that handles authentication for J-Obs dashboard endpoints.
 * Supports Basic Authentication and API Key authentication.
 * <p>
 * Implements {@link DisposableBean} to ensure proper cleanup of the rate limiter's
 * background thread on application shutdown.
 */
public class JObsAuthenticationFilter implements HandlerInterceptor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(JObsAuthenticationFilter.class);
    private static final String SESSION_ATTR_AUTHENTICATED = "j-obs-authenticated";
    private static final String SESSION_ATTR_USERNAME = "j-obs-username";
    private static final String SESSION_ATTR_CSRF_TOKEN = "j-obs-csrf-token";
    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String CSRF_PARAM = "_csrf";
    private static final int CSRF_TOKEN_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JObsProperties.Security securityConfig;
    private final String pathPrefix;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final LoginRateLimiter loginRateLimiter;

    // Cache for validated API keys (key -> timestamp)
    private final Map<String, Long> validatedApiKeys = new ConcurrentHashMap<>();

    public JObsAuthenticationFilter(JObsProperties.Security securityConfig, String pathPrefix) {
        this(securityConfig, pathPrefix, new LoginRateLimiter());
    }

    public JObsAuthenticationFilter(JObsProperties.Security securityConfig, String pathPrefix,
                                     LoginRateLimiter loginRateLimiter) {
        this.securityConfig = securityConfig;
        this.pathPrefix = pathPrefix;
        this.loginRateLimiter = loginRateLimiter;
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
        String clientIp = getClientIp(request);

        // Check rate limiting BEFORE processing credentials
        if (!loginRateLimiter.isAllowed(clientIp)) {
            long lockoutSeconds = loginRateLimiter.getLockoutRemaining(clientIp).toSeconds();
            log.warn("Login attempt blocked for IP {} - rate limited for {} more seconds", clientIp, lockoutSeconds);
            AuditLogger.logLoginLocked(clientIp, lockoutSeconds);

            if (isApiRequest(request)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(lockoutSeconds));
                response.getWriter().write(
                        "{\"error\":\"Too many login attempts\",\"retryAfter\":" + lockoutSeconds + "}"
                );
            } else {
                response.sendRedirect(pathPrefix + "/login?error=locked&retry=" + lockoutSeconds);
            }
            return false;
        }

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

        // Validate CSRF token for form submissions
        if (!validateCsrfToken(request)) {
            log.warn("CSRF token validation failed for login attempt from {}", clientIp);
            AuditLogger.logCsrfFailure(clientIp, request.getRequestURI());
            response.sendRedirect(pathPrefix + "/login?error=csrf");
            return false;
        }

        if (validateCredentials(username, password)) {
            // Successful login - clear rate limiting for this IP
            loginRateLimiter.recordSuccessfulLogin(clientIp);

            // Session fixation prevention: invalidate old session and create new one
            HttpSession oldSession = request.getSession(false);
            String redirectUrl = sanitizeRedirectUrl(request.getParameter("redirect"));

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(SESSION_ATTR_AUTHENTICATED, true);
            newSession.setAttribute(SESSION_ATTR_USERNAME, username);
            newSession.setMaxInactiveInterval((int) securityConfig.getSessionTimeout().toSeconds());

            // Generate new CSRF token for the authenticated session
            newSession.setAttribute(SESSION_ATTR_CSRF_TOKEN, generateCsrfToken());

            log.info("User '{}' logged in to J-Obs dashboard from {}", username, clientIp);
            AuditLogger.logLoginSuccess(username, clientIp);

            response.sendRedirect(redirectUrl);
            return false;
        }

        // Login failed - record for rate limiting
        loginRateLimiter.recordFailedAttempt(clientIp);
        int remaining = loginRateLimiter.getRemainingAttempts(clientIp);

        log.warn("Failed login attempt for user '{}' from {} ({} attempts remaining)",
                username, clientIp, remaining);
        AuditLogger.logLoginFailure(username, clientIp, remaining);

        if (remaining == 0) {
            long lockoutSeconds = loginRateLimiter.getLockoutRemaining(clientIp).toSeconds();
            response.sendRedirect(pathPrefix + "/login?error=locked&retry=" + lockoutSeconds);
        } else {
            response.sendRedirect(pathPrefix + "/login?error=true&remaining=" + remaining);
        }
        return false;
    }

    /**
     * Extracts the client IP address, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header first (when behind proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client)
            String ip = xForwardedFor.split(",")[0].trim();
            if (!ip.isBlank()) {
                return ip;
            }
        }

        // Check X-Real-IP header (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Checks if the request is an API request (vs browser request).
     */
    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().contains("/api/") ||
               "application/json".equals(request.getContentType());
    }

    private boolean handleLogoutRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        String clientIp = getClientIp(request);
        if (session != null) {
            String username = (String) session.getAttribute(SESSION_ATTR_USERNAME);
            session.invalidate();
            log.info("User '{}' logged out from J-Obs dashboard", username);
            AuditLogger.logLogout(username, clientIp);
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

        // Security: API keys in query parameters are no longer supported
        // They expose credentials in server logs, browser history, and referrer headers
        if (apiKey == null || apiKey.isBlank()) {
            String queryApiKey = request.getParameter("api_key");
            if (queryApiKey != null && !queryApiKey.isBlank()) {
                log.warn("API key provided via query parameter from {}. " +
                         "This is insecure and no longer supported. " +
                         "Use the {} header instead.",
                         request.getRemoteAddr(), apiKeyHeader);
                AuditLogger.logApiKeyInsecure(request.getRemoteAddr());
                return false;
            }
        }

        if (apiKey != null && validateApiKey(apiKey)) {
            AuditLogger.logApiKeyUsed(apiKey, request.getRemoteAddr());
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
            // Constant-time username comparison
            boolean usernameMatches = secureCompare(username, user.getUsername());
            // Use PasswordEncoder for password verification (supports both hashed and plaintext)
            boolean passwordMatches = PasswordEncoder.matches(password, user.getPassword());

            if (usernameMatches && passwordMatches) {
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
     * Uses {@link ConstantTimeUtils#secureEquals} which doesn't leak length information.
     */
    private boolean secureCompare(String a, String b) {
        return ConstantTimeUtils.secureEquals(a, b);
    }

    /**
     * Generates a cryptographically secure CSRF token.
     */
    private String generateCsrfToken() {
        byte[] tokenBytes = new byte[CSRF_TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validates CSRF token for state-changing requests.
     * Token must be present in either header (X-CSRF-Token) or form parameter (_csrf).
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        // If no session exists yet (first login), generate token for the login page
        if (session == null) {
            // First login attempt - CSRF token should have been generated on login page view
            // For backward compatibility, allow first login without CSRF
            return true;
        }

        String expectedToken = (String) session.getAttribute(SESSION_ATTR_CSRF_TOKEN);
        if (expectedToken == null) {
            // No token in session yet - likely first login, allow it
            return true;
        }

        // Get token from request (header or parameter)
        String providedToken = request.getHeader(CSRF_HEADER);
        if (providedToken == null || providedToken.isBlank()) {
            providedToken = request.getParameter(CSRF_PARAM);
        }

        if (providedToken == null || providedToken.isBlank()) {
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        return secureCompare(expectedToken, providedToken);
    }

    /**
     * Gets the CSRF token for the current session, generating one if needed.
     * This method is used by the login page to include the token in forms.
     */
    public String getOrCreateCsrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(SESSION_ATTR_CSRF_TOKEN);
        if (token == null) {
            token = generateCsrfToken();
            session.setAttribute(SESSION_ATTR_CSRF_TOKEN, token);
        }
        return token;
    }

    /**
     * Validates redirect URL to prevent open redirect attacks.
     * Uses URI parsing for secure validation instead of string manipulation.
     */
    private String sanitizeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return pathPrefix;
        }

        try {
            // Decode URL to catch encoded bypass attempts like %2f%2f
            String decodedUrl = java.net.URLDecoder.decode(redirectUrl, StandardCharsets.UTF_8);

            // Parse as URI for proper validation
            URI uri = new URI(decodedUrl);

            // Block absolute URLs (with scheme or authority)
            if (uri.isAbsolute() || uri.getAuthority() != null) {
                log.debug("Blocked redirect to absolute URL: {}", redirectUrl);
                return pathPrefix;
            }

            // Get the path component
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return pathPrefix;
            }

            // Block protocol-relative URLs
            if (path.startsWith("//")) {
                log.debug("Blocked protocol-relative redirect: {}", redirectUrl);
                return pathPrefix;
            }

            // Must start with the J-Obs path prefix
            if (!path.startsWith(pathPrefix)) {
                log.debug("Blocked redirect outside J-Obs path: {}", redirectUrl);
                return pathPrefix;
            }

            // Block path traversal attempts
            if (path.contains("..") || path.contains("./")) {
                log.debug("Blocked path traversal in redirect: {}", redirectUrl);
                return pathPrefix;
            }

            // Block newline injection (HTTP response splitting)
            if (path.contains("\r") || path.contains("\n") ||
                path.contains("%0d") || path.contains("%0a") ||
                path.contains("%0D") || path.contains("%0A")) {
                log.debug("Blocked newline injection in redirect: {}", redirectUrl);
                return pathPrefix;
            }

            // Re-encode to ensure safe output
            return path + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

        } catch (URISyntaxException | IllegalArgumentException e) {
            log.debug("Invalid redirect URL: {}", redirectUrl, e);
            return pathPrefix;
        }
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

    /**
     * Cleans up resources when the bean is destroyed.
     * This ensures the rate limiter's cleanup thread is properly shut down.
     */
    @Override
    public void destroy() {
        log.debug("Shutting down J-Obs authentication filter");
        if (loginRateLimiter != null) {
            loginRateLimiter.shutdown();
        }
    }
}
