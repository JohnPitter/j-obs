package io.github.jobs.sample.repository;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Order repository - simulates database operations.
 * Each method creates a traced span showing the "database" layer.
 */
@Repository
@Traced
public class OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(OrderRepository.class);

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @Traced(name = "SELECT orders")
    public List<Order> findAll() {
        log.debug("Executing: SELECT * FROM orders");
        simulateDbLatency();
        return new ArrayList<>(orders.values());
    }

    @Traced(name = "SELECT order WHERE id = ?")
    public Optional<Order> findById(String id) {
        log.debug("Executing: SELECT * FROM orders WHERE id = '{}'", id);
        simulateDbLatency();
        return Optional.ofNullable(orders.get(id));
    }

    @Traced(name = "INSERT INTO orders")
    public Order save(Order order) {
        log.debug("Executing: INSERT INTO orders (id, customer_id, status, total) VALUES ('{}', '{}', '{}', {})",
                order.id(), order.customerId(), order.status(), order.total());
        simulateDbLatency();
        orders.put(order.id(), order);
        return order;
    }

    @Traced(name = "UPDATE orders SET status = ?")
    public Order updateStatus(String orderId, Order.OrderStatus status) {
        log.debug("Executing: UPDATE orders SET status = '{}' WHERE id = '{}'", status, orderId);
        simulateDbLatency();
        Order order = orders.get(orderId);
        if (order != null) {
            order = order.withStatus(status);
            orders.put(orderId, order);
        }
        return order;
    }

    @Traced(name = "DELETE FROM orders")
    public void delete(String orderId) {
        log.debug("Executing: DELETE FROM orders WHERE id = '{}'", orderId);
        simulateDbLatency();
        orders.remove(orderId);
    }

    @Traced(name = "SELECT COUNT(*) FROM orders")
    public long count() {
        log.debug("Executing: SELECT COUNT(*) FROM orders");
        simulateDbLatency();
        return orders.size();
    }

    private void simulateDbLatency() {
        try {
            // Simulate database query latency (5-30ms)
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
