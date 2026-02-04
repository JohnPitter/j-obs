package io.github.jobs.samples.alerts;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controllable health indicator for testing health-based alerts.
 */
@Component("simulatedService")
public class AlertHealthIndicator implements HealthIndicator {

    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicReference<String> status = new AtomicReference<>("All systems operational");

    @Override
    public Health health() {
        if (healthy.get()) {
            return Health.up()
                .withDetail("status", status.get())
                .withDetail("lastCheck", System.currentTimeMillis())
                .build();
        } else {
            return Health.down()
                .withDetail("status", status.get())
                .withDetail("lastCheck", System.currentTimeMillis())
                .build();
        }
    }

    public void setHealthy(boolean isHealthy) {
        this.healthy.set(isHealthy);
    }

    public void setStatus(String newStatus) {
        this.status.set(newStatus);
    }

    public boolean isHealthy() {
        return healthy.get();
    }
}
