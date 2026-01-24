package io.github.jobs.spring.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that applies rate limiting to requests.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final String pathPrefix;

    public RateLimitInterceptor(RateLimiter rateLimiter, String pathPrefix) {
        this.rateLimiter = rateLimiter;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only rate limit API endpoints
        String path = request.getRequestURI();
        if (!path.startsWith(pathPrefix + "/api/")) {
            return true;
        }

        String clientKey = getClientKey(request);

        if (!rateLimiter.tryAcquire(clientKey)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");

            // Add rate limit headers
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60");

            return false;
        }

        // Add remaining requests header
        int remaining = rateLimiter.getRemainingRequests(clientKey);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        return true;
    }

    /**
     * Extracts a unique key for the client from the request.
     * Uses {@code request.getRemoteAddr()} which respects Spring's
     * {@code ForwardedHeaderFilter} when {@code server.forward-headers-strategy}
     * is configured. Does NOT read X-Forwarded-For directly to prevent
     * rate limit bypass via header spoofing.
     */
    private String getClientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
