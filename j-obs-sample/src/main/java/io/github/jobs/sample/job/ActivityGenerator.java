package io.github.jobs.sample.job;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import io.github.jobs.sample.service.InventoryService;
import io.github.jobs.sample.service.NotificationService;
import io.github.jobs.sample.service.OrderService;
import io.github.jobs.sample.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled job that generates sample activity for observability demonstration.
 * Generates varied scenarios to demonstrate all J-Obs features:
 * - Traces: Complex distributed traces across services
 * - Logs: Various log levels with context
 * - Metrics: Custom counters, timers, gauges
 * - Alerts: Error spikes, latency anomalies
 * - SQL Analyzer: N+1 patterns, slow queries
 * - Anomaly Detection: Traffic bursts, latency spikes
 */
@Component
public class ActivityGenerator {

    private static final Logger log = LoggerFactory.getLogger(ActivityGenerator.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    private final Counter activitiesGenerated;
    private final Counter errorSimulations;
    private final AtomicInteger burstModeCounter = new AtomicInteger(0);

    private boolean enabled = true;
    private boolean burstMode = false;

    public ActivityGenerator(OrderService orderService,
                             InventoryService inventoryService,
                             NotificationService notificationService,
                             PaymentService paymentService,
                             MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;

        this.activitiesGenerated = Counter.builder("activity.generated")
                .description("Number of activities generated")
                .register(meterRegistry);

        this.errorSimulations = Counter.builder("activity.errors.simulated")
                .description("Number of simulated errors")
                .register(meterRegistry);
    }

    /**
     * Generates random orders every 10 seconds.
     * In burst mode, generates multiple orders rapidly.
     */
    @Scheduled(fixedRate = 10000, initialDelay = 5000)
    @Traced(name = "Scheduled: Generate Activity")
    public void generateActivity() {
        if (!enabled) {
            return;
        }

        activitiesGenerated.increment();

        try {
            int action = ThreadLocalRandom.current().nextInt(20);

            if (action < 6) {
                // 30% - Create new order (triggers full flow)
                log.info("Generating new sample order...");
                orderService.generateSampleOrders(1);
            } else if (action < 10) {
                // 20% - Process pending order
                processPendingOrder();
            } else if (action < 12) {
                // 10% - Cancel random order
                cancelRandomOrder();
            } else if (action < 14) {
                // 10% - Check inventory (triggers SQL patterns)
                checkInventoryStatus();
            } else if (action < 16) {
                // 10% - Send notifications (multi-channel)
                sendTestNotifications();
            } else if (action < 18) {
                // 10% - Test payment flow
                testPaymentFlow();
            } else {
                // 10% - Simulate slow operation
                simulateSlowOperation();
            }
        } catch (Exception e) {
            log.warn("Activity generation failed: {}", e.getMessage());
            errorSimulations.increment();
        }
    }

    /**
     * Generates traffic burst every 2 minutes for anomaly detection demo.
     */
    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    @Traced(name = "Scheduled: Traffic Burst")
    public void generateTrafficBurst() {
        if (!enabled) {
            return;
        }

        // 20% chance of burst
        if (ThreadLocalRandom.current().nextDouble() < 0.2) {
            burstMode = true;
            log.warn("Traffic burst mode activated - simulating high load");

            for (int i = 0; i < 10; i++) {
                try {
                    orderService.generateSampleOrders(1);
                    burstModeCounter.incrementAndGet();
                } catch (Exception e) {
                    log.debug("Burst order failed: {}", e.getMessage());
                }
            }

            burstMode = false;
            log.info("Traffic burst completed, generated {} extra orders", burstModeCounter.getAndSet(0));
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

        String correlationId = "corr-" + System.currentTimeMillis();
        MDC.put("correlationId", correlationId);

        try {
            int level = ThreadLocalRandom.current().nextInt(100);

            if (level < 40) {
                log.debug("Debug: Memory usage {}MB, free {}MB",
                        Runtime.getRuntime().totalMemory() / 1024 / 1024,
                        Runtime.getRuntime().freeMemory() / 1024 / 1024);
            } else if (level < 70) {
                log.info("Info: System healthy, threads={}, orders={}",
                        Thread.activeCount(),
                        orderService.findAll().size());
            } else if (level < 85) {
                log.warn("Warning: Response time elevated ({}ms), consider scaling",
                        ThreadLocalRandom.current().nextInt(500, 2000));
            } else if (level < 95) {
                log.error("Error: Operation failed - {}",
                        getRandomErrorMessage(),
                        new RuntimeException("Simulated exception"));
                errorSimulations.increment();
            } else {
                // 5% - Generate error spike for alert testing
                for (int i = 0; i < 5; i++) {
                    log.error("Error spike #{}: Critical failure detected", i);
                }
                errorSimulations.increment();
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Checks inventory status - demonstrates SQL Analyzer N+1 pattern.
     */
    @Scheduled(fixedRate = 30000, initialDelay = 15000)
    @Traced(name = "Scheduled: Inventory Check")
    public void inventoryHealthCheck() {
        if (!enabled) {
            return;
        }

        MDC.put("operation", "inventoryCheck");
        try {
            Map<String, Integer> lowStock = inventoryService.getLowStockItems(30);
            int totalInventory = inventoryService.getTotalInventory();

            if (!lowStock.isEmpty()) {
                log.warn("Low stock detected: {} items below threshold", lowStock.size());
                for (Map.Entry<String, Integer> item : lowStock.entrySet()) {
                    // This triggers individual notification calls - demonstrates service map
                    notificationService.notifyLowStock(item.getKey(), item.getValue());
                }
            }

            log.info("Inventory check complete: total={}, lowStock={}", totalInventory, lowStock.size());
        } finally {
            MDC.remove("operation");
        }
    }

    private void processPendingOrder() {
        List<Order> pending = orderService.findAll().stream()
                .filter(o -> o.status() == Order.OrderStatus.PENDING)
                .toList();

        if (!pending.isEmpty()) {
            Order order = pending.get(ThreadLocalRandom.current().nextInt(pending.size()));
            MDC.put("orderId", order.id());
            log.info("Processing pending order");
            try {
                orderService.processOrder(order.id());
            } catch (Exception e) {
                log.warn("Failed to process order: {}", e.getMessage());
                errorSimulations.increment();
            } finally {
                MDC.remove("orderId");
            }
        }
    }

    private void cancelRandomOrder() {
        List<Order> cancellable = orderService.findAll().stream()
                .filter(o -> o.status() == Order.OrderStatus.PENDING)
                .toList();

        if (!cancellable.isEmpty()) {
            Order order = cancellable.get(ThreadLocalRandom.current().nextInt(cancellable.size()));
            MDC.put("orderId", order.id());
            log.info("Cancelling order");
            try {
                orderService.cancelOrder(order.id());
            } catch (Exception e) {
                log.warn("Failed to cancel order: {}", e.getMessage());
            } finally {
                MDC.remove("orderId");
            }
        }
    }

    private void checkInventoryStatus() {
        log.debug("Checking inventory status for all products");

        // This creates multiple individual queries - SQL Analyzer will detect N+1 pattern
        String[] products = {"prod-1", "prod-2", "prod-3", "prod-4", "prod-5"};
        for (String productId : products) {
            inventoryService.getLowStockItems(10);
        }

        log.info("Inventory status check completed for {} products", products.length);
    }

    private void sendTestNotifications() {
        String testCustomerId = "test-customer-" + ThreadLocalRandom.current().nextInt(1000);
        String testOrderId = "test-order-" + System.currentTimeMillis();

        try {
            int channel = ThreadLocalRandom.current().nextInt(3);
            switch (channel) {
                case 0 -> notificationService.sendEmail(
                        testCustomerId + "@example.com",
                        "Test Notification",
                        "This is a test notification message");
                case 1 -> notificationService.sendSms(
                        "+1555" + ThreadLocalRandom.current().nextInt(1000000, 9999999),
                        "Test SMS notification");
                case 2 -> notificationService.sendPushNotification(
                        testCustomerId,
                        "Test Alert",
                        "Push notification test");
            }
        } catch (Exception e) {
            log.debug("Test notification failed (expected occasionally): {}", e.getMessage());
        }
    }

    private void testPaymentFlow() {
        String testOrderId = "payment-test-" + System.currentTimeMillis();
        BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10, 500));

        MDC.put("orderId", testOrderId);
        try {
            String txnId = paymentService.authorizePayment(testOrderId, amount);
            log.info("Test payment authorized: txn={}", txnId);

            // 50% chance to capture
            if (ThreadLocalRandom.current().nextBoolean()) {
                paymentService.capturePayment(txnId);
                log.info("Test payment captured: txn={}", txnId);
            }
        } catch (Exception e) {
            log.warn("Test payment failed (expected occasionally): {}", e.getMessage());
        } finally {
            MDC.remove("orderId");
        }
    }

    private void simulateSlowOperation() {
        log.debug("Simulating slow operation for latency testing");
        try {
            // Simulate variable latency
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("Slow operation completed");
    }

    private String getRandomErrorMessage() {
        String[] errors = {
                "Database connection timeout",
                "External service unavailable",
                "Resource allocation failed",
                "Cache miss - fallback activated",
                "Rate limit exceeded",
                "Invalid response from upstream",
                "Circuit breaker open",
                "Retry exhausted"
        };
        return errors[ThreadLocalRandom.current().nextInt(errors.length)];
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBurstMode() {
        return burstMode;
    }
}
