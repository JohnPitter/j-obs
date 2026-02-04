package io.github.jobs.spring.web;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent injection attacks.
 */
public final class InputSanitizer {

    // Maximum lengths for various input types
    private static final int MAX_LOGGER_LENGTH = 256;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final int MAX_THREAD_LENGTH = 128;
    private static final int MAX_GENERIC_LENGTH = 256;

    // Maximum number of wildcards allowed in a search pattern (ReDoS prevention)
    private static final int MAX_WILDCARDS = 5;

    // Patterns for validation
    // Trace IDs can be: OpenTelemetry (32 hex), W3C (hex with dashes), or custom (alphanumeric with dashes/underscores)
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\w\\s.\\-_@/:,()\\[\\]{}]+$");

    private InputSanitizer() {
        // Utility class
    }

    /**
     * Sanitizes a logger name parameter.
     * Removes potentially dangerous characters and limits length.
     */
    public static String sanitizeLogger(String logger) {
        if (logger == null || logger.isBlank()) {
            return null;
        }
        String sanitized = removeControlCharacters(logger);
        sanitized = truncate(sanitized, MAX_LOGGER_LENGTH);
        // Logger names should only contain package-like characters
        if (!isValidLoggerName(sanitized)) {
            return null;
        }
        return sanitized;
    }

    /**
     * Sanitizes a message search pattern.
     * Removes potentially dangerous regex characters and limits length.
     */
    public static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String sanitized = removeControlCharacters(message);
        sanitized = truncate(sanitized, MAX_MESSAGE_LENGTH);
        // Escape regex special characters for safe pattern matching
        sanitized = escapeRegexSpecialChars(sanitized);
        return sanitized;
    }

    /**
     * Sanitizes a trace ID parameter.
     * Allows alphanumeric characters, dashes, and underscores.
     */
    public static String sanitizeTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        String sanitized = removeControlCharacters(traceId.trim());
        sanitized = truncate(sanitized, MAX_TRACE_ID_LENGTH);
        if (!TRACE_ID_PATTERN.matcher(sanitized).matches()) {
            return null;
        }
        return sanitized;
    }

    /**
     * Sanitizes a thread name parameter.
     */
    public static String sanitizeThreadName(String thread) {
        if (thread == null || thread.isBlank()) {
            return null;
        }
        String sanitized = removeControlCharacters(thread);
        sanitized = truncate(sanitized, MAX_THREAD_LENGTH);
        return sanitized;
    }

    /**
     * Sanitizes a generic string parameter.
     */
    public static String sanitizeGeneric(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String sanitized = removeControlCharacters(input);
        sanitized = truncate(sanitized, MAX_GENERIC_LENGTH);
        return sanitized;
    }

    /**
     * Validates and normalizes limit parameter.
     */
    public static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return 100; // default
        }
        return Math.min(limit, 1000); // max 1000
    }

    /**
     * Validates and normalizes offset parameter.
     */
    public static int sanitizeOffset(int offset) {
        return Math.max(0, offset);
    }

    /**
     * Removes control characters from input.
     */
    private static String removeControlCharacters(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (!Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Truncates string to maximum length.
     */
    private static String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }

    /**
     * Validates logger name format.
     */
    private static boolean isValidLoggerName(String logger) {
        if (logger == null || logger.isEmpty()) {
            return false;
        }
        // Logger names should be like: com.example.MyClass or simple names
        return logger.matches("^[a-zA-Z_$][a-zA-Z0-9_$.]*$");
    }

    /**
     * Escapes regex special characters for safe use in Pattern.
     * Limits the number of wildcards to prevent ReDoS attacks.
     *
     * @throws IllegalArgumentException if too many wildcards are present
     */
    private static String escapeRegexSpecialChars(String input) {
        if (input == null) {
            return null;
        }

        // Pre-process: collapse consecutive wildcards to prevent ReDoS
        // "**" or "***" become a single "*"
        String collapsed = collapseWildcards(input);

        // Count wildcards and reject if too many
        int wildcardCount = countWildcards(collapsed);
        if (wildcardCount > MAX_WILDCARDS) {
            throw new IllegalArgumentException(
                    "Too many wildcards in search pattern (max " + MAX_WILDCARDS + ", found " + wildcardCount + ")");
        }

        // We want to allow * as wildcard for user-friendly search
        // Convert * to .* for regex matching
        StringBuilder sb = new StringBuilder();
        for (char c : collapsed.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '.' -> sb.append("\\.");
                case '\\' -> sb.append("\\\\");
                case '[', ']', '(', ')', '{', '}', '^', '$', '|', '?', '+' -> {
                    sb.append("\\");
                    sb.append(c);
                }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Collapses consecutive wildcards into a single wildcard.
     * This prevents patterns like "a**b" from becoming "a.*.*b" which can cause ReDoS.
     */
    private static String collapseWildcards(String input) {
        if (input == null || !input.contains("*")) {
            return input;
        }
        // Replace 2+ consecutive wildcards with a single one
        return input.replaceAll("\\*{2,}", "*");
    }

    /**
     * Counts the number of wildcard characters in a string.
     */
    private static int countWildcards(String input) {
        if (input == null) {
            return 0;
        }
        int count = 0;
        for (char c : input.toCharArray()) {
            if (c == '*') {
                count++;
            }
        }
        return count;
    }
}
