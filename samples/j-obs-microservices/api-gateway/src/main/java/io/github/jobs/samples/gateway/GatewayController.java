package io.github.jobs.samples.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Gateway controller that routes requests to downstream services.
 * Trace context is automatically propagated via HTTP headers.
 */
@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate;
    private final String orderServiceUrl;
    private final String inventoryServiceUrl;

    public GatewayController(
            RestTemplate restTemplate,
            @Value("${services.order.url}") String orderServiceUrl,
            @Value("${services.inventory.url}") String inventoryServiceUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceUrl = orderServiceUrl;
        this.inventoryServiceUrl = inventoryServiceUrl;
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        log.info("Gateway: Routing to order-service /orders");
        return restTemplate.getForEntity(orderServiceUrl + "/api/orders", Object.class);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        log.info("Gateway: Routing to order-service /orders/{}", id);
        return restTemplate.getForEntity(orderServiceUrl + "/api/orders/" + id, Object.class);
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> order) {
        log.info("Gateway: Routing POST to order-service /orders");
        return restTemplate.postForEntity(orderServiceUrl + "/api/orders", order, Object.class);
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory() {
        log.info("Gateway: Routing to inventory-service /inventory");
        return restTemplate.getForEntity(inventoryServiceUrl + "/api/inventory", Object.class);
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> getInventoryItem(@PathVariable String productId) {
        log.info("Gateway: Routing to inventory-service /inventory/{}", productId);
        return restTemplate.getForEntity(inventoryServiceUrl + "/api/inventory/" + productId, Object.class);
    }

    @GetMapping("/health/services")
    public Map<String, Object> checkServices() {
        log.info("Gateway: Checking downstream services health");

        boolean orderServiceHealthy = checkServiceHealth(orderServiceUrl);
        boolean inventoryServiceHealthy = checkServiceHealth(inventoryServiceUrl);

        return Map.of(
            "gateway", "UP",
            "orderService", orderServiceHealthy ? "UP" : "DOWN",
            "inventoryService", inventoryServiceHealthy ? "UP" : "DOWN"
        );
    }

    private boolean checkServiceHealth(String serviceUrl) {
        try {
            restTemplate.getForEntity(serviceUrl + "/actuator/health", Object.class);
            return true;
        } catch (Exception e) {
            log.warn("Service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
