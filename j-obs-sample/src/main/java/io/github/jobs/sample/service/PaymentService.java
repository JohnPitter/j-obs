package io.github.jobs.sample.service;

import io.github.jobs.annotation.Traced;
import io.github.jobs.sample.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Payment service - simulates external payment gateway calls.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Traced(name = "Payment Gateway - Authorize", attributes = {"payment.provider=stripe"})
    public String authorizePayment(String orderId, BigDecimal amount) {
        log.info("Authorizing payment of {} for order {}", amount, orderId);
        simulateExternalCall(50, 200);

        String transactionId = "txn_" + System.currentTimeMillis();
        log.info("Payment authorized: {}", transactionId);
        return transactionId;
    }

    @Traced(name = "Payment Gateway - Capture", attributes = {"payment.provider=stripe"})
    public void capturePayment(String transactionId) {
        log.info("Capturing payment: {}", transactionId);
        simulateExternalCall(30, 100);
        log.info("Payment captured successfully");
    }

    @Traced(name = "Payment Gateway - Refund", attributes = {"payment.provider=stripe"})
    public void refundPayment(String transactionId, BigDecimal amount) {
        log.info("Refunding {} for transaction {}", amount, transactionId);
        simulateExternalCall(50, 150);
        log.info("Refund processed");
    }

    private void simulateExternalCall(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
