package io.github.jobs.spring.alert.provider;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertProvider;
import io.github.jobs.domain.alert.AlertSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for alert notification providers.
 *
 * <p>This class provides common functionality for sending alert notifications via HTTP,
 * including HTTP client configuration, message formatting utilities, and error handling.
 * Subclasses implement the {@link #doSend(AlertEvent)} method to define provider-specific
 * sending logic.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Configurable HTTP timeout</li>
 *   <li>Automatic disabled/unconfigured provider handling</li>
 *   <li>Common formatting utilities (emoji, timestamp, escaping)</li>
 *   <li>Async HTTP POST support</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyProvider extends AbstractAlertProvider {
 *     @Override
 *     protected CompletableFuture<AlertNotificationResult> doSend(AlertEvent event) {
 *         String message = formatMessage(event);
 *         return sendHttpPost(webhookUrl, message, "application/json");
 *     }
 * }
 * }</pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertProvider
 * @see TelegramAlertProvider
 * @see SlackAlertProvider
 * @see TeamsAlertProvider
 */
public abstract class AbstractAlertProvider implements AlertProvider {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final HttpClient httpClient;
    protected final Duration timeout;

    protected static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Creates a new provider with the default timeout of 30 seconds.
     */
    protected AbstractAlertProvider() {
        this(Duration.ofSeconds(30));
    }

    /**
     * Creates a new provider with the specified timeout.
     *
     * @param timeout the HTTP connection and request timeout
     */
    protected AbstractAlertProvider(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public CompletableFuture<AlertNotificationResult> send(AlertEvent event) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), "Provider is disabled")
            );
        }

        if (!isConfigured()) {
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), "Provider is not configured")
            );
        }

        return doSend(event);
    }

    /**
     * Implements the actual sending logic for this provider.
     *
     * <p>This method is called after verifying the provider is enabled and configured.
     * Implementations should handle message formatting and sending to the target service.</p>
     *
     * @param event the alert event to send
     * @return a future containing the notification result
     */
    protected abstract CompletableFuture<AlertNotificationResult> doSend(AlertEvent event);

    /**
     * Sends an HTTP POST request asynchronously.
     *
     * <p>This utility method handles the HTTP communication, including error handling
     * and response status code validation. Success is any 2xx status code.</p>
     *
     * @param url the target URL
     * @param body the request body content
     * @param contentType the Content-Type header value (e.g., "application/json")
     * @return a future containing the notification result
     */
    protected CompletableFuture<AlertNotificationResult> sendHttpPost(String url, String body, String contentType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return AlertNotificationResult.success(getName());
                        } else {
                            return AlertNotificationResult.failure(getName(),
                                    "HTTP " + response.statusCode() + ": " + response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to send notification via {}: {}", getName(), ex.getMessage());
                        return AlertNotificationResult.failure(getName(), ex.getMessage());
                    });
        } catch (Exception e) {
            log.error("Failed to create request for {}: {}", getName(), e.getMessage());
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), e.getMessage())
            );
        }
    }

    /**
     * Returns an emoji character representing the alert severity.
     *
     * @param severity the alert severity level
     * @return an emoji string: 🔴 for CRITICAL, ⚠️ for WARNING, ℹ️ for INFO
     */
    protected String getSeverityEmoji(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "\uD83D\uDD34"; // Red circle
            case WARNING -> "\u26A0\uFE0F";   // Warning sign
            case INFO -> "\u2139\uFE0F";       // Info sign
        };
    }

    /**
     * Returns an emoji character representing the alert status.
     *
     * @param event the alert event
     * @return an emoji string: 🔥 for FIRING, 👀 for ACKNOWLEDGED, ✅ for RESOLVED
     */
    protected String getStatusEmoji(AlertEvent event) {
        return switch (event.status()) {
            case FIRING -> "\uD83D\uDD25";      // Fire
            case ACKNOWLEDGED -> "\uD83D\uDC40"; // Eyes
            case RESOLVED -> "\u2705";           // Check mark
        };
    }

    /**
     * Formats the event timestamp using the system timezone.
     *
     * @param event the alert event
     * @return the formatted timestamp in "yyyy-MM-dd HH:mm:ss" format
     */
    protected String formatTimestamp(AlertEvent event) {
        return TIME_FORMATTER.format(event.firedAt());
    }

    /**
     * Escapes special characters for safe inclusion in JSON strings.
     *
     * <p>Handles backslashes, quotes, and control characters (newline, carriage return, tab).</p>
     *
     * @param text the text to escape, may be null
     * @return the escaped text, or empty string if input is null
     */
    protected String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Escapes special Markdown formatting characters.
     *
     * <p>Handles asterisks, underscores, backticks, and brackets that would
     * otherwise be interpreted as Markdown formatting.</p>
     *
     * @param text the text to escape, may be null
     * @return the escaped text, or empty string if input is null
     */
    protected String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }
}
