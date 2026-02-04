package io.github.jobs.samples.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Service for the microservices sample.
 *
 * Manages product inventory and stock levels.
 * Access the dashboard at http://localhost:8086/j-obs
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
