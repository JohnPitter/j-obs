package io.github.jobs.spring.trace;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Configurable trace sampler that determines whether a trace should be recorded.
 * Uses probability-based sampling to reduce overhead in high-traffic environments.
 * <p>
 * Thread-safe: uses {@link ThreadLocalRandom} for random sampling and deterministic
 * hash-based sampling for consistent decisions across distributed services.
 */
public class TraceSampler {

    private final double sampleRate;

    /**
     * Creates a new TraceSampler with the given sample rate.
     *
     * @param sampleRate value between 0.0 (sample nothing) and 1.0 (sample everything)
     * @throws IllegalArgumentException if sampleRate is outside [0.0, 1.0]
     */
    public TraceSampler(double sampleRate) {
        if (sampleRate < 0.0 || sampleRate > 1.0) {
            throw new IllegalArgumentException(
                    "Sample rate must be between 0.0 and 1.0, got: " + sampleRate);
        }
        this.sampleRate = sampleRate;
    }

    /**
     * Returns true if this trace should be sampled (recorded).
     * Always returns true if sampleRate is 1.0.
     * Always returns false if sampleRate is 0.0.
     */
    public boolean shouldSample() {
        if (sampleRate >= 1.0) return true;
        if (sampleRate <= 0.0) return false;
        return ThreadLocalRandom.current().nextDouble() < sampleRate;
    }

    /**
     * Returns true if a trace with the given ID should be sampled.
     * Uses hash-based deterministic sampling so the same traceId
     * always gets the same decision (important for distributed tracing).
     *
     * @param traceId the trace identifier; if null, falls back to random sampling
     */
    public boolean shouldSample(String traceId) {
        if (sampleRate >= 1.0) return true;
        if (sampleRate <= 0.0) return false;
        if (traceId == null) return shouldSample();
        // Deterministic: same traceId always sampled/dropped consistently
        int hash = traceId.hashCode() & Integer.MAX_VALUE;
        return (hash % 10000) < (int) (sampleRate * 10000);
    }

    public double getSampleRate() {
        return sampleRate;
    }
}
