package io.github.jobs.samples.slo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SLO/SLI tracking J-Obs sample application.
 *
 * Demonstrates J-Obs SLO features including:
 * - SLO definition and tracking
 * - Error budget visualization
 * - Burn rate monitoring
 * - Custom SLIs
 *
 * Access the dashboard at http://localhost:8088/j-obs
 */
@SpringBootApplication
@EnableScheduling
public class SloApplication {

    public static void main(String[] args) {
        SpringApplication.run(SloApplication.class, args);
    }
}
