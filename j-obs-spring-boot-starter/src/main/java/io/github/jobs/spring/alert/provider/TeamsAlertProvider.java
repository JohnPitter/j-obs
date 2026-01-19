package io.github.jobs.spring.alert.provider;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertSeverity;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Teams;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Alert notification provider for Microsoft Teams using incoming webhooks.
 *
 * <p>Sends alert notifications to Teams channels using webhook connectors.
 * Messages are formatted as MessageCard with color-coded theme.</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     providers:
 *       teams:
 *         enabled: true
 *         webhook-url: ${TEAMS_WEBHOOK_URL}
 * }</pre>
 *
 * <h2>Message Format</h2>
 * <p>Uses Microsoft's MessageCard format with:</p>
 * <ul>
 *   <li>Theme color based on severity (red, orange, blue)</li>
 *   <li>Activity title with status emoji and alert name</li>
 *   <li>Facts section for severity, status, and time</li>
 *   <li>Message text with Markdown support</li>
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
public class TeamsAlertProvider extends AbstractAlertProvider {

    private final Teams config;

    /**
     * Creates a new Teams provider with default 30 second timeout.
     *
     * @param config the Teams configuration containing webhook URL
     */
    public TeamsAlertProvider(Teams config) {
        this(config, Duration.ofSeconds(30));
    }

    /**
     * Creates a new Teams provider with custom timeout.
     *
     * @param config the Teams configuration containing webhook URL
     * @param timeout the HTTP request timeout
     */
    public TeamsAlertProvider(Teams config, Duration timeout) {
        super(timeout);
        this.config = config;
    }

    @Override
    public String getName() {
        return "teams";
    }

    @Override
    public String getDisplayName() {
        return "Microsoft Teams";
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
            log.warn("Teams webhook URL validation failed: {}", urlValidation.errorMessage());
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), "Invalid webhook URL: " + urlValidation.errorMessage())
            );
        }

        String payload = buildAdaptiveCard(event);
        return sendHttpPost(webhookUrl, payload, "application/json");
    }

    private String buildAdaptiveCard(AlertEvent event) {
        String themeColor = getThemeColor(event.severity());

        return """
            {
                "@type": "MessageCard",
                "@context": "http://schema.org/extensions",
                "themeColor": "%s",
                "summary": "%s",
                "sections": [{
                    "activityTitle": "%s %s",
                    "facts": [
                        {"name": "Severity", "value": "%s"},
                        {"name": "Status", "value": "%s"},
                        {"name": "Time", "value": "%s"}
                    ],
                    "markdown": true,
                    "text": "%s"
                }]
            }
            """.formatted(
                themeColor,
                escapeJson(event.alertName()),
                getStatusEmoji(event),
                escapeJson(event.alertName()),
                event.severity().displayName(),
                event.status().displayName(),
                formatTimestamp(event),
                escapeJson(event.message())
        );
    }

    private String getThemeColor(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "FF0000";
            case WARNING -> "FFA500";
            case INFO -> "0078D7";
        };
    }
}
