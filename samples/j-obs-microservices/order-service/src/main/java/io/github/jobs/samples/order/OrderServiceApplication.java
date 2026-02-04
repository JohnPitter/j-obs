package io.github.jobs.samples.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service for the microservices sample.
 *
 * Manages orders and calls inventory-service for stock checks.
 * Access the dashboard at http://localhost:8085/j-obs
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
