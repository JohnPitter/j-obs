package io.github.jobs.samples.slo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates traffic patterns for SLO demonstration.
 * Generates consistent request load to show SLO metrics over time.
 */
@Component
public class TrafficSimulator {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulator.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public TrafficSimulator(@Value("${server.port:8088}") int port) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = "http://localhost:" + port;
    }

    /**
     * Generates traffic every second when enabled.
     */
    @Scheduled(fixedRate = 1000)
    public void generateTraffic() {
        if (!enabled.get()) {
            return;
        }

        try {
            // Generate 5-10 requests per second
            int requestCount = 5 + (int) (Math.random() * 6);

            for (int i = 0; i < requestCount; i++) {
                try {
                    restTemplate.getForEntity(baseUrl + "/api/orders", String.class);
                } catch (Exception e) {
                    // Expected for simulated errors, no need to log
                }
            }
        } catch (Exception e) {
            log.debug("Traffic generation error: {}", e.getMessage());
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        log.info("Traffic simulator {}", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return enabled.get();
    }
}
