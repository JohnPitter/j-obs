package io.github.jobs.sample.service;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inventory service - simulates inventory management.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    // Self-injection to allow AOP proxy interception on internal calls
    @Autowired
    @Lazy
    private InventoryService self;

    @Traced(name = "Check Inventory Availability")
    public boolean checkAvailability(List<Order.OrderItem> items) {
        log.debug("Checking inventory for {} items", items.size());
        simulateDbCall();

        // Simulate 95% success rate
        boolean available = ThreadLocalRandom.current().nextDouble() > 0.05;
        log.debug("Inventory check result: {}", available ? "available" : "not available");
        return available;
    }

    @Traced(name = "Reserve Inventory")
    public void reserveItems(String orderId, List<Order.OrderItem> items) {
        log.debug("Reserving inventory for order {}", orderId);
        for (Order.OrderItem item : items) {
            // Call through self-proxy to enable AOP interception
            self.reserveItem(item);
        }
        log.debug("Inventory reserved");
    }

    @Traced(name = "UPDATE inventory SET reserved = reserved + ?")
    public void reserveItem(Order.OrderItem item) {
        log.trace("Reserving {} units of {}", item.quantity(), item.productId());
        simulateDbCall();
    }

    @Traced(name = "Commit Inventory Reservation")
    public void commitReservation(String orderId) {
        log.debug("Committing inventory reservation for order {}", orderId);
        simulateDbCall();
        log.debug("Reservation committed");
    }

    @Traced(name = "Release Inventory Reservation")
    public void releaseReservation(String orderId) {
        log.debug("Releasing inventory reservation for order {}", orderId);
        simulateDbCall();
        log.debug("Reservation released");
    }

    private void simulateDbCall() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 20));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
