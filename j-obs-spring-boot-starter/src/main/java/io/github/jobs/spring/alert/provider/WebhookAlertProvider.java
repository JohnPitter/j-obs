package io.github.jobs.spring.alert.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Webhook;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic webhook alert provider with customizable templates and headers.
 *
 * <p>Sends alert notifications to any HTTP endpoint using configurable
 * request templates, headers, and HTTP methods (POST, PUT, PATCH).</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     providers:
 *       webhook:
 *         enabled: true
 *         url: https://my-service.com/alerts
 *         method: POST
 *         headers:
 *           Authorization: "Bearer ${WEBHOOK_TOKEN}"
 *           X-Custom-Header: "value"
 *         template: |
 *           {
 *             "alert": "{{name}}",
 *             "severity": "{{severity}}",
 *             "message": "{{message}}"
 *           }
 * }</pre>
 *
 * <h2>Template Variables</h2>
 * <ul>
 *   <li>{@code {{id}}} - Unique event ID</li>
 *   <li>{@code {{alertId}}} - Alert rule ID</li>
 *   <li>{@code {{name}}} - Alert name</li>
 *   <li>{@code {{severity}}} / {@code {{severityDisplay}}} - Severity level</li>
 *   <li>{@code {{status}}} / {@code {{statusDisplay}}} - Alert status</li>
 *   <li>{@code {{message}}} - Alert message (JSON escaped)</li>
 *   <li>{@code {{timestamp}}} / {@code {{timestampFormatted}}} - Event time</li>
 *   <li>{@code {{labels}}} - Labels as JSON object</li>
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
public class WebhookAlertProvider extends AbstractAlertProvider {

    private final Webhook config;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new webhook provider with default 30 second timeout.
     *
     * @param config the webhook configuration containing URL, headers, and template
     */
    public WebhookAlertProvider(Webhook config) {
        this(config, Duration.ofSeconds(30));
    }

    /**
     * Creates a new webhook provider with custom timeout.
     *
     * @param config the webhook configuration containing URL, headers, and template
     * @param timeout the HTTP request timeout
     */
    public WebhookAlertProvider(Webhook config, Duration timeout) {
        super(timeout);
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "webhook";
    }

    @Override
    public String getDisplayName() {
        return "Webhook";
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
        try {
            // Validate URL to prevent SSRF
            String webhookUrl = config.getUrl();
            UrlValidator.ValidationResult urlValidation = UrlValidator.validate(webhookUrl);
            if (!urlValidation.isValid()) {
                log.warn("Webhook URL validation failed: {}", urlValidation.errorMessage());
                return CompletableFuture.completedFuture(
                        AlertNotificationResult.failure(getName(), "Invalid webhook URL: " + urlValidation.errorMessage())
                );
            }

            String body = buildRequestBody(event);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(timeout)
                    .header("Content-Type", "application/json");

            // Add custom headers
            Map<String, String> headers = config.getHeaders();
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }

            // Set HTTP method
            HttpRequest request = switch (config.getMethod().toUpperCase()) {
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                case "PATCH" -> requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
                default -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            };

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
                        log.error("Webhook request failed: {}", ex.getMessage());
                        return AlertNotificationResult.failure(getName(), ex.getMessage());
                    });

        } catch (Exception e) {
            log.error("Failed to send webhook: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), e.getMessage())
            );
        }
    }

    private String buildRequestBody(AlertEvent event) {
        String template = config.getTemplate();

        if (template != null && !template.isBlank()) {
            return applyTemplate(template, event);
        }

        // Default JSON payload
        return buildDefaultPayload(event);
    }

    private String applyTemplate(String template, AlertEvent event) {
        return template
                .replace("{{id}}", event.id())
                .replace("{{alertId}}", event.alertId())
                .replace("{{name}}", event.alertName())
                .replace("{{severity}}", event.severity().name())
                .replace("{{severityDisplay}}", event.severity().displayName())
                .replace("{{status}}", event.status().name())
                .replace("{{statusDisplay}}", event.status().displayName())
                .replace("{{message}}", escapeJson(event.message()))
                .replace("{{timestamp}}", event.firedAt().toString())
                .replace("{{timestampFormatted}}", formatTimestamp(event))
                .replace("{{labels}}", labelsToJson(event.labels()));
    }

    private String buildDefaultPayload(AlertEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", event.id());
        payload.put("alertId", event.alertId());
        payload.put("alertName", event.alertName());
        payload.put("severity", event.severity().name());
        payload.put("status", event.status().name());
        payload.put("message", event.message());
        payload.put("timestamp", event.firedAt().toString());
        payload.put("labels", event.labels() != null ? event.labels() : Map.of());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload", e);
            // Fallback to minimal JSON
            return "{\"error\":\"serialization_failed\",\"alertName\":\"" +
                   escapeJson(event.alertName()) + "\"}";
        }
    }

    private String labelsToJson(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize labels", e);
            return "{}";
        }
    }
}
