package io.github.jobs.samples.minimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal J-Obs sample application.
 *
 * This is the quickstart example showing the minimum setup required
 * to get J-Obs working. Access the dashboard at http://localhost:8080/j-obs
 */
@SpringBootApplication
public class MinimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimalApplication.class, args);
    }
}
