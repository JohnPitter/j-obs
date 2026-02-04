package io.github.jobs.samples.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Order controller managing order CRUD operations.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;
    private final String inventoryServiceUrl;
    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();
    private final AtomicLong orderIdCounter = new AtomicLong(1000);

    public OrderController(
            RestTemplate restTemplate,
            @Value("${services.inventory.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.inventoryServiceUrl = inventoryServiceUrl;

        // Initialize with sample orders
        createSampleOrder("PROD-001", 2, "John Doe");
        createSampleOrder("PROD-002", 1, "Jane Smith");
    }

    @GetMapping
    public List<Map<String, Object>> getAllOrders() {
        log.info("Order Service: Getting all orders");
        return new ArrayList<>(orders.values());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        log.info("Order Service: Getting order {}", id);
        Map<String, Object> order = orders.get(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String productId = (String) orderRequest.get("productId");
        Integer quantity = (Integer) orderRequest.get("quantity");
        String customerName = (String) orderRequest.get("customerName");

        log.info("Order Service: Creating order for product {} quantity {}", productId, quantity);

        // Check inventory
        boolean stockAvailable = checkInventory(productId, quantity);
        if (!stockAvailable) {
            log.warn("Order Service: Insufficient stock for product {}", productId);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Insufficient stock",
                "productId", productId,
                "requestedQuantity", quantity
            ));
        }

        // Create order
        String orderId = "ORD-" + orderIdCounter.incrementAndGet();
        Map<String, Object> order = new HashMap<>();
        order.put("id", orderId);
        order.put("productId", productId);
        order.put("quantity", quantity);
        order.put("customerName", customerName);
        order.put("status", "CONFIRMED");
        order.put("createdAt", LocalDateTime.now().toString());
        order.put("total", BigDecimal.valueOf(quantity * 29.99));

        orders.put(orderId, order);
        log.info("Order Service: Created order {}", orderId);

        return ResponseEntity.ok(order);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable String id) {
        log.info("Order Service: Deleting order {}", id);
        if (orders.remove(id) != null) {
            return ResponseEntity.ok(Map.of("message", "Order deleted", "id", id));
        }
        return ResponseEntity.notFound().build();
    }

    private boolean checkInventory(String productId, int quantity) {
        try {
            log.info("Order Service: Checking inventory for {} quantity {}", productId, quantity);
            ResponseEntity<Map> response = restTemplate.getForEntity(
                inventoryServiceUrl + "/api/inventory/" + productId,
                Map.class
            );

            if (response.getBody() != null) {
                Integer availableStock = (Integer) response.getBody().get("quantity");
                return availableStock != null && availableStock >= quantity;
            }
            return false;
        } catch (Exception e) {
            log.error("Order Service: Failed to check inventory: {}", e.getMessage());
            return false;
        }
    }

    private void createSampleOrder(String productId, int quantity, String customerName) {
        String orderId = "ORD-" + orderIdCounter.incrementAndGet();
        Map<String, Object> order = new HashMap<>();
        order.put("id", orderId);
        order.put("productId", productId);
        order.put("quantity", quantity);
        order.put("customerName", customerName);
        order.put("status", "CONFIRMED");
        order.put("createdAt", LocalDateTime.now().toString());
        order.put("total", BigDecimal.valueOf(quantity * 29.99));
        orders.put(orderId, order);
    }
}
