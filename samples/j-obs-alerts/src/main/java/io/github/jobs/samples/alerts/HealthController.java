package io.github.jobs.samples.alerts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller to manage health status for alert testing.
 */
@RestController
@RequestMapping("/api/health-control")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final AlertHealthIndicator healthIndicator;

    public HealthController(AlertHealthIndicator healthIndicator) {
        this.healthIndicator = healthIndicator;
    }

    @PostMapping("/set-down")
    public Map<String, Object> setHealthDown() {
        log.warn("Setting service health to DOWN for alert testing");
        healthIndicator.setHealthy(false);
        healthIndicator.setStatus("Service degraded - simulated failure");
        return Map.of(
            "message", "Health set to DOWN",
            "alertExpected", "health-down alert should fire after duration threshold"
        );
    }

    @PostMapping("/set-up")
    public Map<String, Object> setHealthUp() {
        log.info("Setting service health to UP");
        healthIndicator.setHealthy(true);
        healthIndicator.setStatus("All systems operational");
        return Map.of(
            "message", "Health set to UP",
            "note", "Any active health alerts should resolve"
        );
    }

    @PostMapping("/set-status/{status}")
    public Map<String, Object> setStatus(@PathVariable String status) {
        log.info("Setting health status message to: {}", status);
        healthIndicator.setStatus(status);
        return Map.of(
            "message", "Status updated",
            "newStatus", status
        );
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "healthy", healthIndicator.isHealthy(),
            "health", healthIndicator.health().getDetails()
        );
    }
}
