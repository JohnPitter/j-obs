package io.github.jobs.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sample application demonstrating J-Obs observability features.
 *
 * <p>After starting the application:</p>
 * <ul>
 *   <li>Application API: http://localhost:8080/api/orders</li>
 *   <li>J-Obs Dashboard: http://localhost:8080/j-obs</li>
 *   <li>Health: http://localhost:8080/j-obs/health</li>
 *   <li>Traces: http://localhost:8080/j-obs/traces</li>
 *   <li>Logs: http://localhost:8080/j-obs/logs</li>
 *   <li>Metrics: http://localhost:8080/j-obs/metrics</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
