package io.github.jobs.sample.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom health indicator for Kafka message broker simulation.
 * Demonstrates messaging infrastructure health monitoring.
 */
@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    private final AtomicInteger checkCount = new AtomicInteger(0);

    @Override
    public Health health() {
        int count = checkCount.incrementAndGet();
        int responseTime = ThreadLocalRandom.current().nextInt(1, 20);

        // Simulate occasional broker issues (5% chance)
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            return Health.status("DEGRADED")
                    .withDetail("cluster", "kafka-cluster-1")
                    .withDetail("brokers", "2/3 available")
                    .withDetail("warning", "Broker kafka-2 is unreachable")
                    .withDetail("topics", 12)
                    .withDetail("partitions", 36)
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("checkCount", count)
                    .build();
        }

        return Health.up()
                .withDetail("cluster", "kafka-cluster-1")
                .withDetail("brokers", "3/3 available")
                .withDetail("bootstrapServers", "localhost:9092,localhost:9093,localhost:9094")
                .withDetail("topics", 12)
                .withDetail("partitions", 36)
                .withDetail("consumerGroups", 5)
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("checkCount", count)
                .build();
    }
}
