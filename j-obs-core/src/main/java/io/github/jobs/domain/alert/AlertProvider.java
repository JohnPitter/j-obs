package io.github.jobs.domain.alert;

import java.util.concurrent.CompletableFuture;

/**
 * Service Provider Interface (SPI) for alert notification providers.
 * <p>
 * Implementations of this interface are responsible for delivering alert notifications
 * to specific channels such as Telegram, Slack, Email, Microsoft Teams, or custom webhooks.
 * <p>
 * Providers are automatically discovered and registered by J-Obs when properly configured.
 * Configuration is typically done via application.yml properties.
 * <p>
 * Built-in implementations:
 * <ul>
 *   <li>{@code TelegramAlertProvider} - Sends alerts via Telegram Bot API</li>
 *   <li>{@code SlackAlertProvider} - Sends alerts via Slack webhook</li>
 *   <li>{@code TeamsAlertProvider} - Sends alerts via Microsoft Teams webhook</li>
 *   <li>{@code EmailAlertProvider} - Sends alerts via SMTP email</li>
 *   <li>{@code WebhookAlertProvider} - Sends alerts to custom HTTP endpoints</li>
 * </ul>
 *
 * @see AlertEvent
 * @see AlertNotificationResult
 */
public interface AlertProvider {

    /**
     * Returns the unique identifier name of this provider.
     * <p>
     * This name is used in configuration and for provider selection.
     * Examples: "telegram", "slack", "teams", "email", "webhook"
     *
     * @return the provider name, must be unique among all registered providers
     */
    String getName();

    /**
     * Returns a human-readable display name for UI presentation.
     *
     * @return the display name, defaults to {@link #getName()} if not overridden
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * Checks if this provider is properly configured and ready to send notifications.
     * <p>
     * A provider is configured when all required configuration properties are set.
     * For example, Telegram requires bot token and chat IDs, Slack requires webhook URL.
     *
     * @return true if the provider has all required configuration
     */
    boolean isConfigured();

    /**
     * Checks if this provider is enabled in configuration.
     * <p>
     * A provider can be configured but disabled. Use this to temporarily disable
     * notifications without removing configuration.
     *
     * @return true if the provider is enabled
     */
    boolean isEnabled();

    /**
     * Sends an alert notification asynchronously.
     *
     * @param event the alert event to send
     * @return a CompletableFuture with the result
     */
    CompletableFuture<AlertNotificationResult> send(AlertEvent event);

    /**
     * Tests the provider configuration by sending a test notification.
     *
     * @return a CompletableFuture with the test result
     */
    default CompletableFuture<AlertNotificationResult> test() {
        AlertEvent testEvent = AlertEvent.builder()
                .alertId("test")
                .alertName("Test Alert")
                .severity(AlertSeverity.INFO)
                .message("This is a test notification from J-Obs")
                .build();
        return send(testEvent);
    }
}
