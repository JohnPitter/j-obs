package io.github.jobs.domain.profiling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a profiling session.
 */
public record ProfileResult(
        ProfileType type,
        Duration duration,
        Instant capturedAt,
        CpuProfileData cpuData,
        MemoryInfo memoryData,
        ThreadDump threadData
) {
    public ProfileResult {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
    }

    /**
     * Creates a CPU profile result.
     */
    public static ProfileResult cpu(CpuProfileData cpuData, Duration duration) {
        return new ProfileResult(ProfileType.CPU, duration, Instant.now(), cpuData, null, null);
    }

    /**
     * Creates a memory profile result.
     */
    public static ProfileResult memory(MemoryInfo memoryInfo) {
        return new ProfileResult(ProfileType.MEMORY, Duration.ZERO, Instant.now(), null, memoryInfo, null);
    }

    /**
     * Creates a thread dump result.
     */
    public static ProfileResult threadDump(ThreadDump threadDump) {
        return new ProfileResult(ProfileType.THREAD, Duration.ZERO, Instant.now(), null, null, threadDump);
    }

    /**
     * CPU profiling data.
     */
    public record CpuProfileData(
            long totalSamples,
            Duration samplingInterval,
            List<CpuSample> samples,
            FlameGraphNode flameGraph,
            List<FlameGraphNode.HotMethod> hotMethods
    ) {
        public CpuProfileData {
            Objects.requireNonNull(samples, "samples cannot be null");
        }

        /**
         * Creates CPU profile data from samples.
         */
        public static CpuProfileData from(List<CpuSample> samples, Duration samplingInterval) {
            long totalSamples = samples.stream().mapToLong(CpuSample::sampleCount).sum();

            // Build flame graph
            FlameGraphNode flameGraph = FlameGraphNode.root();
            for (CpuSample sample : samples) {
                flameGraph.addSample(sample.stackTrace(), sample.sampleCount());
            }

            // Find hot methods
            List<FlameGraphNode.HotMethod> hotMethods = flameGraph.findHotMethods(20);

            return new CpuProfileData(totalSamples, samplingInterval, samples, flameGraph, hotMethods);
        }

        /**
         * Returns the top N samples by count.
         */
        public List<CpuSample> topSamples(int limit) {
            return samples.stream()
                    .sorted((a, b) -> Long.compare(b.sampleCount(), a.sampleCount()))
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * Checks if this is a CPU profile.
     */
    public boolean isCpuProfile() {
        return type == ProfileType.CPU && cpuData != null;
    }

    /**
     * Checks if this is a memory profile.
     */
    public boolean isMemoryProfile() {
        return type == ProfileType.MEMORY && memoryData != null;
    }

    /**
     * Checks if this is a thread dump.
     */
    public boolean isThreadDump() {
        return type == ProfileType.THREAD && threadData != null;
    }
}
