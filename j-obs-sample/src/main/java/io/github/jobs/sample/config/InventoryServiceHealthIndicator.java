package io.github.jobs.sample.config;

import io.github.jobs.sample.service.InventoryService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom health indicator for the inventory service.
 * Demonstrates application-specific health checks with business metrics.
 */
@Component("inventory")
public class InventoryServiceHealthIndicator implements HealthIndicator {

    private final InventoryService inventoryService;

    public InventoryServiceHealthIndicator(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public Health health() {
        try {
            int totalInventory = inventoryService.getTotalInventory();
            Map<String, Integer> lowStockItems = inventoryService.getLowStockItems(20);
            int responseTime = ThreadLocalRandom.current().nextInt(5, 30);

            // Warning if too many low stock items
            if (lowStockItems.size() > 3) {
                return Health.status("WARNING")
                        .withDetail("status", "Low stock warning")
                        .withDetail("totalInventory", totalInventory)
                        .withDetail("lowStockItemCount", lowStockItems.size())
                        .withDetail("lowStockItems", lowStockItems)
                        .withDetail("threshold", 20)
                        .withDetail("responseTime", responseTime + "ms")
                        .build();
            }

            return Health.up()
                    .withDetail("totalInventory", totalInventory)
                    .withDetail("lowStockItemCount", lowStockItems.size())
                    .withDetail("productsTracked", 8)
                    .withDetail("responseTime", responseTime + "ms")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}
