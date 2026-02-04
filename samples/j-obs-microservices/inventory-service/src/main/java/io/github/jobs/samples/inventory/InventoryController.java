package io.github.jobs.samples.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inventory controller managing product stock levels.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final Map<String, Map<String, Object>> inventory = new ConcurrentHashMap<>();

    public InventoryController() {
        // Initialize with sample inventory
        addProduct("PROD-001", "Wireless Headphones", 50, new BigDecimal("79.99"));
        addProduct("PROD-002", "USB-C Cable", 200, new BigDecimal("14.99"));
        addProduct("PROD-003", "Laptop Stand", 30, new BigDecimal("49.99"));
        addProduct("PROD-004", "Mechanical Keyboard", 25, new BigDecimal("129.99"));
        addProduct("PROD-005", "Monitor Light Bar", 15, new BigDecimal("89.99"));
    }

    @GetMapping
    public List<Map<String, Object>> getAllInventory() {
        log.info("Inventory Service: Getting all inventory items");
        simulateLatency();
        return new ArrayList<>(inventory.values());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getInventoryItem(@PathVariable String productId) {
        log.info("Inventory Service: Getting inventory for product {}", productId);
        simulateLatency();

        Map<String, Object> item = inventory.get(productId);
        if (item == null) {
            log.warn("Inventory Service: Product {} not found", productId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Map<String, Object>> reserveStock(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {

        Integer quantity = (Integer) request.get("quantity");
        log.info("Inventory Service: Reserving {} units of product {}", quantity, productId);
        simulateLatency();

        Map<String, Object> item = inventory.get(productId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        Integer currentStock = (Integer) item.get("quantity");
        if (currentStock < quantity) {
            log.warn("Inventory Service: Insufficient stock for product {}", productId);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Insufficient stock",
                "available", currentStock,
                "requested", quantity
            ));
        }

        // Reserve stock
        item.put("quantity", currentStock - quantity);
        log.info("Inventory Service: Reserved {} units, {} remaining", quantity, currentStock - quantity);

        return ResponseEntity.ok(Map.of(
            "productId", productId,
            "reserved", quantity,
            "remaining", currentStock - quantity
        ));
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<Map<String, Object>> restockProduct(
            @PathVariable String productId,
            @RequestBody Map<String, Object> request) {

        Integer quantity = (Integer) request.get("quantity");
        log.info("Inventory Service: Restocking {} units of product {}", quantity, productId);

        Map<String, Object> item = inventory.get(productId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }

        Integer currentStock = (Integer) item.get("quantity");
        item.put("quantity", currentStock + quantity);
        log.info("Inventory Service: Restocked, new quantity: {}", currentStock + quantity);

        return ResponseEntity.ok(Map.of(
            "productId", productId,
            "added", quantity,
            "newQuantity", currentStock + quantity
        ));
    }

    private void addProduct(String productId, String name, int quantity, BigDecimal price) {
        Map<String, Object> product = new HashMap<>();
        product.put("productId", productId);
        product.put("name", name);
        product.put("quantity", quantity);
        product.put("price", price);
        product.put("warehouse", "MAIN");
        inventory.put(productId, product);
    }

    private void simulateLatency() {
        // Simulate realistic database/cache latency
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
