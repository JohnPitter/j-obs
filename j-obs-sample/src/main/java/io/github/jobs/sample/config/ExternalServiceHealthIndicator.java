package io.github.jobs.sample.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom health indicator for external payment service simulation.
 */
@Component("paymentServiceHealth")
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        int responseTime = ThreadLocalRandom.current().nextInt(50, 300);

        // Simulate occasional high latency warning
        if (responseTime > 200) {
            return Health.status("DEGRADED")
                    .withDetail("service", "payment-service")
                    .withDetail("url", "https://payment-api.example.com")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("warning", "High latency detected")
                    .build();
        }

        return Health.up()
                .withDetail("service", "payment-service")
                .withDetail("url", "https://payment-api.example.com")
                .withDetail("version", "v2")
                .withDetail("responseTime", responseTime + "ms")
                .build();
    }
}
