package io.github.jobs.sample.job;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import io.github.jobs.sample.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scheduled job that generates sample activity for observability demonstration.
 */
@Component
public class ActivityGenerator {

    private static final Logger log = LoggerFactory.getLogger(ActivityGenerator.class);

    private final OrderService orderService;
    private boolean enabled = true;

    public ActivityGenerator(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Generates random orders every 10 seconds.
     */
    @Scheduled(fixedRate = 10000, initialDelay = 5000)
    @Traced(name = "Scheduled: Generate Activity")
    public void generateActivity() {
        if (!enabled) {
            return;
        }

        try {
            int action = ThreadLocalRandom.current().nextInt(10);

            if (action < 5) {
                // 50% - Create new order
                log.info("Generating new sample order...");
                orderService.generateSampleOrders(1);
            } else if (action < 8) {
                // 30% - Process pending order
                processPendingOrder();
            } else {
                // 20% - Cancel random order
                cancelRandomOrder();
            }
        } catch (Exception e) {
            log.warn("Activity generation failed: {}", e.getMessage());
        }
    }

    /**
     * Logs various level messages for demonstration.
     */
    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    public void generateLogs() {
        if (!enabled) {
            return;
        }

        int level = ThreadLocalRandom.current().nextInt(100);

        if (level < 50) {
            log.debug("Debug message: Current memory usage is {}MB", Runtime.getRuntime().totalMemory() / 1024 / 1024);
        } else if (level < 80) {
            log.info("Info message: System running normally, active threads: {}", Thread.activeCount());
        } else if (level < 95) {
            log.warn("Warning message: High response time detected ({}ms)", ThreadLocalRandom.current().nextInt(500, 2000));
        } else {
            log.error("Error message: Simulated error for demonstration", new RuntimeException("Sample exception"));
        }
    }

    private void processPendingOrder() {
        List<Order> pending = orderService.findAll().stream()
                .filter(o -> o.status() == Order.OrderStatus.PENDING)
                .toList();

        if (!pending.isEmpty()) {
            Order order = pending.get(ThreadLocalRandom.current().nextInt(pending.size()));
            log.info("Processing pending order: {}", order.id());
            try {
                orderService.processOrder(order.id());
            } catch (Exception e) {
                log.warn("Failed to process order {}: {}", order.id(), e.getMessage());
            }
        }
    }

    private void cancelRandomOrder() {
        List<Order> cancellable = orderService.findAll().stream()
                .filter(o -> o.status() == Order.OrderStatus.PENDING)
                .toList();

        if (!cancellable.isEmpty()) {
            Order order = cancellable.get(ThreadLocalRandom.current().nextInt(cancellable.size()));
            log.info("Cancelling order: {}", order.id());
            try {
                orderService.cancelOrder(order.id());
            } catch (Exception e) {
                log.warn("Failed to cancel order {}: {}", order.id(), e.getMessage());
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
