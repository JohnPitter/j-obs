package io.github.jobs.spring.webflux;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive WebFilter that handles authentication for J-Obs dashboard endpoints.
 * Supports Basic Authentication and API Key authentication.
 */
public class ReactiveAuthenticationFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ReactiveAuthenticationFilter.class);
    private static final String SESSION_ATTR_AUTHENTICATED = "j-obs-authenticated";
    private static final String SESSION_ATTR_USERNAME = "j-obs-username";

    private final JObsProperties.Security securityConfig;
    private final String pathPrefix;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Cache for validated API keys (key -> timestamp)
    private final Map<String, Long> validatedApiKeys = new ConcurrentHashMap<>();

    public ReactiveAuthenticationFilter(JObsProperties.Security securityConfig, String pathPrefix) {
        this.securityConfig = securityConfig;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100; // After rate limiting
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Only apply to J-Obs paths
        if (!path.startsWith(pathPrefix)) {
            return chain.filter(exchange);
        }

        // Check if path is exempt from authentication
        if (isExemptPath(path)) {
            return chain.filter(exchange);
        }

        // Handle login endpoint
        if (isLoginRequest(request, path)) {
            return handleLoginRequest(exchange);
        }

        // Handle logout endpoint
        if (isLogoutRequest(request, path)) {
            return handleLogoutRequest(exchange);
        }

        // Check API key authentication first (stateless)
        if (isApiKeyAuthenticated(request)) {
            return chain.filter(exchange);
        }

        // Check Basic Auth header
        if (isBasicAuthAuthenticated(request)) {
            return chain.filter(exchange);
        }

        // Check session authentication
        return exchange.getSession()
                .flatMap(session -> {
                    Boolean authenticated = session.getAttribute(SESSION_ATTR_AUTHENTICATED);
                    if (Boolean.TRUE.equals(authenticated)) {
                        return chain.filter(exchange);
                    }
                    return handleUnauthenticated(exchange);
                });
    }

    private boolean isExemptPath(String path) {
        String relativePath = path.substring(pathPrefix.length());
        if (relativePath.isEmpty()) {
            relativePath = "/";
        }

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

    private boolean isLoginRequest(ServerHttpRequest request, String path) {
        return "POST".equals(request.getMethod().name()) &&
               (path.equals(pathPrefix + "/login") || path.equals(pathPrefix + "/api/login"));
    }

    private boolean isLogoutRequest(ServerHttpRequest request, String path) {
        return path.equals(pathPrefix + "/logout") || path.equals(pathPrefix + "/api/logout");
    }

    private Mono<Void> handleLoginRequest(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String username = formData.getFirst("username");
                    String password = formData.getFirst("password");
                    String redirect = formData.getFirst("redirect");

                    if (validateCredentials(username, password)) {
                        return exchange.getSession()
                                .flatMap(session -> {
                                    session.getAttributes().put(SESSION_ATTR_AUTHENTICATED, true);
                                    session.getAttributes().put(SESSION_ATTR_USERNAME, username);
                                    session.setMaxIdleTime(securityConfig.getSessionTimeout());

                                    log.info("User '{}' logged in to J-Obs dashboard", username);

                                    String redirectUrl = (redirect != null && !redirect.isBlank()) ? redirect : pathPrefix;
                                    ServerHttpResponse response = exchange.getResponse();
                                    response.setStatusCode(HttpStatus.FOUND);
                                    response.getHeaders().setLocation(URI.create(redirectUrl));
                                    return response.setComplete();
                                });
                    }

                    // Login failed
                    log.warn("Failed login attempt for user '{}' from {}",
                            username, exchange.getRequest().getRemoteAddress());

                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.FOUND);
                    response.getHeaders().setLocation(URI.create(pathPrefix + "/login?error=true"));
                    return response.setComplete();
                });
    }

    private Mono<Void> handleLogoutRequest(ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(session -> {
                    String username = session.getAttribute(SESSION_ATTR_USERNAME);
                    return session.invalidate()
                            .then(Mono.fromRunnable(() -> {
                                if (username != null) {
                                    log.info("User '{}' logged out from J-Obs dashboard", username);
                                }
                            }));
                })
                .then(Mono.defer(() -> {
                    ServerHttpRequest request = exchange.getRequest();
                    ServerHttpResponse response = exchange.getResponse();

                    // For API logout, return 200 OK
                    if (request.getPath().value().contains("/api/")) {
                        response.setStatusCode(HttpStatus.OK);
                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        return response.writeWith(Mono.just(
                                response.bufferFactory().wrap("{\"message\":\"Logged out successfully\"}".getBytes(StandardCharsets.UTF_8))
                        ));
                    } else {
                        // Redirect to login page
                        response.setStatusCode(HttpStatus.FOUND);
                        response.getHeaders().setLocation(URI.create(pathPrefix + "/login?logout=true"));
                        return response.setComplete();
                    }
                }));
    }

    private boolean isApiKeyAuthenticated(ServerHttpRequest request) {
        String authType = securityConfig.getType().toLowerCase();
        if (!"api-key".equals(authType) && !"both".equals(authType)) {
            return false;
        }

        String apiKeyHeader = securityConfig.getApiKeyHeader();
        String apiKey = request.getHeaders().getFirst(apiKeyHeader);

        if (apiKey == null || apiKey.isBlank()) {
            // Also check query parameter
            apiKey = request.getQueryParams().getFirst("api_key");
        }

        return apiKey != null && validateApiKey(apiKey);
    }

    private boolean isBasicAuthAuthenticated(ServerHttpRequest request) {
        String authType = securityConfig.getType().toLowerCase();
        if (!"basic".equals(authType) && !"both".equals(authType)) {
            return false;
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
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

            return validateCredentials(username, password);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid Base64 in Authorization header", e);
            return false;
        }
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

        // Check cache first
        Long cachedTime = validatedApiKeys.get(apiKey);
        if (cachedTime != null && System.currentTimeMillis() - cachedTime < 60000) {
            return true;
        }

        for (String validKey : validKeys) {
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

    private Mono<Void> handleUnauthenticated(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String acceptHeader = request.getHeaders().getFirst(HttpHeaders.ACCEPT);
        boolean isApiRequest = request.getPath().value().contains("/api/");
        boolean isBrowserRequest = acceptHeader != null && acceptHeader.contains("text/html") && !isApiRequest;

        if (isBrowserRequest) {
            // Redirect to login page
            String currentPath = request.getURI().toString();
            String redirectParam = URLEncoder.encode(currentPath, StandardCharsets.UTF_8);
            response.setStatusCode(HttpStatus.FOUND);
            response.getHeaders().setLocation(URI.create(pathPrefix + "/login?redirect=" + redirectParam));
            return response.setComplete();
        } else {
            // Return 401 for API requests
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"J-Obs Dashboard\"");
            return response.writeWith(Mono.just(
                    response.bufferFactory().wrap("{\"error\":\"Authentication required\",\"message\":\"Please provide valid credentials\"}".getBytes(StandardCharsets.UTF_8))
            ));
        }
    }
}
