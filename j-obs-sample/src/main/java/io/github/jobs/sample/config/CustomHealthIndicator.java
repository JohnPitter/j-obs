package io.github.jobs.sample.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom health indicator for demonstration.
 * Simulates a database connection with occasional issues.
 */
@Component("database")
public class CustomHealthIndicator implements HealthIndicator {

    private final AtomicInteger checkCount = new AtomicInteger(0);

    @Override
    public Health health() {
        int count = checkCount.incrementAndGet();

        // Simulate occasional database issues (every 10th check has 30% chance of being DOWN)
        if (count % 10 == 0 && ThreadLocalRandom.current().nextDouble() < 0.3) {
            return Health.down()
                    .withDetail("error", "Connection timeout after 30000ms")
                    .withDetail("database", "PostgreSQL")
                    .withDetail("host", "localhost:5432")
                    .withDetail("checkCount", count)
                    .build();
        }

        // Normal healthy state
        return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("host", "localhost:5432")
                .withDetail("connectionPool", "active=5, idle=10, max=20")
                .withDetail("responseTime", ThreadLocalRandom.current().nextInt(1, 15) + "ms")
                .withDetail("checkCount", count)
                .build();
    }
}
