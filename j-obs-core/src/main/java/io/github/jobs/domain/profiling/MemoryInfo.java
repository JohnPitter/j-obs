package io.github.jobs.domain.profiling;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents memory information snapshot.
 */
public record MemoryInfo(
        HeapMemory heap,
        HeapMemory nonHeap,
        List<MemoryPool> memoryPools,
        List<GarbageCollector> garbageCollectors,
        Instant capturedAt
) {
    public MemoryInfo {
        Objects.requireNonNull(heap, "heap cannot be null");
        Objects.requireNonNull(nonHeap, "nonHeap cannot be null");
        Objects.requireNonNull(memoryPools, "memoryPools cannot be null");
        Objects.requireNonNull(garbageCollectors, "garbageCollectors cannot be null");
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
    }

    /**
     * Returns the total used memory (heap + non-heap).
     */
    public long totalUsed() {
        return heap.used() + nonHeap.used();
    }

    /**
     * Returns the total committed memory.
     */
    public long totalCommitted() {
        return heap.committed() + nonHeap.committed();
    }

    /**
     * Returns the heap usage percentage.
     */
    public double heapUsagePercentage() {
        if (heap.max() <= 0) return 0;
        return (heap.used() * 100.0) / heap.max();
    }

    /**
     * Represents heap or non-heap memory.
     */
    public record HeapMemory(
            long used,
            long committed,
            long max,
            long init
    ) {
        public HeapMemory {
            if (used < 0) throw new IllegalArgumentException("used cannot be negative");
            if (committed < 0) throw new IllegalArgumentException("committed cannot be negative");
        }

        /**
         * Returns the usage percentage (used/max).
         */
        public double usagePercentage() {
            if (max <= 0) return 0;
            return (used * 100.0) / max;
        }

        /**
         * Returns the free memory (max - used).
         */
        public long free() {
            return max > 0 ? max - used : committed - used;
        }

        /**
         * Formats the used memory as a human-readable string.
         */
        public String formatUsed() {
            return formatBytes(used);
        }

        /**
         * Formats the max memory as a human-readable string.
         */
        public String formatMax() {
            return max > 0 ? formatBytes(max) : "N/A";
        }

        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
            return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Represents a memory pool (e.g., Eden Space, Old Gen).
     */
    public record MemoryPool(
            String name,
            String type,
            long used,
            long committed,
            long max,
            double usagePercentage
    ) {
        public MemoryPool {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }

        public String formatUsed() {
            return HeapMemory.formatBytes(used);
        }

        public String formatMax() {
            return max > 0 ? HeapMemory.formatBytes(max) : "N/A";
        }
    }

    /**
     * Represents garbage collector statistics.
     */
    public record GarbageCollector(
            String name,
            long collectionCount,
            long collectionTimeMs
    ) {
        public GarbageCollector {
            Objects.requireNonNull(name, "name cannot be null");
        }

        /**
         * Returns the average collection time.
         */
        public double averageCollectionTimeMs() {
            if (collectionCount == 0) return 0;
            return (double) collectionTimeMs / collectionCount;
        }

        /**
         * Formats the total collection time.
         */
        public String formatCollectionTime() {
            if (collectionTimeMs < 1000) {
                return collectionTimeMs + "ms";
            }
            return String.format(Locale.US, "%.2fs", collectionTimeMs / 1000.0);
        }
    }
}
