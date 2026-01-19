package io.github.jobs.sample.service;

import io.github.jobs.annotation.Measured;
import io.github.jobs.annotation.Observable;
import io.github.jobs.annotation.Traced;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Payment service - simulates external payment gateway calls.
 * Demonstrates custom metrics and distributed tracing.
 */
@Service
@Observable
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // Custom metrics
    private final Counter paymentsAuthorized;
    private final Counter paymentsCaptured;
    private final Counter paymentsFailed;
    private final Counter refundsProcessed;
    private final Timer paymentLatency;
    private final AtomicInteger pendingPayments = new AtomicInteger(0);

    public PaymentService(MeterRegistry meterRegistry) {
        this.paymentsAuthorized = Counter.builder("payments.authorized")
                .description("Number of authorized payments")
                .tag("provider", "stripe")
                .register(meterRegistry);

        this.paymentsCaptured = Counter.builder("payments.captured")
                .description("Number of captured payments")
                .tag("provider", "stripe")
                .register(meterRegistry);

        this.paymentsFailed = Counter.builder("payments.failed")
                .description("Number of failed payments")
                .tag("provider", "stripe")
                .register(meterRegistry);

        this.refundsProcessed = Counter.builder("refunds.processed")
                .description("Number of processed refunds")
                .tag("provider", "stripe")
                .register(meterRegistry);

        this.paymentLatency = Timer.builder("payment.latency")
                .description("Payment processing latency")
                .tag("provider", "stripe")
                .register(meterRegistry);

        // Gauge for pending payments
        meterRegistry.gauge("payments.pending", pendingPayments);
    }

    @Traced(name = "Payment Gateway - Authorize")
    @Measured(name = "payment.authorize.time")
    public String authorizePayment(String orderId, BigDecimal amount) {
        log.info("Authorizing payment of {} for order {}", amount, orderId);
        pendingPayments.incrementAndGet();

        try {
            return paymentLatency.record(() -> {
                simulateExternalCall(50, 200);

                // Simulate 3% failure rate
                if (ThreadLocalRandom.current().nextDouble() < 0.03) {
                    paymentsFailed.increment();
                    throw new RuntimeException("Payment authorization failed: Card declined");
                }

                String transactionId = "txn_" + System.currentTimeMillis();
                paymentsAuthorized.increment();
                log.info("Payment authorized: {} (amount: {})", transactionId, amount);
                return transactionId;
            });
        } finally {
            pendingPayments.decrementAndGet();
        }
    }

    @Traced(name = "Payment Gateway - Capture")
    @Measured(name = "payment.capture.time")
    public void capturePayment(String transactionId) {
        log.info("Capturing payment: {}", transactionId);

        paymentLatency.record(() -> {
            simulateExternalCall(30, 100);

            // Simulate 1% failure rate
            if (ThreadLocalRandom.current().nextDouble() < 0.01) {
                paymentsFailed.increment();
                throw new RuntimeException("Payment capture failed: Transaction expired");
            }

            paymentsCaptured.increment();
            log.info("Payment captured successfully: {}", transactionId);
        });
    }

    @Traced(name = "Payment Gateway - Refund")
    @Measured(name = "payment.refund.time")
    public void refundPayment(String transactionId, BigDecimal amount) {
        log.info("Refunding {} for transaction {}", amount, transactionId);

        paymentLatency.record(() -> {
            simulateExternalCall(50, 150);
            refundsProcessed.increment();
            log.info("Refund processed: {} for amount {}", transactionId, amount);
        });
    }

    @Traced(name = "Payment Gateway - Check Status")
    public String checkPaymentStatus(String transactionId) {
        log.debug("Checking payment status: {}", transactionId);
        simulateExternalCall(20, 50);

        String[] statuses = {"COMPLETED", "PENDING", "PROCESSING"};
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }

    public int getPendingPaymentsCount() {
        return pendingPayments.get();
    }

    private void simulateExternalCall(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
