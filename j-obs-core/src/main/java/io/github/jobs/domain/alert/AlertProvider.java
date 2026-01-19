package io.github.jobs.domain.alert;

import java.util.concurrent.CompletableFuture;

/**
 * SPI interface for alert notification providers.
 * Implementations should be registered via ServiceLoader or Spring beans.
 */
public interface AlertProvider {

    /**
     * Returns the unique name of this provider.
     */
    String getName();

    /**
     * Returns a human-readable display name.
     */
    default String getDisplayName() {
        return getName();
    }

    /**
     * Checks if this provider is properly configured and ready to send notifications.
     */
    boolean isConfigured();

    /**
     * Checks if this provider is enabled.
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
