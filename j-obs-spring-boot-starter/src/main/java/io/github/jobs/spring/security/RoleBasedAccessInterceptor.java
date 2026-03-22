package io.github.jobs.spring.security;

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
import java.util.List;

/**
 * Spring MVC interceptor that enforces role-based access control on J-Obs API endpoints.
 * <p>
 * This interceptor runs AFTER {@code JObsAuthenticationFilter} (authentication),
 * providing authorization based on the user's assigned {@link JObsRole}.
 * <p>
 * Access rules:
 * <ul>
 *   <li>POST/PUT/DELETE on alerts endpoints - requires ADMIN</li>
 *   <li>POST/DELETE on SLOs endpoints - requires ADMIN</li>
 *   <li>POST on profiling start endpoints - requires ADMIN or OPERATOR</li>
 *   <li>DELETE on profiling sessions - requires ADMIN</li>
 *   <li>All GET requests - any authenticated role (VIEWER+)</li>
 * </ul>
 */
public class RoleBasedAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RoleBasedAccessInterceptor.class);
    private static final String SESSION_ATTR_ROLE = "j-obs-role";

    private final String pathPrefix;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /** Paths that require ADMIN role for mutating operations. */
    private static final List<String> ADMIN_MUTATE_PATHS = List.of(
            "/api/alerts/**",
            "/api/slos/**"
    );

    /** Profiling paths that require ADMIN or OPERATOR for POST. */
    private static final List<String> PROFILING_START_PATHS = List.of(
            "/api/profiling/cpu/start",
            "/api/profiling/memory",
            "/api/profiling/threads"
    );

    /** Profiling paths that require ADMIN for DELETE. */
    private static final List<String> PROFILING_ADMIN_PATHS = List.of(
            "/api/profiling/sessions"
    );

    public RoleBasedAccessInterceptor(JObsProperties properties) {
        this.pathPrefix = properties.getPath();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String method = request.getMethod().toUpperCase();

        // GET requests are allowed for any authenticated user
        if ("GET".equals(method)) {
            return true;
        }

        JObsRole role = resolveRole(request);
        if (role == null) {
            // No role in session — let the auth filter handle authentication.
            // This allows unauthenticated requests to pass through to the auth filter.
            return true;
        }

        String relativePath = extractRelativePath(request.getRequestURI());

        // Check ADMIN-only mutating operations on alerts/SLOs
        if (isMutatingMethod(method) && matchesAny(relativePath, ADMIN_MUTATE_PATHS)) {
            if (!role.canModifyAlerts()) {
                return forbidden(response, "Insufficient permissions: ADMIN role required");
            }
        }

        // Check profiling start (ADMIN or OPERATOR)
        if ("POST".equals(method) && matchesAny(relativePath, PROFILING_START_PATHS)) {
            if (!role.canStartProfiling()) {
                return forbidden(response, "Insufficient permissions: ADMIN or OPERATOR role required");
            }
        }

        // Check profiling session deletion (ADMIN only)
        if ("DELETE".equals(method) && matchesAny(relativePath, PROFILING_ADMIN_PATHS)) {
            if (!role.canModifyAlerts()) {
                return forbidden(response, "Insufficient permissions: ADMIN role required");
            }
        }

        return true;
    }

    private JObsRole resolveRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object roleAttr = session.getAttribute(SESSION_ATTR_ROLE);
        if (roleAttr instanceof JObsRole) {
            return (JObsRole) roleAttr;
        }
        return null;
    }

    private String extractRelativePath(String requestUri) {
        if (requestUri.startsWith(pathPrefix)) {
            return requestUri.substring(pathPrefix.length());
        }
        return requestUri;
    }

    private boolean isMutatingMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
    }

    private boolean matchesAny(String path, List<String> patterns) {
        for (String pattern : patterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean forbidden(HttpServletResponse response, String message) throws IOException {
        log.warn("Access denied: {}", message);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + message + "\"}");
        return false;
    }
}
