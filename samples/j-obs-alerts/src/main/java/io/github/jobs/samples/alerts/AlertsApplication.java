package io.github.jobs.samples.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Alert system J-Obs sample application.
 *
 * Demonstrates J-Obs alert features including:
 * - Telegram notifications
 * - Slack notifications
 * - Email notifications
 * - Webhook notifications
 * - Alert rules (metric, log, health-based)
 * - Alert throttling and grouping
 *
 * Access the dashboard at http://localhost:8083/j-obs
 */
@SpringBootApplication
public class AlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertsApplication.class, args);
    }
}
