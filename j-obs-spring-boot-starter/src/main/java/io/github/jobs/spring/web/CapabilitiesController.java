package io.github.jobs.spring.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API controller that exposes J-Obs capabilities.
 * <p>
 * This endpoint allows clients to discover which features are available
 * before attempting to use them, enabling graceful degradation when
 * optional dependencies (like WebSocket) are not installed.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/capabilities")
public class CapabilitiesController {

    private final boolean websocketEnabled;

    /**
     * Creates the capabilities controller.
     * <p>
     * Uses ObjectProvider to optionally detect if WebSocket handler is available.
     * This avoids errors when WebSocket dependencies are not on the classpath.
     *
     * @param webSocketHandlerProvider optional WebSocket handler provider
     */
    public CapabilitiesController(
            ObjectProvider<io.github.jobs.spring.websocket.LogWebSocketHandler> webSocketHandlerProvider
    ) {
        // Check if WebSocket handler bean is available
        this.websocketEnabled = webSocketHandlerProvider.getIfAvailable() != null;
    }

    /**
     * Returns the available capabilities of the J-Obs installation.
     * <p>
     * Clients should check this endpoint before attempting to use optional features
     * like WebSocket streaming.
     *
     * @return map of capabilities with their availability status
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CapabilitiesResponse getCapabilities() {
        return new CapabilitiesResponse(
                websocketEnabled,
                true,  // REST API is always available
                true,  // Logs are always available when j-obs is enabled
                true,  // Traces are always available
                true   // Metrics are always available
        );
    }

    /**
     * Response DTO for capabilities endpoint.
     *
     * @param websocketEnabled whether real-time WebSocket streaming is available
     * @param restApiEnabled whether REST API endpoints are available
     * @param logsEnabled whether log collection is enabled
     * @param tracesEnabled whether trace collection is enabled
     * @param metricsEnabled whether metrics collection is enabled
     */
    public record CapabilitiesResponse(
            boolean websocketEnabled,
            boolean restApiEnabled,
            boolean logsEnabled,
            boolean tracesEnabled,
            boolean metricsEnabled
    ) {
        /**
         * Returns capabilities as a simple map for easy JSON serialization.
         */
        public Map<String, Boolean> toMap() {
            return Map.of(
                    "websocket", websocketEnabled,
                    "restApi", restApiEnabled,
                    "logs", logsEnabled,
                    "traces", tracesEnabled,
                    "metrics", metricsEnabled
            );
        }
    }
}
