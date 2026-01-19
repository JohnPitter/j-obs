package io.github.jobs.sample.service;

import io.github.jobs.annotation.Measured;
import io.github.jobs.annotation.Observable;
import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inventory service - simulates inventory management.
 * Demonstrates database-like operations for SQL Analyzer demo.
 */
@Service
@Observable
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    // Simulated inventory data
    private final Map<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> reservations = new ConcurrentHashMap<>();

    // Custom metrics
    private final Counter inventoryChecks;
    private final Counter inventoryReservations;
    private final Counter inventoryCommits;
    private final Counter inventoryReleases;
    private final Counter stockOuts;
    private final Timer inventoryQueryTime;

    // Self-injection to allow AOP proxy interception on internal calls
    @Autowired
    @Lazy
    private InventoryService self;

    public InventoryService(MeterRegistry meterRegistry) {
        // Initialize some inventory
        inventory.put("prod-1", new AtomicInteger(100));
        inventory.put("prod-2", new AtomicInteger(500));
        inventory.put("prod-3", new AtomicInteger(200));
        inventory.put("prod-4", new AtomicInteger(50));
        inventory.put("prod-5", new AtomicInteger(150));
        inventory.put("prod-6", new AtomicInteger(75));
        inventory.put("prod-7", new AtomicInteger(300));
        inventory.put("prod-8", new AtomicInteger(200));

        // Register metrics
        this.inventoryChecks = Counter.builder("inventory.checks")
                .description("Number of inventory availability checks")
                .register(meterRegistry);

        this.inventoryReservations = Counter.builder("inventory.reservations")
                .description("Number of inventory reservations")
                .register(meterRegistry);

        this.inventoryCommits = Counter.builder("inventory.commits")
                .description("Number of committed reservations")
                .register(meterRegistry);

        this.inventoryReleases = Counter.builder("inventory.releases")
                .description("Number of released reservations")
                .register(meterRegistry);

        this.stockOuts = Counter.builder("inventory.stockouts")
                .description("Number of stock-out events")
                .register(meterRegistry);

        this.inventoryQueryTime = Timer.builder("inventory.query.time")
                .description("Inventory query execution time")
                .register(meterRegistry);

        // Gauge for total inventory items
        meterRegistry.gauge("inventory.total.items", inventory,
                inv -> inv.values().stream().mapToInt(AtomicInteger::get).sum());
    }

    @Traced(name = "SELECT * FROM inventory WHERE product_id IN (?)")
    @Measured(name = "inventory.check.time")
    public boolean checkAvailability(List<Order.OrderItem> items) {
        log.debug("Checking inventory for {} items", items.size());
        inventoryChecks.increment();

        return inventoryQueryTime.record(() -> {
            simulateDbCall();

            for (Order.OrderItem item : items) {
                AtomicInteger stock = inventory.get(item.productId());
                if (stock == null || stock.get() < item.quantity()) {
                    log.warn("Insufficient inventory for product {}: requested {}, available {}",
                            item.productId(), item.quantity(), stock != null ? stock.get() : 0);
                    stockOuts.increment();
                    return false;
                }
            }

            log.debug("Inventory check passed for {} items", items.size());
            return true;
        });
    }

    @Traced(name = "BEGIN TRANSACTION; UPDATE inventory SET reserved = reserved + ?")
    @Measured(name = "inventory.reserve.time")
    public void reserveItems(String orderId, List<Order.OrderItem> items) {
        log.debug("Reserving inventory for order {}", orderId);
        inventoryReservations.increment();

        Map<String, Integer> orderReservation = new ConcurrentHashMap<>();

        for (Order.OrderItem item : items) {
            // Call through self-proxy to enable AOP interception
            // This demonstrates N+1 query pattern for SQL Analyzer
            self.reserveItem(item.productId(), item.quantity());
            orderReservation.put(item.productId(), item.quantity());
        }

        reservations.put(orderId, orderReservation);
        log.debug("Inventory reserved for order {} ({} items)", orderId, items.size());
    }

    @Traced(name = "UPDATE inventory SET reserved = reserved + ? WHERE product_id = ?")
    public void reserveItem(String productId, int quantity) {
        log.trace("Reserving {} units of {}", quantity, productId);
        simulateDbCall();

        AtomicInteger stock = inventory.get(productId);
        if (stock != null) {
            stock.addAndGet(-quantity);
        }
    }

    @Traced(name = "COMMIT; UPDATE inventory SET quantity = quantity - reserved WHERE order_id = ?")
    @Measured(name = "inventory.commit.time")
    public void commitReservation(String orderId) {
        log.debug("Committing inventory reservation for order {}", orderId);
        inventoryCommits.increment();
        simulateDbCall();

        reservations.remove(orderId);
        log.debug("Reservation committed for order {}", orderId);
    }

    @Traced(name = "ROLLBACK; UPDATE inventory SET reserved = reserved - ? WHERE order_id = ?")
    @Measured(name = "inventory.release.time")
    public void releaseReservation(String orderId) {
        log.debug("Releasing inventory reservation for order {}", orderId);
        inventoryReleases.increment();
        simulateDbCall();

        Map<String, Integer> orderReservation = reservations.remove(orderId);
        if (orderReservation != null) {
            // Restore inventory
            orderReservation.forEach((productId, quantity) -> {
                AtomicInteger stock = inventory.get(productId);
                if (stock != null) {
                    stock.addAndGet(quantity);
                }
            });
        }

        log.debug("Reservation released for order {}", orderId);
    }

    @Traced(name = "SELECT product_id, quantity FROM inventory WHERE quantity < ?")
    public Map<String, Integer> getLowStockItems(int threshold) {
        log.debug("Querying low stock items (threshold: {})", threshold);
        simulateDbCall();

        Map<String, Integer> lowStock = new ConcurrentHashMap<>();
        inventory.forEach((productId, stock) -> {
            if (stock.get() < threshold) {
                lowStock.put(productId, stock.get());
            }
        });

        return lowStock;
    }

    @Traced(name = "SELECT SUM(quantity) FROM inventory")
    public int getTotalInventory() {
        simulateDbCall();
        return inventory.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    private void simulateDbCall() {
        try {
            // Simulate database query latency (5-30ms)
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
