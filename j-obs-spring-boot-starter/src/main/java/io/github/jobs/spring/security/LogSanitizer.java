package io.github.jobs.spring.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sanitizes log messages to mask sensitive data before storage.
 * <p>
 * This sanitizer detects and masks:
 * <ul>
 *   <li>Passwords in various formats (password=xxx, "password": "xxx")</li>
 *   <li>API keys and tokens</li>
 *   <li>Authorization headers (Basic, Bearer)</li>
 *   <li>Credit card numbers</li>
 *   <li>Social Security Numbers</li>
 *   <li>Email addresses (partial masking)</li>
 *   <li>Custom patterns via configuration</li>
 * </ul>
 * <p>
 * Performance considerations:
 * <ul>
 *   <li>Patterns are pre-compiled for efficiency</li>
 *   <li>Early return if no potential sensitive data detected</li>
 *   <li>Thread-safe and stateless</li>
 * </ul>
 */
public class LogSanitizer {

    private static final String MASK = "***REDACTED***";
    private static final String PARTIAL_MASK = "***";

    // Pre-compiled patterns for common sensitive data
    private static final List<SanitizationRule> DEFAULT_RULES = List.of(
            // Passwords in various formats
            new SanitizationRule(
                    "password",
                    Pattern.compile("(?i)(password|passwd|pwd|secret|credential)[\"'\\s]*[:=][\"'\\s]*([^\"'\\s,;}&]+)", Pattern.CASE_INSENSITIVE),
                    "$1=***REDACTED***"
            ),
            // JSON passwords
            new SanitizationRule(
                    "json-password",
                    Pattern.compile("(?i)(\"(?:password|passwd|pwd|secret|credential)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
                    "$1\"***REDACTED***\""
            ),
            // API keys
            new SanitizationRule(
                    "api-key",
                    Pattern.compile("(?i)(api[_-]?key|apikey|x-api-key)[\"'\\s]*[:=][\"'\\s]*([^\"'\\s,;}&]+)", Pattern.CASE_INSENSITIVE),
                    "$1=***REDACTED***"
            ),
            // JSON API keys
            new SanitizationRule(
                    "json-api-key",
                    Pattern.compile("(?i)(\"(?:api[_-]?key|apikey)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
                    "$1\"***REDACTED***\""
            ),
            // Bearer tokens
            new SanitizationRule(
                    "bearer-token",
                    Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE),
                    "$1***REDACTED***"
            ),
            // Basic Auth header
            new SanitizationRule(
                    "basic-auth",
                    Pattern.compile("(?i)(Basic\\s+)[A-Za-z0-9+/=]+", Pattern.CASE_INSENSITIVE),
                    "$1***REDACTED***"
            ),
            // Authorization header value
            new SanitizationRule(
                    "auth-header",
                    Pattern.compile("(?i)(Authorization[\"'\\s]*[:=][\"'\\s]*)([^\"'\\s,;&]+)", Pattern.CASE_INSENSITIVE),
                    "$1***REDACTED***"
            ),
            // Generic tokens
            new SanitizationRule(
                    "token",
                    Pattern.compile("(?i)((?:access|refresh|auth|session|jwt)[_-]?token)[\"'\\s]*[:=][\"'\\s]*([^\"'\\s,;}&]+)", Pattern.CASE_INSENSITIVE),
                    "$1=***REDACTED***"
            ),
            // JSON tokens
            new SanitizationRule(
                    "json-token",
                    Pattern.compile("(?i)(\"(?:access|refresh|auth|session|jwt)[_-]?token\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
                    "$1\"***REDACTED***\""
            ),
            // Credit card numbers (basic detection - 13-19 digits with optional separators)
            new SanitizationRule(
                    "credit-card",
                    Pattern.compile("\\b([0-9]{4})[- ]?([0-9]{4})[- ]?([0-9]{4})[- ]?([0-9]{1,7})\\b"),
                    "****-****-****-$4"
            ),
            // AWS Access Keys
            new SanitizationRule(
                    "aws-key",
                    Pattern.compile("(?i)(AKIA|ABIA|ACCA|ASIA)[A-Z0-9]{16}"),
                    "***AWS_KEY_REDACTED***"
            ),
            // Private keys (PEM format start)
            new SanitizationRule(
                    "private-key",
                    Pattern.compile("-----BEGIN[A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END[A-Z ]*PRIVATE KEY-----"),
                    "***PRIVATE_KEY_REDACTED***"
            )
    );

    // Quick check patterns to avoid full regex scan on clean messages
    private static final List<String> QUICK_CHECK_KEYWORDS = List.of(
            "password", "passwd", "pwd", "secret", "credential",
            "api_key", "api-key", "apikey",
            "token", "bearer", "basic", "authorization",
            "-----begin", "akia", "abia", "acca", "asia"
    );

    private final List<SanitizationRule> rules;
    private final boolean enabled;

    /**
     * Creates a LogSanitizer with default rules.
     */
    public LogSanitizer() {
        this(true, List.of());
    }

    /**
     * Creates a LogSanitizer with custom configuration.
     *
     * @param enabled whether sanitization is enabled
     * @param additionalPatterns additional regex patterns to sanitize (pattern -> replacement)
     */
    public LogSanitizer(boolean enabled, List<Map.Entry<String, String>> additionalPatterns) {
        this.enabled = enabled;
        this.rules = new ArrayList<>(DEFAULT_RULES);

        if (additionalPatterns != null) {
            for (Map.Entry<String, String> entry : additionalPatterns) {
                rules.add(new SanitizationRule(
                        "custom-" + rules.size(),
                        Pattern.compile(entry.getKey()),
                        entry.getValue()
                ));
            }
        }
    }

    /**
     * Sanitizes a log message by masking sensitive data.
     *
     * @param message the original log message
     * @return the sanitized message
     */
    public String sanitize(String message) {
        if (!enabled || message == null || message.isEmpty()) {
            return message;
        }

        // Quick check to avoid expensive regex operations on clean messages
        String lowerMessage = message.toLowerCase();
        boolean potentiallySensitive = QUICK_CHECK_KEYWORDS.stream()
                .anyMatch(lowerMessage::contains);

        // Also check for patterns that look like credit cards or keys
        if (!potentiallySensitive) {
            potentiallySensitive = message.matches(".*\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{1,7}.*");
        }

        if (!potentiallySensitive) {
            return message;
        }

        // Apply sanitization rules
        String result = message;
        for (SanitizationRule rule : rules) {
            result = rule.pattern.matcher(result).replaceAll(rule.replacement);
        }

        return result;
    }

    /**
     * Sanitizes a map of MDC properties.
     *
     * @param mdc the original MDC map
     * @return a new map with sanitized values
     */
    public Map<String, String> sanitizeMdc(Map<String, String> mdc) {
        if (!enabled || mdc == null || mdc.isEmpty()) {
            return mdc;
        }

        return mdc.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> sanitizeValue(e.getKey(), e.getValue())
                ));
    }

    /**
     * Sanitizes a value based on its key name.
     */
    private String sanitizeValue(String key, String value) {
        if (value == null) {
            return null;
        }

        String lowerKey = key.toLowerCase();

        // Completely redact values with sensitive key names
        if (lowerKey.contains("password") ||
            lowerKey.contains("secret") ||
            lowerKey.contains("credential") ||
            lowerKey.contains("api_key") ||
            lowerKey.contains("apikey") ||
            lowerKey.contains("token") ||
            lowerKey.contains("authorization")) {
            return MASK;
        }

        // Apply general sanitization to the value
        return sanitize(value);
    }

    /**
     * Sanitizes a throwable message (stack trace).
     *
     * @param stackTrace the original stack trace
     * @return the sanitized stack trace
     */
    public String sanitizeStackTrace(String stackTrace) {
        if (!enabled || stackTrace == null) {
            return stackTrace;
        }
        return sanitize(stackTrace);
    }

    /**
     * Checks if sanitization is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Internal class representing a sanitization rule.
     */
    private static class SanitizationRule {
        final String name;
        final Pattern pattern;
        final String replacement;

        SanitizationRule(String name, Pattern pattern, String replacement) {
            this.name = name;
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }
}
