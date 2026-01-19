package io.github.jobs.domain.profiling;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryInfoTest {

    @Test
    void shouldCreateMemoryInfo() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(
                512 * 1024 * 1024L, // 512MB used
                1024 * 1024 * 1024L, // 1GB committed
                2048L * 1024 * 1024, // 2GB max
                256 * 1024 * 1024L  // 256MB init
        );

        MemoryInfo.HeapMemory nonHeap = new MemoryInfo.HeapMemory(
                64 * 1024 * 1024L, // 64MB used
                128 * 1024 * 1024L, // 128MB committed
                -1, // no max
                32 * 1024 * 1024L  // 32MB init
        );

        MemoryInfo info = new MemoryInfo(heap, nonHeap, List.of(), List.of(), Instant.now());

        assertEquals(heap, info.heap());
        assertEquals(nonHeap, info.nonHeap());
    }

    @Test
    void shouldCalculateTotalUsed() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(500, 1000, 2000, 100);
        MemoryInfo.HeapMemory nonHeap = new MemoryInfo.HeapMemory(100, 200, 500, 50);

        MemoryInfo info = new MemoryInfo(heap, nonHeap, List.of(), List.of(), Instant.now());

        assertEquals(600, info.totalUsed());
    }

    @Test
    void shouldCalculateHeapUsagePercentage() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(500, 1000, 1000, 100);
        MemoryInfo.HeapMemory nonHeap = new MemoryInfo.HeapMemory(0, 0, 0, 0);

        MemoryInfo info = new MemoryInfo(heap, nonHeap, List.of(), List.of(), Instant.now());

        assertEquals(50.0, info.heapUsagePercentage(), 0.01);
    }

    @Test
    void shouldCalculateHeapMemoryUsage() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(512, 1024, 2048, 256);

        assertEquals(25.0, heap.usagePercentage(), 0.01);
        assertEquals(1536, heap.free());
    }

    @Test
    void shouldFormatHeapMemory() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(
                512 * 1024 * 1024L,
                1024 * 1024 * 1024L,
                2048L * 1024 * 1024,
                256 * 1024 * 1024L
        );

        assertEquals("512.0 MB", heap.formatUsed());
        assertEquals("2.00 GB", heap.formatMax());
    }

    @Test
    void shouldFormatMaxAsNAWhenNegative() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(100, 200, -1, 50);

        assertEquals("N/A", heap.formatMax());
    }

    @Test
    void shouldCreateMemoryPool() {
        MemoryInfo.MemoryPool pool = new MemoryInfo.MemoryPool(
                "G1 Eden Space", "HEAP", 100 * 1024 * 1024L, 200 * 1024 * 1024L, 500 * 1024 * 1024L, 20.0
        );

        assertEquals("G1 Eden Space", pool.name());
        assertEquals("HEAP", pool.type());
        assertEquals("100.0 MB", pool.formatUsed());
    }

    @Test
    void shouldCreateGarbageCollector() {
        MemoryInfo.GarbageCollector gc = new MemoryInfo.GarbageCollector(
                "G1 Young Generation", 150, 1500
        );

        assertEquals("G1 Young Generation", gc.name());
        assertEquals(150, gc.collectionCount());
        assertEquals(1500, gc.collectionTimeMs());
        assertEquals(10.0, gc.averageCollectionTimeMs(), 0.01);
        assertEquals("1.50s", gc.formatCollectionTime());
    }

    @Test
    void shouldFormatShortGcTime() {
        MemoryInfo.GarbageCollector gc = new MemoryInfo.GarbageCollector("GC", 10, 500);

        assertEquals("500ms", gc.formatCollectionTime());
    }

    @Test
    void shouldRejectNullParameters() {
        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(0, 0, 0, 0);

        assertThrows(NullPointerException.class, () ->
                new MemoryInfo(null, heap, List.of(), List.of(), Instant.now()));

        assertThrows(NullPointerException.class, () ->
                new MemoryInfo(heap, null, List.of(), List.of(), Instant.now()));
    }
}
