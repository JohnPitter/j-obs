package io.github.jobs.sample.controller;

import io.github.jobs.sample.model.Order;
import io.github.jobs.sample.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for order operations.
 * Demonstrates logging and tracing in a typical Spring controller.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> listOrders() {
        log.info("Listing all orders");
        List<Order> orders = orderService.findAll();
        log.debug("Found {} orders", orders.size());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        log.info("Getting order by id: {}", id);
        return orderService.findById(id)
                .map(order -> {
                    log.debug("Order found: {}", order.id());
                    return ResponseEntity.ok(order);
                })
                .orElseGet(() -> {
                    log.warn("Order not found: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.customerId());

        List<Order.OrderItem> items = request.items().stream()
                .map(item -> new Order.OrderItem(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.price()
                ))
                .toList();

        Order order = orderService.createOrder(request.customerId(), items);
        log.info("Order created successfully: {}", order.id());

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Order> processOrder(@PathVariable String id) {
        log.info("Processing order: {}", id);

        try {
            Order order = orderService.processOrder(id);
            log.info("Order processed successfully: {}", id);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            log.error("Failed to process order {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String id) {
        log.info("Cancelling order: {}", id);

        try {
            Order order = orderService.cancelOrder(id);
            log.info("Order cancelled: {}", id);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot cancel order {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.debug("Getting order statistics");
        return ResponseEntity.ok(orderService.getStatistics());
    }

    @PostMapping("/generate")
    public ResponseEntity<List<Order>> generateSampleOrders(@RequestParam(defaultValue = "5") int count) {
        log.info("Generating {} sample orders", count);
        List<Order> orders = orderService.generateSampleOrders(count);
        log.info("Generated {} sample orders", orders.size());
        return ResponseEntity.ok(orders);
    }

    public record CreateOrderRequest(
            String customerId,
            List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
            String productId,
            String productName,
            int quantity,
            BigDecimal price
    ) {}
}
