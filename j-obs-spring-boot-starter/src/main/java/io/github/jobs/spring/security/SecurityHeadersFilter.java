package io.github.jobs.spring.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Security headers filter that adds protective HTTP headers to all J-Obs responses.
 * <p>
 * This filter adds the following security headers:
 * <ul>
 *   <li><b>X-Frame-Options: DENY</b> - Prevents clickjacking by disabling framing</li>
 *   <li><b>X-Content-Type-Options: nosniff</b> - Prevents MIME type sniffing</li>
 *   <li><b>X-XSS-Protection: 1; mode=block</b> - Enables XSS filter in older browsers</li>
 *   <li><b>Referrer-Policy: strict-origin-when-cross-origin</b> - Controls referrer information</li>
 *   <li><b>Permissions-Policy</b> - Restricts browser features</li>
 *   <li><b>Content-Security-Policy</b> - Restricts resource loading (configurable)</li>
 *   <li><b>Strict-Transport-Security</b> - Enforces HTTPS (only over HTTPS)</li>
 *   <li><b>Cache-Control</b> - Prevents caching of sensitive pages</li>
 * </ul>
 */
public class SecurityHeadersFilter implements Filter {

    private final SecurityHeadersConfig config;

    public SecurityHeadersFilter() {
        this(new SecurityHeadersConfig());
    }

    public SecurityHeadersFilter(SecurityHeadersConfig config) {
        this.config = config;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Prevent clickjacking
            httpResponse.setHeader("X-Frame-Options", config.getFrameOptions());

            // Prevent MIME type sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // XSS protection for older browsers
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // Control referrer information
            httpResponse.setHeader("Referrer-Policy", config.getReferrerPolicy());

            // Restrict browser features
            httpResponse.setHeader("Permissions-Policy", config.getPermissionsPolicy());

            // Content Security Policy
            httpResponse.setHeader("Content-Security-Policy", config.getContentSecurityPolicy());

            // HSTS - only set over HTTPS
            if (isSecureRequest(httpRequest)) {
                httpResponse.setHeader("Strict-Transport-Security",
                        "max-age=" + config.getHstsMaxAge() + "; includeSubDomains");
            }

            // Cache control for HTML pages (not static assets)
            String path = httpRequest.getRequestURI();
            if (isHtmlPage(path)) {
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup required
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() ||
               "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
               "https".equalsIgnoreCase(request.getScheme());
    }

    private boolean isHtmlPage(String path) {
        return !path.contains("/static/") &&
               !path.endsWith(".js") &&
               !path.endsWith(".css") &&
               !path.endsWith(".png") &&
               !path.endsWith(".jpg") &&
               !path.endsWith(".svg") &&
               !path.endsWith(".ico") &&
               !path.endsWith(".woff") &&
               !path.endsWith(".woff2");
    }

    /**
     * Configuration for security headers.
     */
    public static class SecurityHeadersConfig {

        private String frameOptions = "DENY";
        private String referrerPolicy = "strict-origin-when-cross-origin";
        private String permissionsPolicy = "geolocation=(), microphone=(), camera=(), payment=()";
        private String contentSecurityPolicy = buildDefaultCsp();
        private long hstsMaxAge = 31536000; // 1 year

        private static String buildDefaultCsp() {
            return String.join("; ",
                    "default-src 'self'",
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // HTMX and Alpine.js need unsafe-inline
                    "style-src 'self' 'unsafe-inline'", // Tailwind CSS needs unsafe-inline
                    "img-src 'self' data:",
                    "font-src 'self'",
                    "connect-src 'self' ws: wss:", // WebSocket connections
                    "frame-ancestors 'none'",
                    "form-action 'self'",
                    "base-uri 'self'"
            );
        }

        public String getFrameOptions() {
            return frameOptions;
        }

        public void setFrameOptions(String frameOptions) {
            this.frameOptions = frameOptions;
        }

        public String getReferrerPolicy() {
            return referrerPolicy;
        }

        public void setReferrerPolicy(String referrerPolicy) {
            this.referrerPolicy = referrerPolicy;
        }

        public String getPermissionsPolicy() {
            return permissionsPolicy;
        }

        public void setPermissionsPolicy(String permissionsPolicy) {
            this.permissionsPolicy = permissionsPolicy;
        }

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public long getHstsMaxAge() {
            return hstsMaxAge;
        }

        public void setHstsMaxAge(long hstsMaxAge) {
            this.hstsMaxAge = hstsMaxAge;
        }
    }
}
