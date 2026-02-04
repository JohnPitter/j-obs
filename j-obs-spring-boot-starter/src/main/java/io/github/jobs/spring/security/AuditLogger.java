package io.github.jobs.spring.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security audit logger for J-Obs.
 * <p>
 * Logs security-relevant events in a structured format suitable for compliance
 * and security monitoring. Events include authentication attempts, access control
 * decisions, and configuration changes.
 * <p>
 * Log format uses structured JSON-like output with consistent fields:
 * <pre>
 * [AUDIT] event=LOGIN_SUCCESS user=admin ip=192.168.1.1 timestamp=2024-01-15T10:30:00Z
 * </pre>
 * <p>
 * The audit log uses a dedicated logger category {@code j-obs.audit} which can be
 * configured separately in logging configuration:
 * <pre>{@code
 * logging:
 *   level:
 *     j-obs.audit: INFO
 * }</pre>
 */
public final class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("j-obs.audit");

    /**
     * Audit event types for categorization.
     */
    public enum EventType {
        // Authentication events
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGIN_LOCKED,
        LOGOUT,

        // Authorization events
        ACCESS_DENIED,
        ACCESS_GRANTED,

        // API Key events
        API_KEY_USED,
        API_KEY_INVALID,
        API_KEY_INSECURE,  // e.g., passed via query parameter

        // Session events
        SESSION_CREATED,
        SESSION_EXPIRED,
        SESSION_INVALIDATED,

        // Security events
        CSRF_FAILURE,
        RATE_LIMITED,

        // Configuration events
        CONFIG_CHANGED,
        PROVIDER_CONFIGURED,
        ALERT_CREATED,
        ALERT_DELETED
    }

    private AuditLogger() {
        // Utility class
    }

    /**
     * Logs a successful login attempt.
     *
     * @param username the username that logged in
     * @param ip       the client IP address
     */
    public static void logLoginSuccess(String username, String ip) {
        log(EventType.LOGIN_SUCCESS, Map.of(
                "user", sanitize(username),
                "ip", sanitize(ip)
        ));
    }

    /**
     * Logs a failed login attempt.
     *
     * @param username          the username that attempted to log in
     * @param ip                the client IP address
     * @param remainingAttempts number of attempts remaining before lockout
     */
    public static void logLoginFailure(String username, String ip, int remainingAttempts) {
        log(EventType.LOGIN_FAILURE, Map.of(
                "user", sanitize(username),
                "ip", sanitize(ip),
                "remainingAttempts", String.valueOf(remainingAttempts)
        ));
    }

    /**
     * Logs a login attempt that was blocked due to rate limiting.
     *
     * @param ip              the client IP address
     * @param lockoutSeconds  seconds remaining in lockout
     */
    public static void logLoginLocked(String ip, long lockoutSeconds) {
        log(EventType.LOGIN_LOCKED, Map.of(
                "ip", sanitize(ip),
                "lockoutSeconds", String.valueOf(lockoutSeconds)
        ));
    }

    /**
     * Logs a logout event.
     *
     * @param username the username that logged out
     * @param ip       the client IP address
     */
    public static void logLogout(String username, String ip) {
        log(EventType.LOGOUT, Map.of(
                "user", sanitize(username),
                "ip", sanitize(ip)
        ));
    }

    /**
     * Logs an access denied event.
     *
     * @param path   the path that was denied
     * @param ip     the client IP address
     * @param reason the reason for denial
     */
    public static void logAccessDenied(String path, String ip, String reason) {
        log(EventType.ACCESS_DENIED, Map.of(
                "path", sanitize(path),
                "ip", sanitize(ip),
                "reason", sanitize(reason)
        ));
    }

    /**
     * Logs successful API key authentication.
     *
     * @param keyPrefix first 4 characters of the API key (for identification)
     * @param ip        the client IP address
     */
    public static void logApiKeyUsed(String apiKey, String ip) {
        String keyPrefix = (apiKey != null && apiKey.length() >= 4)
                ? apiKey.substring(0, 4) + "****"
                : "****";
        log(EventType.API_KEY_USED, Map.of(
                "keyPrefix", keyPrefix,
                "ip", sanitize(ip)
        ));
    }

    /**
     * Logs an invalid API key attempt.
     *
     * @param ip the client IP address
     */
    public static void logApiKeyInvalid(String ip) {
        log(EventType.API_KEY_INVALID, Map.of(
                "ip", sanitize(ip)
        ));
    }

    /**
     * Logs an insecure API key usage (e.g., via query parameter).
     *
     * @param ip the client IP address
     */
    public static void logApiKeyInsecure(String ip) {
        log(EventType.API_KEY_INSECURE, Map.of(
                "ip", sanitize(ip),
                "reason", "API key passed via query parameter"
        ));
    }

    /**
     * Logs a CSRF token validation failure.
     *
     * @param ip   the client IP address
     * @param path the request path
     */
    public static void logCsrfFailure(String ip, String path) {
        log(EventType.CSRF_FAILURE, Map.of(
                "ip", sanitize(ip),
                "path", sanitize(path)
        ));
    }

    /**
     * Logs a rate limiting event.
     *
     * @param ip      the client IP address
     * @param path    the request path
     * @param seconds seconds until rate limit resets
     */
    public static void logRateLimited(String ip, String path, long seconds) {
        log(EventType.RATE_LIMITED, Map.of(
                "ip", sanitize(ip),
                "path", sanitize(path),
                "retryAfterSeconds", String.valueOf(seconds)
        ));
    }

    /**
     * Logs a configuration change.
     *
     * @param component the component that was changed
     * @param action    the action taken (created, updated, deleted)
     * @param user      the user who made the change (or "system")
     */
    public static void logConfigChange(String component, String action, String user) {
        log(EventType.CONFIG_CHANGED, Map.of(
                "component", sanitize(component),
                "action", sanitize(action),
                "user", sanitize(user)
        ));
    }

    /**
     * Logs a generic audit event with custom attributes.
     *
     * @param eventType  the type of event
     * @param attributes key-value pairs of event attributes
     */
    public static void log(EventType eventType, Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[AUDIT] event=").append(eventType.name());

        // Add timestamp
        sb.append(" timestamp=").append(Instant.now());

        // Add MDC context if available (trace ID, etc.)
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            sb.append(" traceId=").append(traceId);
        }

        // Add custom attributes
        if (attributes != null) {
            // Use LinkedHashMap to preserve order
            Map<String, String> orderedAttrs = new LinkedHashMap<>(attributes);
            orderedAttrs.forEach((key, value) -> {
                sb.append(" ").append(key).append("=").append(value);
            });
        }

        // Log at INFO level for audit trail
        log.info(sb.toString());
    }

    /**
     * Sanitizes a value for safe logging.
     * Removes newlines and limits length to prevent log injection.
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "-";
        }
        // Remove control characters and limit length
        return value
                .replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("[\\x00-\\x1F]", "")
                .substring(0, Math.min(value.length(), 256));
    }
}
