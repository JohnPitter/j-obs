package io.github.jobs.sample.service;

import io.github.jobs.annotation.Measured;
import io.github.jobs.annotation.Observable;
import io.github.jobs.annotation.Traced;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notification service - simulates sending notifications via multiple channels.
 * Demonstrates external service calls and multi-channel notifications.
 */
@Service
@Observable
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Custom metrics
    private final Counter emailsSent;
    private final Counter smsSent;
    private final Counter pushSent;
    private final Counter notificationsFailed;
    private final Timer notificationLatency;
    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    // Self-injection to allow AOP proxy interception on internal calls
    @Autowired
    @Lazy
    private NotificationService self;

    public NotificationService(MeterRegistry meterRegistry) {
        this.emailsSent = Counter.builder("notifications.sent")
                .description("Number of notifications sent")
                .tag("channel", "email")
                .register(meterRegistry);

        this.smsSent = Counter.builder("notifications.sent")
                .description("Number of notifications sent")
                .tag("channel", "sms")
                .register(meterRegistry);

        this.pushSent = Counter.builder("notifications.sent")
                .description("Number of notifications sent")
                .tag("channel", "push")
                .register(meterRegistry);

        this.notificationsFailed = Counter.builder("notifications.failed")
                .description("Number of failed notifications")
                .register(meterRegistry);

        this.notificationLatency = Timer.builder("notification.latency")
                .description("Notification sending latency")
                .register(meterRegistry);

        meterRegistry.gauge("notifications.pending", pendingNotifications);
    }

    @Traced(name = "Email Service - Send")
    @Measured(name = "notification.email.time")
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to {}: {}", to, subject);
        pendingNotifications.incrementAndGet();

        try {
            notificationLatency.record(() -> {
                simulateExternalCall(20, 80);

                // Simulate 2% failure rate
                if (ThreadLocalRandom.current().nextDouble() < 0.02) {
                    notificationsFailed.increment();
                    throw new RuntimeException("Email delivery failed: SMTP timeout");
                }

                emailsSent.increment();
                log.info("Email sent successfully to {}", to);
            });
        } finally {
            pendingNotifications.decrementAndGet();
        }
    }

    @Traced(name = "SMS Gateway - Send")
    @Measured(name = "notification.sms.time")
    public void sendSms(String phoneNumber, String message) {
        log.info("Sending SMS to {}", phoneNumber);
        pendingNotifications.incrementAndGet();

        try {
            notificationLatency.record(() -> {
                simulateExternalCall(30, 100);

                // Simulate 3% failure rate
                if (ThreadLocalRandom.current().nextDouble() < 0.03) {
                    notificationsFailed.increment();
                    throw new RuntimeException("SMS delivery failed: Invalid number");
                }

                smsSent.increment();
                log.info("SMS sent successfully to {}", phoneNumber);
            });
        } finally {
            pendingNotifications.decrementAndGet();
        }
    }

    @Traced(name = "Push Service - Send")
    @Measured(name = "notification.push.time")
    public void sendPushNotification(String userId, String title, String message) {
        log.info("Sending push notification to user {}: {}", userId, title);
        pendingNotifications.incrementAndGet();

        try {
            notificationLatency.record(() -> {
                simulateExternalCall(10, 50);

                // Simulate 1% failure rate
                if (ThreadLocalRandom.current().nextDouble() < 0.01) {
                    notificationsFailed.increment();
                    throw new RuntimeException("Push notification failed: Device not registered");
                }

                pushSent.increment();
                log.info("Push notification sent to user {}", userId);
            });
        } finally {
            pendingNotifications.decrementAndGet();
        }
    }

    @Traced(name = "Notification - Order Created")
    public void notifyOrderCreated(String customerId, String orderId) {
        log.info("Sending order created notifications for order {}", orderId);

        // Call through self-proxy to enable AOP interception
        self.sendEmail(
            customerId + "@example.com",
            "Order Confirmed - " + orderId,
            "Your order has been confirmed. Order ID: " + orderId
        );

        // Also send push notification
        try {
            self.sendPushNotification(customerId, "Order Confirmed",
                "Your order " + orderId + " has been confirmed!");
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }

    @Traced(name = "Notification - Order Shipped")
    public void notifyOrderShipped(String customerId, String orderId) {
        log.info("Sending order shipped notifications for order {}", orderId);

        // Call through self-proxy to enable AOP interception
        self.sendEmail(
            customerId + "@example.com",
            "Order Shipped - " + orderId,
            "Your order has been shipped!"
        );

        self.sendPushNotification(customerId, "Order Shipped",
            "Your order " + orderId + " is on the way!");

        // Also try SMS for important notifications
        try {
            self.sendSms("+1555" + customerId.hashCode() % 10000000,
                "Your order " + orderId + " has shipped!");
        } catch (Exception e) {
            log.warn("Failed to send SMS: {}", e.getMessage());
        }
    }

    @Traced(name = "Notification - Order Failed")
    public void notifyOrderFailed(String customerId, String orderId, String reason) {
        log.warn("Sending order failed notifications for order {}: {}", orderId, reason);

        self.sendEmail(
            customerId + "@example.com",
            "Order Failed - " + orderId,
            "We're sorry, your order could not be processed. Reason: " + reason
        );

        self.sendPushNotification(customerId, "Order Failed",
            "Order " + orderId + " could not be processed.");
    }

    @Traced(name = "Notification - Low Stock Alert")
    public void notifyLowStock(String productId, int currentStock) {
        log.warn("Low stock alert for product {}: {} units remaining", productId, currentStock);

        self.sendEmail(
            "inventory@example.com",
            "Low Stock Alert - " + productId,
            "Product " + productId + " has only " + currentStock + " units remaining."
        );
    }

    public int getPendingNotificationsCount() {
        return pendingNotifications.get();
    }

    private void simulateExternalCall(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
