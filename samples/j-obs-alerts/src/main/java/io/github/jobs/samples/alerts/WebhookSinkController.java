package io.github.jobs.samples.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Mock webhook sink for testing webhook alerts locally.
 * In production, this would be an external service like PagerDuty, Opsgenie, etc.
 */
@RestController
@RequestMapping("/api/webhook-sink")
public class WebhookSinkController {

    private static final Logger log = LoggerFactory.getLogger(WebhookSinkController.class);

    private final List<Map<String, Object>> receivedAlerts = Collections.synchronizedList(new ArrayList<>());

    @PostMapping
    public Map<String, String> receiveWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook received: {}", payload);
        receivedAlerts.add(payload);

        // Keep only last 100 alerts
        while (receivedAlerts.size() > 100) {
            receivedAlerts.remove(0);
        }

        return Map.of("status", "received");
    }

    @PostMapping("/alerts")
    public List<Map<String, Object>> getReceivedAlerts() {
        return new ArrayList<>(receivedAlerts);
    }

    @PostMapping("/clear")
    public Map<String, String> clearAlerts() {
        receivedAlerts.clear();
        return Map.of("status", "cleared");
    }
}
