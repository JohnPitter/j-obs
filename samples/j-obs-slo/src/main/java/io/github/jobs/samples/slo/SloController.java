package io.github.jobs.samples.slo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller with endpoints that have varying latency and error rates
 * for SLO demonstration.
 */
@RestController
@RequestMapping("/api")
public class SloController {

    private static final Logger log = LoggerFactory.getLogger(SloController.class);

    private final Timer apiLatencyTimer;
    private final Counter totalRequestsCounter;
    private final Counter successfulRequestsCounter;
    private final Counter failedRequestsCounter;

    // Configurable error rate and latency for demonstration
    private final AtomicInteger errorRatePercent = new AtomicInteger(1);
    private final AtomicInteger baseLatencyMs = new AtomicInteger(50);

    public SloController(MeterRegistry registry) {
        this.apiLatencyTimer = Timer.builder("slo.api.latency")
            .description("API endpoint latency for SLO tracking")
            .tag("endpoint", "orders")
            .register(registry);

        this.totalRequestsCounter = Counter.builder("slo.api.requests")
            .description("Total API requests")
            .tag("endpoint", "orders")
            .register(registry);

        this.successfulRequestsCounter = Counter.builder("slo.api.requests.success")
            .description("Successful API requests")
            .tag("endpoint", "orders")
            .register(registry);

        this.failedRequestsCounter = Counter.builder("slo.api.requests.failed")
            .description("Failed API requests")
            .tag("endpoint", "orders")
            .register(registry);
    }

    /**
     * Main API endpoint for SLO tracking.
     * Latency and error rate are configurable for demonstration.
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getOrders() {
        long startTime = System.nanoTime();
        totalRequestsCounter.increment();

        try {
            // Simulate varying latency
            int latency = baseLatencyMs.get() + ThreadLocalRandom.current().nextInt(-20, 50);
            if (latency > 0) {
                Thread.sleep(latency);
            }

            // Simulate errors based on configured error rate
            if (ThreadLocalRandom.current().nextInt(100) < errorRatePercent.get()) {
                failedRequestsCounter.increment();
                log.error("Simulated error for SLO demonstration");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Simulated error"));
            }

            successfulRequestsCounter.increment();
            return ResponseEntity.ok(Map.of(
                "orders", 42,
                "status", "success"
            ));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedRequestsCounter.increment();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Interrupted"));
        } finally {
            long elapsedNanos = System.nanoTime() - startTime;
            apiLatencyTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Fast endpoint that should always meet latency SLO.
     */
    @GetMapping("/health-check")
    public Map<String, String> healthCheck() {
        return Map.of("status", "healthy");
    }

    /**
     * Slow endpoint that may violate latency SLO.
     */
    @GetMapping("/slow-operation")
    public Map<String, Object> slowOperation() throws InterruptedException {
        int latency = 200 + ThreadLocalRandom.current().nextInt(0, 300);
        Thread.sleep(latency);
        return Map.of(
            "message", "Slow operation completed",
            "latencyMs", latency
        );
    }

    // ============================================
    // CONFIGURATION ENDPOINTS
    // ============================================

    /**
     * Set the simulated error rate for demonstration.
     */
    @PostMapping("/config/error-rate/{percent}")
    public Map<String, Object> setErrorRate(@PathVariable int percent) {
        int oldRate = errorRatePercent.getAndSet(Math.min(100, Math.max(0, percent)));
        log.info("Error rate changed from {}% to {}%", oldRate, percent);
        return Map.of(
            "oldErrorRate", oldRate,
            "newErrorRate", percent,
            "note", "This affects the /api/orders endpoint"
        );
    }

    /**
     * Set the base latency for demonstration.
     */
    @PostMapping("/config/latency/{ms}")
    public Map<String, Object> setLatency(@PathVariable int ms) {
        int oldLatency = baseLatencyMs.getAndSet(Math.max(0, ms));
        log.info("Base latency changed from {}ms to {}ms", oldLatency, ms);
        return Map.of(
            "oldLatency", oldLatency,
            "newLatency", ms,
            "note", "This affects the /api/orders endpoint"
        );
    }

    /**
     * Get current configuration.
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
            "errorRatePercent", errorRatePercent.get(),
            "baseLatencyMs", baseLatencyMs.get()
        );
    }

    /**
     * Reset configuration to defaults.
     */
    @PostMapping("/config/reset")
    public Map<String, Object> resetConfig() {
        errorRatePercent.set(1);
        baseLatencyMs.set(50);
        log.info("Configuration reset to defaults");
        return Map.of(
            "errorRatePercent", 1,
            "baseLatencyMs", 50,
            "message", "Configuration reset to defaults"
        );
    }
}
