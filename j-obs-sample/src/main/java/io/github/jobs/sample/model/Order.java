package io.github.jobs.sample.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sample order model for demonstration.
 */
public record Order(
        String id,
        String customerId,
        List<OrderItem> items,
        BigDecimal total,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static Order create(String customerId, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant now = Instant.now();
        return new Order(
                UUID.randomUUID().toString(),
                customerId,
                items,
                total,
                OrderStatus.PENDING,
                now,
                now
        );
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, items, total, newStatus, createdAt, Instant.now());
    }

    public record OrderItem(
            String productId,
            String productName,
            int quantity,
            BigDecimal price
    ) {}

    public enum OrderStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        CANCELLED,
        FAILED
    }
}
