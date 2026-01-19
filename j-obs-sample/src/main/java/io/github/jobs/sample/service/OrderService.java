package io.github.jobs.sample.service;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import io.github.jobs.sample.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Order service - orchestrates the order workflow.
 *
 * <p>This service demonstrates a full trace journey:</p>
 * <pre>
 * HTTP Request (endpoint)
 *   └─ OrderService.createOrder
 *       ├─ InventoryService.checkAvailability
 *       ├─ OrderRepository.save (INSERT INTO orders)
 *       ├─ InventoryService.reserveItems
 *       └─ NotificationService.sendEmail
 * </pre>
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        PaymentService paymentService,
                        InventoryService inventoryService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }

    public List<Order> findAll() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }

    public Optional<Order> findById(String id) {
        MDC.put("orderId", id);
        try {
            log.debug("Looking up order");
            return orderRepository.findById(id);
        } finally {
            MDC.remove("orderId");
        }
    }

    @Traced(name = "Create Order")
    public Order createOrder(String customerId, List<Order.OrderItem> items) {
        MDC.put("customerId", customerId);
        try {
            log.info("Creating order with {} items", items.size());

            // 1. Check inventory availability
            if (!inventoryService.checkAvailability(items)) {
                throw new IllegalStateException("Items not available in inventory");
            }

            // 2. Create order entity
            Order order = Order.create(customerId, items);
            MDC.put("orderId", order.id());

            // 3. Save to database
            order = orderRepository.save(order);

            // 4. Reserve inventory
            inventoryService.reserveItems(order.id(), items);

            // 5. Send confirmation notification
            notificationService.notifyOrderCreated(customerId, order.id());

            log.info("Order {} created with total: {}", order.id(), order.total());
            return order;

        } finally {
            MDC.remove("customerId");
            MDC.remove("orderId");
        }
    }

    @Traced(name = "Process Order")
    public Order processOrder(String orderId) {
        MDC.put("orderId", orderId);
        try {
            log.info("Starting order processing");

            // 1. Fetch order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            // 2. Update status to processing
            order = orderRepository.updateStatus(orderId, Order.OrderStatus.PROCESSING);
            log.debug("Order status changed to PROCESSING");

            // 3. Process payment
            String transactionId = paymentService.authorizePayment(orderId, order.total());
            paymentService.capturePayment(transactionId);

            // 4. Commit inventory reservation
            inventoryService.commitReservation(orderId);

            // 5. Random failure simulation (5% chance)
            if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                log.error("Order processing failed due to random error");
                orderRepository.updateStatus(orderId, Order.OrderStatus.FAILED);
                throw new RuntimeException("Random processing failure for demonstration");
            }

            // 6. Complete order
            order = orderRepository.updateStatus(orderId, Order.OrderStatus.COMPLETED);

            // 7. Send shipped notification
            notificationService.notifyOrderShipped(order.customerId(), orderId);

            log.info("Order {} processed successfully", orderId);
            return order;

        } finally {
            MDC.remove("orderId");
        }
    }

    @Traced(name = "Cancel Order")
    public Order cancelOrder(String orderId) {
        MDC.put("orderId", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            if (order.status() == Order.OrderStatus.COMPLETED) {
                log.warn("Cannot cancel completed order: {}", orderId);
                throw new IllegalArgumentException("Cannot cancel completed order");
            }

            // Release inventory
            inventoryService.releaseReservation(orderId);

            // Update status
            order = orderRepository.updateStatus(orderId, Order.OrderStatus.CANCELLED);
            log.info("Order {} cancelled", orderId);

            return order;

        } finally {
            MDC.remove("orderId");
        }
    }

    public Map<String, Object> getStatistics() {
        List<Order> orders = orderRepository.findAll();

        long total = orders.size();
        long pending = orders.stream().filter(o -> o.status() == Order.OrderStatus.PENDING).count();
        long processing = orders.stream().filter(o -> o.status() == Order.OrderStatus.PROCESSING).count();
        long completed = orders.stream().filter(o -> o.status() == Order.OrderStatus.COMPLETED).count();
        long cancelled = orders.stream().filter(o -> o.status() == Order.OrderStatus.CANCELLED).count();
        long failed = orders.stream().filter(o -> o.status() == Order.OrderStatus.FAILED).count();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.status() == Order.OrderStatus.COMPLETED)
                .map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "total", total,
                "pending", pending,
                "processing", processing,
                "completed", completed,
                "cancelled", cancelled,
                "failed", failed,
                "totalRevenue", totalRevenue
        );
    }

    @Traced(name = "Generate Sample Orders")
    public List<Order> generateSampleOrders(int count) {
        List<Order> generated = new ArrayList<>();
        String[] customers = {"customer-1", "customer-2", "customer-3", "customer-4", "customer-5"};
        String[][] products = {
                {"prod-1", "Laptop", "999.99"},
                {"prod-2", "Mouse", "29.99"},
                {"prod-3", "Keyboard", "79.99"},
                {"prod-4", "Monitor", "299.99"},
                {"prod-5", "Headphones", "149.99"},
                {"prod-6", "Webcam", "89.99"},
                {"prod-7", "USB Hub", "39.99"},
                {"prod-8", "Desk Lamp", "49.99"}
        };

        for (int i = 0; i < count; i++) {
            String customerId = customers[ThreadLocalRandom.current().nextInt(customers.length)];

            int itemCount = ThreadLocalRandom.current().nextInt(1, 5);
            List<Order.OrderItem> items = new ArrayList<>();

            for (int j = 0; j < itemCount; j++) {
                String[] product = products[ThreadLocalRandom.current().nextInt(products.length)];
                items.add(new Order.OrderItem(
                        product[0],
                        product[1],
                        ThreadLocalRandom.current().nextInt(1, 4),
                        new BigDecimal(product[2])
                ));
            }

            try {
                Order order = createOrder(customerId, items);
                generated.add(order);

                // Process some orders
                if (ThreadLocalRandom.current().nextBoolean()) {
                    processOrder(order.id());
                }
            } catch (Exception e) {
                log.warn("Failed to create/process sample order: {}", e.getMessage());
            }
        }

        return generated;
    }
}
