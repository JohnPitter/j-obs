package io.github.jobs.sample.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom health indicator for disk space monitoring simulation.
 * Demonstrates storage infrastructure health checks.
 */
@Component("diskSpace")
public class DiskSpaceHealthIndicator implements HealthIndicator {

    // Simulate slowly increasing disk usage
    private double usedPercentage = 45.0;

    @Override
    public Health health() {
        // Simulate disk usage fluctuation
        usedPercentage += ThreadLocalRandom.current().nextDouble(-2, 3);
        usedPercentage = Math.max(30, Math.min(95, usedPercentage));

        long totalSpace = 500L * 1024 * 1024 * 1024; // 500 GB
        long usedSpace = (long) (totalSpace * (usedPercentage / 100));
        long freeSpace = totalSpace - usedSpace;

        // Warning if > 80% used
        if (usedPercentage > 80) {
            return Health.status("WARNING")
                    .withDetail("path", "/var/lib/app")
                    .withDetail("total", formatBytes(totalSpace))
                    .withDetail("used", formatBytes(usedSpace))
                    .withDetail("free", formatBytes(freeSpace))
                    .withDetail("usedPercent", String.format("%.1f%%", usedPercentage))
                    .withDetail("warning", "Disk space running low")
                    .withDetail("threshold", "80%")
                    .build();
        }

        return Health.up()
                .withDetail("path", "/var/lib/app")
                .withDetail("total", formatBytes(totalSpace))
                .withDetail("used", formatBytes(usedSpace))
                .withDetail("free", formatBytes(freeSpace))
                .withDetail("usedPercent", String.format("%.1f%%", usedPercentage))
                .withDetail("threshold", "80%")
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024L * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f KB", bytes / 1024.0);
        }
    }
}
