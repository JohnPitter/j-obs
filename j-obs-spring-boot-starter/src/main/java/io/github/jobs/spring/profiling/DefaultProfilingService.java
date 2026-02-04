package io.github.jobs.spring.profiling;

import io.github.jobs.application.ProfilingService;
import io.github.jobs.domain.profiling.*;
import io.github.jobs.infrastructure.InMemoryProfilingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.lang.management.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default implementation of ProfilingService.
 * Uses JVM management APIs for profiling.
 * <p>
 * Implements {@link DisposableBean} for proper cleanup on Spring shutdown.
 */
public class DefaultProfilingService implements ProfilingService, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DefaultProfilingService.class);

    private final InMemoryProfilingRepository repository;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> runningProfiles;
    private final Map<String, CpuProfileCollector> collectors;

    public DefaultProfilingService(InMemoryProfilingRepository repository) {
        this.repository = repository;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "j-obs-profiler");
            t.setDaemon(true);
            return t;
        });
        this.runningProfiles = new ConcurrentHashMap<>();
        this.collectors = new ConcurrentHashMap<>();
    }

    @Override
    public ProfileSession startCpuProfile(Duration duration, Duration samplingInterval) {
        // Check if already profiling
        Optional<ProfileSession> existing = getRunningCpuProfile();
        if (existing.isPresent()) {
            throw new IllegalStateException("CPU profiling session already running: " + existing.get().id());
        }

        ProfileSession session = ProfileSession.cpu(duration, samplingInterval);
        repository.save(session);

        // Start the profiler
        CpuProfileCollector collector = new CpuProfileCollector(samplingInterval);
        collectors.put(session.id(), collector);

        // Update to running
        session = session.running();
        repository.save(session);

        log.info("Started CPU profiling session {} for {} with interval {}",
                session.id(), duration, samplingInterval);

        // Schedule sampling
        ScheduledFuture<?> samplingFuture = scheduler.scheduleAtFixedRate(
                collector::sample,
                0,
                samplingInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // Schedule stop
        String sessionId = session.id();
        ScheduledFuture<?> stopFuture = scheduler.schedule(
                () -> stopCpuProfileInternal(sessionId),
                duration.toMillis(),
                TimeUnit.MILLISECONDS
        );

        runningProfiles.put(session.id(), samplingFuture);

        return session;
    }

    @Override
    public ProfileSession stopCpuProfile(String sessionId) {
        return stopCpuProfileInternal(sessionId);
    }

    private synchronized ProfileSession stopCpuProfileInternal(String sessionId) {
        Optional<ProfileSession> sessionOpt = repository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        ProfileSession session = sessionOpt.get();
        if (!session.isActive()) {
            return session;
        }

        // Cancel the sampling
        ScheduledFuture<?> future = runningProfiles.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }

        // Get the collector and build results
        CpuProfileCollector collector = collectors.remove(sessionId);
        if (collector == null) {
            session = session.fail("No collector found");
            repository.save(session);
            return session;
        }

        try {
            session = session.stopping();
            repository.save(session);

            List<CpuSample> samples = collector.getSamples();
            ProfileResult.CpuProfileData cpuData = ProfileResult.CpuProfileData.from(
                    samples, session.samplingInterval());
            ProfileResult result = ProfileResult.cpu(cpuData, session.elapsed());

            session = session.complete(result);
            repository.save(session);

            log.info("Completed CPU profiling session {} with {} samples",
                    sessionId, cpuData.totalSamples());

            return session;
        } catch (Exception e) {
            log.error("Failed to complete CPU profiling session {}", sessionId, e);
            session = session.fail(e.getMessage());
            repository.save(session);
            return session;
        }
    }

    @Override
    public MemoryInfo captureMemorySnapshot() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        MemoryInfo.HeapMemory heap = new MemoryInfo.HeapMemory(
                heapUsage.getUsed(),
                heapUsage.getCommitted(),
                heapUsage.getMax(),
                heapUsage.getInit()
        );

        MemoryInfo.HeapMemory nonHeap = new MemoryInfo.HeapMemory(
                nonHeapUsage.getUsed(),
                nonHeapUsage.getCommitted(),
                nonHeapUsage.getMax(),
                nonHeapUsage.getInit()
        );

        List<MemoryInfo.MemoryPool> pools = poolBeans.stream()
                .map(pool -> {
                    MemoryUsage usage = pool.getUsage();
                    double usagePct = usage.getMax() > 0
                            ? (usage.getUsed() * 100.0) / usage.getMax()
                            : 0;
                    return new MemoryInfo.MemoryPool(
                            pool.getName(),
                            pool.getType().name(),
                            usage.getUsed(),
                            usage.getCommitted(),
                            usage.getMax(),
                            usagePct
                    );
                })
                .toList();

        List<MemoryInfo.GarbageCollector> gcs = gcBeans.stream()
                .map(gc -> new MemoryInfo.GarbageCollector(
                        gc.getName(),
                        gc.getCollectionCount(),
                        gc.getCollectionTime()
                ))
                .toList();

        MemoryInfo memoryInfo = new MemoryInfo(heap, nonHeap, pools, gcs, Instant.now());

        // Save as a session
        ProfileSession session = ProfileSession.memory();
        session = session.complete(ProfileResult.memory(memoryInfo));
        repository.save(session);

        log.debug("Captured memory snapshot: heap={}MB, non-heap={}MB",
                heapUsage.getUsed() / (1024 * 1024),
                nonHeapUsage.getUsed() / (1024 * 1024));

        return memoryInfo;
    }

    @Override
    public ThreadDump captureThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);

        List<ThreadDump.ThreadSnapshot> snapshots = Arrays.stream(threadInfos)
                .map(ThreadDump.ThreadSnapshot::from)
                .toList();

        ThreadDump dump = new ThreadDump(snapshots, Instant.now());

        // Save as a session
        ProfileSession session = ProfileSession.threadDump();
        session = session.complete(ProfileResult.threadDump(dump));
        repository.save(session);

        log.debug("Captured thread dump: {} threads", snapshots.size());

        return dump;
    }

    @Override
    public Optional<ProfileSession> getSession(String sessionId) {
        return repository.findById(sessionId);
    }

    @Override
    public List<ProfileSession> getAllSessions() {
        return repository.findAll();
    }

    @Override
    public List<ProfileSession> getActiveSessions() {
        return repository.findActive();
    }

    @Override
    public Optional<ProfileSession> getRunningCpuProfile() {
        return repository.findRunningCpuProfile();
    }

    @Override
    public boolean cancelSession(String sessionId) {
        Optional<ProfileSession> sessionOpt = repository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        ProfileSession session = sessionOpt.get();
        if (!session.isActive()) {
            return false;
        }

        // Cancel any scheduled tasks
        ScheduledFuture<?> future = runningProfiles.remove(sessionId);
        if (future != null) {
            future.cancel(true);
        }
        collectors.remove(sessionId);

        session = session.cancel();
        repository.save(session);

        log.info("Cancelled profiling session {}", sessionId);
        return true;
    }

    @Override
    public void clearCompletedSessions() {
        repository.clearCompleted();
    }

    @Override
    public boolean isCpuProfilingSupported() {
        return true; // Simple sampling is always supported
    }

    @Override
    public ProfilingStats getStats() {
        Map<ProfileStatus, Long> counts = repository.countByStatus();

        long active = counts.getOrDefault(ProfileStatus.STARTING, 0L)
                + counts.getOrDefault(ProfileStatus.RUNNING, 0L)
                + counts.getOrDefault(ProfileStatus.STOPPING, 0L);

        long completed = counts.getOrDefault(ProfileStatus.COMPLETED, 0L);
        long failed = counts.getOrDefault(ProfileStatus.FAILED, 0L)
                + counts.getOrDefault(ProfileStatus.CANCELLED, 0L);

        return new ProfilingStats(
                repository.count(),
                (int) active,
                (int) completed,
                (int) failed,
                true,  // CPU profiling supported
                true,  // Memory profiling supported
                true   // Thread dump supported
        );
    }

    /**
     * Shuts down the profiling service.
     */
    public void shutdown() {
        // Cancel all running profiles
        runningProfiles.forEach((id, future) -> future.cancel(true));
        runningProfiles.clear();
        collectors.clear();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Called by Spring when the bean is destroyed.
     */
    @Override
    public void destroy() {
        shutdown();
    }

    /**
     * Collector for CPU profile samples.
     */
    private static class CpuProfileCollector {
        private final Duration samplingInterval;
        private final Map<String, SampleData> samples = new ConcurrentHashMap<>();
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        CpuProfileCollector(Duration samplingInterval) {
            this.samplingInterval = samplingInterval;
        }

        void sample() {
            ThreadInfo[] threads = threadBean.dumpAllThreads(false, false);
            for (ThreadInfo thread : threads) {
                if (thread.getThreadState() == Thread.State.RUNNABLE) {
                    StackTraceElement[] stackTrace = thread.getStackTrace();
                    if (stackTrace.length > 0) {
                        String key = buildKey(stackTrace);
                        samples.computeIfAbsent(key, k -> new SampleData(stackTrace))
                                .increment();
                    }
                }
            }
        }

        List<CpuSample> getSamples() {
            long total = samples.values().stream().mapToLong(s -> s.count).sum();
            if (total == 0) {
                return List.of();
            }

            return samples.values().stream()
                    .map(data -> {
                        List<CpuSample.StackFrame> frames = Arrays.stream(data.stackTrace)
                                .map(CpuSample.StackFrame::from)
                                .toList();
                        double pct = (data.count * 100.0) / total;
                        return new CpuSample(frames, data.count, pct);
                    })
                    .sorted((a, b) -> Long.compare(b.sampleCount(), a.sampleCount()))
                    .toList();
        }

        private String buildKey(StackTraceElement[] stackTrace) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement e : stackTrace) {
                sb.append(e.getClassName()).append(".")
                        .append(e.getMethodName()).append(":")
                        .append(e.getLineNumber()).append(";");
            }
            return sb.toString();
        }

        private static class SampleData {
            final StackTraceElement[] stackTrace;
            volatile long count = 0;

            SampleData(StackTraceElement[] stackTrace) {
                this.stackTrace = stackTrace;
            }

            void increment() {
                count++;
            }
        }
    }
}
