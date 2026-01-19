package io.github.jobs.sample.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom health indicator for Redis cache simulation.
 */
@Component("cache")
public class CacheHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        int responseTime = ThreadLocalRandom.current().nextInt(1, 5);

        return Health.up()
                .withDetail("type", "Redis")
                .withDetail("host", "localhost:6379")
                .withDetail("version", "7.2.0")
                .withDetail("connectedClients", ThreadLocalRandom.current().nextInt(5, 20))
                .withDetail("usedMemory", ThreadLocalRandom.current().nextInt(100, 500) + "MB")
                .withDetail("responseTime", responseTime + "ms")
                .build();
    }
}
