package io.github.jobs.samples.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway for the microservices sample.
 *
 * Routes requests to downstream services while propagating trace context.
 * Access the dashboard at http://localhost:8084/j-obs
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
