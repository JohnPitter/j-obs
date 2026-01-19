package io.github.jobs.spring.alert.provider;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertSeverity;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Slack;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Alert notification provider for Slack using incoming webhooks.
 *
 * <p>Sends alert notifications to Slack channels using webhook URLs.
 * Messages are formatted as rich attachments with color-coded severity.</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     providers:
 *       slack:
 *         enabled: true
 *         webhook-url: ${SLACK_WEBHOOK_URL}
 *         channel: "#alerts"
 * }</pre>
 *
 * <h2>Message Format</h2>
 * <p>Uses Slack's attachment format with:</p>
 * <ul>
 *   <li>Color-coded sidebar (red=critical, orange=warning, green=info)</li>
 *   <li>Title with status emoji and alert name</li>
 *   <li>Fields for severity, status, time, and labels</li>
 *   <li>Footer with timestamp</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>Webhook URLs are validated against SSRF attacks before sending.</p>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AbstractAlertProvider
 * @see UrlValidator
 */
public class SlackAlertProvider extends AbstractAlertProvider {

    private final Slack config;

    /**
     * Creates a new Slack provider with default 30 second timeout.
     *
     * @param config the Slack configuration containing webhook URL and channel
     */
    public SlackAlertProvider(Slack config) {
        this(config, Duration.ofSeconds(30));
    }

    /**
     * Creates a new Slack provider with custom timeout.
     *
     * @param config the Slack configuration containing webhook URL and channel
     * @param timeout the HTTP request timeout
     */
    public SlackAlertProvider(Slack config, Duration timeout) {
        super(timeout);
        this.config = config;
    }

    @Override
    public String getName() {
        return "slack";
    }

    @Override
    public String getDisplayName() {
        return "Slack";
    }

    @Override
    public boolean isConfigured() {
        return config.isConfigured();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    protected CompletableFuture<AlertNotificationResult> doSend(AlertEvent event) {
        // Validate URL to prevent SSRF
        String webhookUrl = config.getWebhookUrl();
        UrlValidator.ValidationResult urlValidation = UrlValidator.validate(webhookUrl);
        if (!urlValidation.isValid()) {
            log.warn("Slack webhook URL validation failed: {}", urlValidation.errorMessage());
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), "Invalid webhook URL: " + urlValidation.errorMessage())
            );
        }

        String payload = buildSlackPayload(event);
        return sendHttpPost(webhookUrl, payload, "application/json");
    }

    private String buildSlackPayload(AlertEvent event) {
        String color = getSlackColor(event.severity());
        String channel = config.getChannel();

        StringBuilder payload = new StringBuilder();
        payload.append("{");

        if (channel != null && !channel.isBlank()) {
            payload.append("\"channel\":\"").append(escapeJson(channel)).append("\",");
        }

        payload.append("\"attachments\":[{");
        payload.append("\"color\":\"").append(color).append("\",");
        payload.append("\"title\":\"").append(getStatusEmoji(event)).append(" ")
                .append(escapeJson(event.alertName())).append("\",");
        payload.append("\"text\":\"").append(escapeJson(event.message())).append("\",");
        payload.append("\"fields\":[");
        payload.append("{\"title\":\"Severity\",\"value\":\"").append(event.severity().displayName())
                .append("\",\"short\":true},");
        payload.append("{\"title\":\"Status\",\"value\":\"").append(event.status().displayName())
                .append("\",\"short\":true},");
        payload.append("{\"title\":\"Time\",\"value\":\"").append(formatTimestamp(event))
                .append("\",\"short\":true}");

        // Add labels as additional fields
        if (!event.labels().isEmpty()) {
            event.labels().forEach((key, value) -> {
                payload.append(",{\"title\":\"").append(escapeJson(key))
                        .append("\",\"value\":\"").append(escapeJson(value))
                        .append("\",\"short\":true}");
            });
        }

        payload.append("],");
        payload.append("\"footer\":\"J-Obs Alert System\",");
        payload.append("\"ts\":").append(event.firedAt().getEpochSecond());
        payload.append("}]}");

        return payload.toString();
    }

    private String getSlackColor(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "danger";
            case WARNING -> "warning";
            case INFO -> "good";
        };
    }
}
