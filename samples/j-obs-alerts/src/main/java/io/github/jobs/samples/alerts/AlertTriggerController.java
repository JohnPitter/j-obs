package io.github.jobs.samples.alerts;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Controller to trigger various alert conditions for testing.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertTriggerController {

    private static final Logger log = LoggerFactory.getLogger(AlertTriggerController.class);

    private final Counter errorCounter;
    private final Timer requestTimer;

    public AlertTriggerController(MeterRegistry registry) {
        this.errorCounter = Counter.builder("alerts.sample.errors")
            .description("Sample error counter for alert testing")
            .tag("type", "simulated")
            .register(registry);

        this.requestTimer = Timer.builder("alerts.sample.requests")
            .description("Sample request timer for alert testing")
            .tag("endpoint", "trigger")
            .register(registry);
    }

    @GetMapping("/trigger-errors/{count}")
    public Map<String, Object> triggerErrors(@PathVariable int count) {
        log.warn("Triggering {} simulated errors for alert testing", count);

        for (int i = 0; i < count; i++) {
            errorCounter.increment();
            log.error("Simulated error #{} for alert testing", i + 1);
        }

        return Map.of(
            "message", "Triggered " + count + " errors",
            "alertExpected", "error-spike alert should fire if threshold exceeded"
        );
    }

    @GetMapping("/trigger-slow/{durationMs}")
    public Map<String, Object> triggerSlowRequest(@PathVariable int durationMs) throws InterruptedException {
        log.info("Triggering slow request with {}ms delay", durationMs);

        long startTime = System.nanoTime();
        Thread.sleep(durationMs);
        long elapsedNanos = System.nanoTime() - startTime;

        requestTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);

        return Map.of(
            "message", "Slow request completed",
            "durationMs", durationMs,
            "alertExpected", "high-latency alert should fire if threshold exceeded"
        );
    }

    @GetMapping("/trigger-random-errors")
    public Map<String, Object> triggerRandomErrors() {
        int errorCount = ThreadLocalRandom.current().nextInt(1, 20);
        int delayMs = ThreadLocalRandom.current().nextInt(50, 500);

        log.warn("Triggering {} random errors with ~{}ms interval", errorCount, delayMs);

        for (int i = 0; i < errorCount; i++) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            errorCounter.increment();
            log.error("Random simulated error #{}", i + 1);
        }

        return Map.of(
            "message", "Random errors triggered",
            "errorCount", errorCount,
            "approximateDurationMs", errorCount * delayMs
        );
    }

    @GetMapping("/trigger-warning")
    public Map<String, String> triggerWarning() {
        log.warn("This is a warning level log message for alert testing");
        log.warn("Warning: Elevated resource usage detected (simulated)");
        log.warn("Warning: Connection pool usage at 80% (simulated)");
        return Map.of(
            "message", "Warning messages logged",
            "alertExpected", "warning-count alert may fire"
        );
    }

    @GetMapping("/trigger-critical")
    public Map<String, String> triggerCritical() {
        log.error("CRITICAL: System health degraded (simulated)");
        log.error("CRITICAL: Database connection failed (simulated)");
        log.error("CRITICAL: Out of memory warning (simulated)");
        errorCounter.increment(3);
        return Map.of(
            "message", "Critical messages logged",
            "alertExpected", "critical-error alert should fire"
        );
    }

    @GetMapping("/status")
    public Map<String, Object> getAlertStatus() {
        return Map.of(
            "errorCount", errorCounter.count(),
            "message", "Use the J-Obs dashboard to view active alerts"
        );
    }
}
