package io.github.jobs.samples.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reactive WebFlux J-Obs sample application.
 *
 * Demonstrates J-Obs features in a non-blocking reactive environment.
 * Access the dashboard at http://localhost:8081/j-obs
 */
@SpringBootApplication
public class WebFluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxApplication.class, args);
    }
}
