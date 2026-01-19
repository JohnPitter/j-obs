package io.github.jobs.spring.slo;

import io.github.jobs.application.SloRepository;
import io.github.jobs.application.SloService;
import io.github.jobs.domain.slo.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of SloService.
 * Evaluates SLOs against Micrometer metrics.
 */
public class DefaultSloService implements SloService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSloService.class);

    private final SloRepository repository;
    private final MeterRegistry meterRegistry;
    private final SloEvaluator evaluator;

    public DefaultSloService(SloRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.evaluator = new SloEvaluator(meterRegistry);
    }

    @Override
    public void register(Slo slo) {
        log.info("Registering SLO: {} (objective: {}%)", slo.name(), slo.objective());
        repository.save(slo);
    }

    @Override
    public boolean unregister(String name) {
        log.info("Unregistering SLO: {}", name);
        return repository.delete(name);
    }

    @Override
    public Optional<Slo> getSlo(String name) {
        return repository.findByName(name);
    }

    @Override
    public List<Slo> getAllSlos() {
        return repository.findAll();
    }

    @Override
    public SloEvaluation evaluate(String name) {
        Optional<Slo> sloOpt = repository.findByName(name);
        if (sloOpt.isEmpty()) {
            throw new IllegalArgumentException("SLO not found: " + name);
        }

        Slo slo = sloOpt.get();
        SloEvaluation evaluation = evaluator.evaluate(slo);
        repository.saveEvaluation(evaluation);

        log.debug("Evaluated SLO {}: {} ({}%)", name, evaluation.status(),
                String.format("%.2f", evaluation.currentValue()));

        return evaluation;
    }

    @Override
    public List<SloEvaluation> evaluateAll() {
        List<Slo> slos = repository.findAll();
        List<SloEvaluation> evaluations = new ArrayList<>();

        for (Slo slo : slos) {
            try {
                SloEvaluation evaluation = evaluator.evaluate(slo);
                repository.saveEvaluation(evaluation);
                evaluations.add(evaluation);
            } catch (Exception e) {
                log.error("Error evaluating SLO {}: {}", slo.name(), e.getMessage());
                evaluations.add(SloEvaluation.noData(slo));
            }
        }

        return evaluations;
    }

    @Override
    public Optional<SloEvaluation> getLatestEvaluation(String name) {
        return repository.getLatestEvaluation(name);
    }

    @Override
    public List<SloEvaluation> getAllLatestEvaluations() {
        return repository.getAllLatestEvaluations();
    }

    @Override
    public List<SloEvaluation> getEvaluationHistory(String name, int limit) {
        return repository.getEvaluationHistory(name, limit);
    }

    @Override
    public SloSummary getSummary() {
        List<SloEvaluation> evaluations = getAllLatestEvaluations();
        return SloSummary.from(evaluations);
    }

    /**
     * Internal evaluator for SLOs.
     */
    private static class SloEvaluator {
        private final MeterRegistry registry;

        SloEvaluator(MeterRegistry registry) {
            this.registry = registry;
        }

        SloEvaluation evaluate(Slo slo) {
            Sli sli = slo.sli();

            return switch (sli.type()) {
                case AVAILABILITY -> evaluateAvailability(slo);
                case LATENCY -> evaluateLatency(slo);
                case ERROR_RATE -> evaluateErrorRate(slo);
                case THROUGHPUT -> evaluateThroughput(slo);
            };
        }

        private SloEvaluation evaluateAvailability(Slo slo) {
            // Look for HTTP request metrics
            String metricName = slo.sli().metric();

            // Try to find success/total counters
            long successCount = getCounterValue(metricName, "status", "2xx", "3xx");
            long errorCount = getCounterValue(metricName, "status", "4xx", "5xx");
            long totalCount = successCount + errorCount;

            if (totalCount == 0) {
                return SloEvaluation.noData(slo);
            }

            double availability = (successCount * 100.0) / totalCount;

            return buildEvaluation(slo, availability, successCount, totalCount);
        }

        private SloEvaluation evaluateLatency(Slo slo) {
            Sli sli = slo.sli();
            String metricName = sli.metric();

            // Find timer and get percentile
            Timer timer = registry.find(metricName).timer();
            if (timer == null) {
                return SloEvaluation.noData(slo);
            }

            double threshold = sli.threshold() != null ? sli.threshold() : 200.0;
            int percentile = sli.percentile() != null ? sli.percentile() : 99;

            // Get the percentile value
            double percentileValue = timer.percentile(percentile / 100.0, java.util.concurrent.TimeUnit.MILLISECONDS);

            // Calculate what percentage of requests meet the threshold
            // This is an approximation - in reality we'd need histogram buckets
            double successRate;
            if (percentileValue <= threshold) {
                successRate = 100.0; // All requests below p99 meet threshold
            } else {
                // Estimate based on how far over threshold we are
                successRate = Math.max(0, 100.0 - ((percentileValue - threshold) / threshold * 10));
            }

            long totalRequests = timer.count();

            return buildEvaluation(slo, successRate, (long)(totalRequests * successRate / 100), totalRequests);
        }

        private SloEvaluation evaluateErrorRate(Slo slo) {
            String metricName = slo.sli().metric();

            long errorCount = getCounterValue(metricName, "status", "5xx");
            long totalCount = getCounterValue(metricName, null, null);

            if (totalCount == 0) {
                return SloEvaluation.noData(slo);
            }

            double errorRate = (errorCount * 100.0) / totalCount;
            // For error rate SLO, the "good" state is when error rate is LOW
            // So we invert: if objective is 99.9% (meaning 0.1% max errors)
            double successRate = 100.0 - errorRate;

            return buildEvaluation(slo, successRate, totalCount - errorCount, totalCount);
        }

        private SloEvaluation evaluateThroughput(Slo slo) {
            String metricName = slo.sli().metric();
            Double thresholdObj = slo.sli().threshold();

            if (thresholdObj == null) {
                return SloEvaluation.noData(slo);
            }

            double threshold = thresholdObj;

            // Find counter or timer
            Counter counter = registry.find(metricName).counter();
            if (counter != null) {
                double rate = counter.count(); // This is total, not rate
                double successRate = rate >= threshold ? 100.0 : (rate / threshold * 100.0);
                return buildEvaluation(slo, successRate, (long) rate, (long) threshold);
            }

            Timer timer = registry.find(metricName).timer();
            if (timer != null) {
                double rate = timer.count();
                double successRate = rate >= threshold ? 100.0 : (rate / threshold * 100.0);
                return buildEvaluation(slo, successRate, (long) rate, (long) threshold);
            }

            return SloEvaluation.noData(slo);
        }

        private long getCounterValue(String metricName, String tagKey, String... tagValues) {
            if (tagKey == null) {
                Counter counter = registry.find(metricName).counter();
                return counter != null ? (long) counter.count() : 0;
            }

            long total = 0;
            for (String tagValue : tagValues) {
                Search search = registry.find(metricName);
                if (tagValue.endsWith("xx")) {
                    // Handle 2xx, 3xx, etc. patterns
                    String prefix = tagValue.substring(0, 1);
                    for (int i = 0; i <= 99; i++) {
                        String status = prefix + String.format("%02d", i);
                        Counter counter = registry.find(metricName).tag(tagKey, status).counter();
                        if (counter != null) {
                            total += (long) counter.count();
                        }
                    }
                } else {
                    Counter counter = search.tag(tagKey, tagValue).counter();
                    if (counter != null) {
                        total += (long) counter.count();
                    }
                }
            }
            return total;
        }

        private SloEvaluation buildEvaluation(Slo slo, double currentValue, long goodEvents, long totalEvents) {
            SloStatus status = slo.determineStatus(currentValue);
            ErrorBudget errorBudget = slo.calculateErrorBudget(currentValue);

            // Calculate burn rates for different windows
            List<BurnRate> burnRates = calculateBurnRates(slo, currentValue, errorBudget);

            return SloEvaluation.builder()
                    .slo(slo)
                    .currentValue(currentValue)
                    .status(status)
                    .errorBudget(errorBudget)
                    .burnRates(burnRates)
                    .events(goodEvents, totalEvents)
                    .evaluatedAt(Instant.now())
                    .build();
        }

        private List<BurnRate> calculateBurnRates(Slo slo, double currentValue, ErrorBudget errorBudget) {
            List<BurnRate> rates = new ArrayList<>();

            // Short window (1 hour)
            rates.add(BurnRate.calculate(
                    errorBudget.consumedBudget(),
                    errorBudget.totalBudget(),
                    Duration.ofHours(1),
                    slo.window(),
                    Duration.ofHours(1)
            ));

            // Long window (6 hours)
            rates.add(BurnRate.calculate(
                    errorBudget.consumedBudget(),
                    errorBudget.totalBudget(),
                    Duration.ofHours(6),
                    slo.window(),
                    Duration.ofHours(6)
            ));

            return rates;
        }
    }
}
