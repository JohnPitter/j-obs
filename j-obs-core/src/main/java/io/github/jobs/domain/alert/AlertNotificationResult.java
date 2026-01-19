package io.github.jobs.domain.alert;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of sending an alert notification to a provider.
 */
public record AlertNotificationResult(
        String providerName,
        boolean success,
        String message,
        Instant sentAt,
        String errorDetails
) {
    public AlertNotificationResult {
        Objects.requireNonNull(providerName, "providerName cannot be null");
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

    public static AlertNotificationResult success(String providerName) {
        return new AlertNotificationResult(providerName, true, "Notification sent successfully", Instant.now(), null);
    }

    public static AlertNotificationResult success(String providerName, String message) {
        return new AlertNotificationResult(providerName, true, message, Instant.now(), null);
    }

    public static AlertNotificationResult failure(String providerName, String errorDetails) {
        return new AlertNotificationResult(providerName, false, "Failed to send notification", Instant.now(), errorDetails);
    }

    public static AlertNotificationResult failure(String providerName, String message, String errorDetails) {
        return new AlertNotificationResult(providerName, false, message, Instant.now(), errorDetails);
    }

    /**
     * Merges two results, combining success states.
     */
    public AlertNotificationResult merge(AlertNotificationResult other) {
        boolean combinedSuccess = this.success && other.success;
        String combinedMessage = this.success && other.success
                ? "All notifications sent successfully"
                : "Some notifications failed";
        String combinedErrors = null;
        if (!this.success || !other.success) {
            StringBuilder sb = new StringBuilder();
            if (this.errorDetails != null) {
                sb.append(this.providerName).append(": ").append(this.errorDetails);
            }
            if (other.errorDetails != null) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(other.providerName).append(": ").append(other.errorDetails);
            }
            combinedErrors = sb.toString();
        }
        return new AlertNotificationResult(
                this.providerName + "," + other.providerName,
                combinedSuccess,
                combinedMessage,
                Instant.now(),
                combinedErrors
        );
    }
}
