package io.github.jobs.sample.service;

import io.github.jobs.annotation.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Notification service - simulates sending notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    // Self-injection to allow AOP proxy interception on internal calls
    @Autowired
    @Lazy
    private NotificationService self;

    @Traced(name = "Send Email Notification", attributes = {"notification.channel=email"})
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to {}: {}", to, subject);
        simulateExternalCall(20, 80);
        log.info("Email sent successfully");
    }

    @Traced(name = "Send SMS Notification", attributes = {"notification.channel=sms"})
    public void sendSms(String phoneNumber, String message) {
        log.info("Sending SMS to {}", phoneNumber);
        simulateExternalCall(30, 100);
        log.info("SMS sent successfully");
    }

    @Traced(name = "Send Push Notification", attributes = {"notification.channel=push"})
    public void sendPushNotification(String userId, String title, String message) {
        log.info("Sending push notification to user {}: {}", userId, title);
        simulateExternalCall(10, 50);
        log.info("Push notification sent");
    }

    @Traced(name = "Notify Order Created")
    public void notifyOrderCreated(String customerId, String orderId) {
        // Call through self-proxy to enable AOP interception
        self.sendEmail(
            customerId + "@example.com",
            "Order Confirmed - " + orderId,
            "Your order has been confirmed. Order ID: " + orderId
        );
    }

    @Traced(name = "Notify Order Shipped")
    public void notifyOrderShipped(String customerId, String orderId) {
        // Call through self-proxy to enable AOP interception
        self.sendEmail(
            customerId + "@example.com",
            "Order Shipped - " + orderId,
            "Your order has been shipped!"
        );
        self.sendPushNotification(customerId, "Order Shipped", "Your order " + orderId + " is on the way!");
    }

    private void simulateExternalCall(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
