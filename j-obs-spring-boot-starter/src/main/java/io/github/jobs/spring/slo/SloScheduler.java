package io.github.jobs.spring.slo;

import io.github.jobs.application.SloService;
import io.github.jobs.domain.slo.SloEvaluation;
import io.github.jobs.domain.slo.SloStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for periodic SLO evaluation.
 */
public class SloScheduler {

    private static final Logger log = LoggerFactory.getLogger(SloScheduler.class);

    private final SloService sloService;
    private final Duration evaluationInterval;
    private final ScheduledExecutorService scheduler;

    private volatile boolean running = false;

    public SloScheduler(SloService sloService, Duration evaluationInterval) {
        this.sloService = sloService;
        this.evaluationInterval = evaluationInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "slo-evaluator");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the periodic evaluation.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        log.info("Starting SLO scheduler with interval: {}", evaluationInterval);

        scheduler.scheduleAtFixedRate(
                this::evaluateAll,
                evaluationInterval.toMillis(),
                evaluationInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stops the scheduler.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("SLO scheduler stopped");
    }

    /**
     * Triggers evaluation of all SLOs.
     */
    public void evaluateAll() {
        try {
            List<SloEvaluation> evaluations = sloService.evaluateAll();

            // Log summary
            long healthy = evaluations.stream().filter(e -> e.status() == SloStatus.HEALTHY).count();
            long atRisk = evaluations.stream().filter(e -> e.status() == SloStatus.AT_RISK).count();
            long breached = evaluations.stream().filter(e -> e.status() == SloStatus.BREACHED).count();

            if (breached > 0 || atRisk > 0) {
                log.warn("SLO evaluation: {} healthy, {} at risk, {} breached",
                        healthy, atRisk, breached);
            } else {
                log.debug("SLO evaluation: {} healthy, {} at risk, {} breached",
                        healthy, atRisk, breached);
            }

        } catch (Exception e) {
            log.error("Error during SLO evaluation", e);
        }
    }

    /**
     * Checks if the scheduler is running.
     */
    public boolean isRunning() {
        return running;
    }
}
