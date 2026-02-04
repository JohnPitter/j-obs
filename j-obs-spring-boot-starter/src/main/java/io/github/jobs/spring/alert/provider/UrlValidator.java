package io.github.jobs.spring.alert.provider;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for validating URLs to prevent Server-Side Request Forgery (SSRF) attacks.
 *
 * <p>This validator ensures webhook URLs don't point to internal network resources
 * that could be exploited for SSRF attacks.</p>
 *
 * <h2>Security Checks</h2>
 * <ul>
 *   <li>Blocks localhost and loopback addresses (127.0.0.1, ::1)</li>
 *   <li>Blocks private IP ranges (10.x, 172.16-31.x, 192.168.x)</li>
 *   <li>Blocks link-local addresses (169.254.x, fe80::)</li>
 *   <li>Only allows HTTP and HTTPS schemes</li>
 *   <li>Resolves hostnames to verify target IP addresses</li>
 * </ul>
 *
 * <h2>Trusted Domains</h2>
 * <p>The following webhook domains are whitelisted and bypass IP checks:</p>
 * <ul>
 *   <li>api.telegram.org</li>
 *   <li>hooks.slack.com</li>
 *   <li>outlook.office.com</li>
 *   <li>outlook.webhook.office.com</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * UrlValidator.ValidationResult result = UrlValidator.validate(webhookUrl);
 * if (!result.isValid()) {
 *     log.warn("Invalid URL: {}", result.errorMessage());
 *     return;
 * }
 * // Safe to use the URL
 * }</pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 */
public final class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0.0.0.0",
            "0:0:0:0:0:0:0:1"
    );

    // Pattern for private IPv4 ranges
    private static final Pattern PRIVATE_IPV4_PATTERN = Pattern.compile(
            "^(10\\..*)|(172\\.(1[6-9]|2[0-9]|3[0-1])\\..*)|(192\\.168\\..*)$"
    );

    // Pattern for link-local addresses
    private static final Pattern LINK_LOCAL_PATTERN = Pattern.compile(
            "^(169\\.254\\..*)|(fe80:.*)$"
    );

    // Known webhook domains that are allowed
    private static final Set<String> KNOWN_WEBHOOK_DOMAINS = Set.of(
            "api.telegram.org",
            "hooks.slack.com",
            "outlook.office.com",
            "outlook.webhook.office.com"
    );

    private UrlValidator() {
        // Utility class
    }

    /**
     * Validates a URL for webhook usage.
     *
     * @param urlString the URL to validate
     * @return validation result
     */
    public static ValidationResult validate(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return ValidationResult.invalid("URL cannot be empty");
        }

        URL url;
        try {
            url = URI.create(urlString).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            return ValidationResult.invalid("Invalid URL format: " + e.getMessage());
        }

        // Validate scheme
        String scheme = url.getProtocol().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            return ValidationResult.invalid("Invalid scheme: " + scheme + ". Only HTTP/HTTPS allowed.");
        }

        String host = url.getHost();
        if (host == null || host.isBlank()) {
            return ValidationResult.invalid("URL must have a valid host");
        }

        host = host.toLowerCase();

        // Strip brackets from IPv6 addresses (URL returns [::1] for IPv6)
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        // Check against blocked hosts
        if (BLOCKED_HOSTS.contains(host)) {
            return ValidationResult.invalid("URL points to a blocked host: " + host);
        }

        // If it's a known webhook domain, allow it
        if (isKnownWebhookDomain(host)) {
            return ValidationResult.valid();
        }

        // Check for IP addresses
        if (isIpAddress(host)) {
            if (isPrivateOrReservedIp(host)) {
                return ValidationResult.invalid("URL points to a private or reserved IP address");
            }
        } else {
            // For hostnames, try to resolve and check the IP
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress addr : addresses) {
                    if (isPrivateOrLoopback(addr)) {
                        return ValidationResult.invalid(
                                "URL hostname resolves to a private or loopback address");
                    }
                }
            } catch (UnknownHostException e) {
                // Can't resolve - we'll allow it and let the HTTP client handle the error
                // This is safer than blocking everything we can't resolve
            }
        }

        // Validate port
        int port = url.getPort();
        if (port != -1 && (port < 1 || port > 65535)) {
            return ValidationResult.invalid("Invalid port number: " + port);
        }

        return ValidationResult.valid();
    }

    /**
     * Convenience method to check if a URL is safe for webhook usage.
     *
     * @param urlString the URL to validate
     * @return {@code true} if the URL passes all security checks, {@code false} otherwise
     */
    public static boolean isSafe(String urlString) {
        return validate(urlString).isValid();
    }

    private static boolean isKnownWebhookDomain(String host) {
        for (String domain : KNOWN_WEBHOOK_DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a host string represents an IP address (IPv4 or IPv6).
     * Uses InetAddress for accurate detection instead of regex.
     */
    private static boolean isIpAddress(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            // InetAddress.getByName() will parse the IP without DNS lookup
            // for valid IP address formats
            InetAddress addr = InetAddress.getByName(host);
            // Verify it's actually an IP by checking if the string representation matches
            return addr.getHostAddress().equalsIgnoreCase(host) ||
                   // Handle IPv6 formats that may differ in representation
                   isNumericIpFormat(host);
        } catch (UnknownHostException e) {
            // If it can't be parsed, check if it looks like an IP
            return isNumericIpFormat(host);
        }
    }

    /**
     * Checks if the string looks like a numeric IP format.
     */
    private static boolean isNumericIpFormat(String host) {
        // IPv4: digits and dots only
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return true;
        }
        // IPv6: contains colon (simplified check)
        if (host.contains(":")) {
            // Valid IPv6 chars: hex digits, colons, and dots (for IPv4-mapped)
            return host.matches("^[0-9a-fA-F:.]+$");
        }
        return false;
    }

    private static boolean isPrivateOrReservedIp(String ip) {
        if (BLOCKED_HOSTS.contains(ip)) {
            return true;
        }

        // Use InetAddress for proper IP parsing and checking
        try {
            InetAddress addr = InetAddress.getByName(ip);

            // Check using InetAddress built-in methods
            if (isPrivateOrLoopback(addr)) {
                return true;
            }

            // Additional check for IPv4-mapped IPv6 addresses (::ffff:x.x.x.x)
            // These can bypass string-based checks
            if (addr instanceof java.net.Inet6Address) {
                java.net.Inet6Address ipv6 = (java.net.Inet6Address) addr;
                // Check if it's an IPv4-mapped or IPv4-compatible address
                if (ipv6.isIPv4CompatibleAddress()) {
                    // Extract the IPv4 part and check it
                    byte[] bytes = ipv6.getAddress();
                    // IPv4 part is in the last 4 bytes
                    String ipv4 = String.format("%d.%d.%d.%d",
                            bytes[12] & 0xFF, bytes[13] & 0xFF,
                            bytes[14] & 0xFF, bytes[15] & 0xFF);
                    return isPrivateOrReservedIp(ipv4);
                }
            }

            return false;
        } catch (UnknownHostException e) {
            // Fall back to pattern matching for unparseable addresses
            if (PRIVATE_IPV4_PATTERN.matcher(ip).matches()) {
                return true;
            }
            if (LINK_LOCAL_PATTERN.matcher(ip).matches()) {
                return true;
            }
            // Check for IPv6 private ranges
            String lowerIp = ip.toLowerCase();
            if (lowerIp.startsWith("fc") || lowerIp.startsWith("fd") ||
                lowerIp.startsWith("fe80:") || lowerIp.startsWith("::ffff:")) {
                return true;
            }
            return false;
        }
    }

    private static boolean isPrivateOrLoopback(InetAddress addr) {
        return addr.isLoopbackAddress() ||
               addr.isLinkLocalAddress() ||
               addr.isSiteLocalAddress() ||
               addr.isAnyLocalAddress();
    }

    /**
     * Result of URL validation containing validity status and error details.
     *
     * @param isValid      {@code true} if the URL passed all security checks
     * @param errorMessage description of why validation failed, or {@code null} if valid
     */
    public record ValidationResult(boolean isValid, String errorMessage) {

        /**
         * Creates a successful validation result.
         *
         * @return a valid result with no error message
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates a failed validation result.
         *
         * @param message description of why validation failed
         * @return an invalid result with the error message
         */
        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
