package io.github.jobs.spring.web;

import io.github.jobs.application.ProfilingService;
import io.github.jobs.domain.profiling.*;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for application profiling operations.
 *
 * <p>Provides endpoints for CPU profiling, memory analysis, and thread dumps
 * to help identify performance bottlenecks and memory issues.</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/profiling/stats} - Get profiling statistics</li>
 *   <li>{@code GET /api/profiling/sessions} - List profiling sessions</li>
 *   <li>{@code GET /api/profiling/sessions/{id}} - Get specific session</li>
 *   <li>{@code POST /api/profiling/cpu/start} - Start CPU profiling</li>
 *   <li>{@code POST /api/profiling/cpu/{id}/stop} - Stop CPU profiling</li>
 *   <li>{@code GET /api/profiling/cpu/running} - Get running CPU profile</li>
 *   <li>{@code GET /api/profiling/sessions/{id}/cpu} - Get CPU results</li>
 *   <li>{@code POST /api/profiling/memory} - Capture memory snapshot</li>
 *   <li>{@code GET /api/profiling/memory} - Get latest memory snapshot</li>
 *   <li>{@code POST /api/profiling/threads} - Capture thread dump</li>
 *   <li>{@code GET /api/profiling/threads} - Get latest thread dump</li>
 *   <li>{@code POST /api/profiling/sessions/{id}/cancel} - Cancel session</li>
 *   <li>{@code DELETE /api/profiling/sessions} - Clear completed sessions</li>
 * </ul>
 *
 * <h2>Profile Types</h2>
 * <ul>
 *   <li>CPU - Sample-based CPU profiling with flame graph support</li>
 *   <li>MEMORY - Heap and non-heap memory analysis with GC info</li>
 *   <li>THREAD - Thread dump with blocking analysis</li>
 * </ul>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see io.github.jobs.domain.profiling.ProfileSession
 * @see io.github.jobs.domain.profiling.ProfileResult
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/profiling")
@ConditionalOnBean(ProfilingService.class)
public class ProfilingApiController {

    private final ProfilingService profilingService;
    private final JObsProperties properties;

    public ProfilingApiController(ProfilingService profilingService, JObsProperties properties) {
        this.profilingService = profilingService;
        this.properties = properties;
    }

    /**
     * Gets profiling statistics.
     */
    @GetMapping("/stats")
    public ProfilingService.ProfilingStats getStats() {
        return profilingService.getStats();
    }

    /**
     * Lists all profiling sessions.
     */
    @GetMapping("/sessions")
    public List<ProfileSessionDto> getSessions(
            @RequestParam(required = false) ProfileType type,
            @RequestParam(required = false) ProfileStatus status) {
        List<ProfileSession> sessions = profilingService.getAllSessions();

        return sessions.stream()
                .filter(s -> type == null || s.type() == type)
                .filter(s -> status == null || s.status() == status)
                .map(ProfileSessionDto::from)
                .toList();
    }

    /**
     * Gets a specific profiling session.
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ProfileSessionDto> getSession(@PathVariable String id) {
        return profilingService.getSession(id)
                .map(ProfileSessionDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Starts a CPU profiling session.
     */
    @PostMapping("/cpu/start")
    public ResponseEntity<ProfileSessionDto> startCpuProfile(
            @RequestParam(required = false) Long durationSeconds,
            @RequestParam(required = false) Long intervalMs) {

        JObsProperties.Profiling config = properties.getProfiling();

        Duration duration = durationSeconds != null
                ? Duration.ofSeconds(durationSeconds)
                : config.getDefaultDuration();

        // Enforce max duration
        if (duration.compareTo(config.getMaxDuration()) > 0) {
            duration = config.getMaxDuration();
        }

        Duration interval = intervalMs != null
                ? Duration.ofMillis(intervalMs)
                : config.getDefaultSamplingInterval();

        try {
            ProfileSession session = profilingService.startCpuProfile(duration, interval);
            return ResponseEntity.ok(ProfileSessionDto.from(session));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Stops a running CPU profiling session.
     */
    @PostMapping("/cpu/{id}/stop")
    public ResponseEntity<ProfileSessionDto> stopCpuProfile(@PathVariable String id) {
        try {
            ProfileSession session = profilingService.stopCpuProfile(id);
            return ResponseEntity.ok(ProfileSessionDto.from(session));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gets the running CPU profile session.
     */
    @GetMapping("/cpu/running")
    public ResponseEntity<ProfileSessionDto> getRunningCpuProfile() {
        return profilingService.getRunningCpuProfile()
                .map(ProfileSessionDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Gets CPU profile results.
     */
    @GetMapping("/sessions/{id}/cpu")
    public ResponseEntity<CpuProfileDto> getCpuProfileResults(@PathVariable String id) {
        return profilingService.getSession(id)
                .filter(s -> s.result() != null && s.result().isCpuProfile())
                .map(s -> CpuProfileDto.from(s.result().cpuData()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Captures a memory snapshot.
     */
    @PostMapping("/memory")
    public MemoryInfoDto captureMemory() {
        MemoryInfo info = profilingService.captureMemorySnapshot();
        return MemoryInfoDto.from(info);
    }

    /**
     * Gets the latest memory snapshot.
     */
    @GetMapping("/memory")
    public ResponseEntity<MemoryInfoDto> getLatestMemory() {
        return profilingService.getAllSessions().stream()
                .filter(s -> s.result() != null && s.result().isMemoryProfile())
                .findFirst()
                .map(s -> MemoryInfoDto.from(s.result().memoryData()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Captures a thread dump.
     */
    @PostMapping("/threads")
    public ThreadDumpDto captureThreadDump() {
        ThreadDump dump = profilingService.captureThreadDump();
        return ThreadDumpDto.from(dump);
    }

    /**
     * Gets the latest thread dump.
     */
    @GetMapping("/threads")
    public ResponseEntity<ThreadDumpDto> getLatestThreadDump() {
        return profilingService.getAllSessions().stream()
                .filter(s -> s.result() != null && s.result().isThreadDump())
                .findFirst()
                .map(s -> ThreadDumpDto.from(s.result().threadData()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Cancels a running profiling session.
     */
    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<Void> cancelSession(@PathVariable String id) {
        boolean cancelled = profilingService.cancelSession(id);
        return cancelled ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Clears completed profiling sessions.
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<Void> clearSessions() {
        profilingService.clearCompletedSessions();
        return ResponseEntity.ok().build();
    }

    // DTOs

    public record ProfileSessionDto(
            String id,
            ProfileType type,
            ProfileStatus status,
            long durationMs,
            long samplingIntervalMs,
            String startedAt,
            String completedAt,
            String error,
            int progressPercentage,
            boolean hasResult
    ) {
        public static ProfileSessionDto from(ProfileSession session) {
            return new ProfileSessionDto(
                    session.id(),
                    session.type(),
                    session.status(),
                    session.duration() != null ? session.duration().toMillis() : 0,
                    session.samplingInterval() != null ? session.samplingInterval().toMillis() : 0,
                    session.startedAt() != null ? session.startedAt().toString() : null,
                    session.completedAt() != null ? session.completedAt().toString() : null,
                    session.error(),
                    session.progressPercentage(),
                    session.result() != null
            );
        }
    }

    public record CpuProfileDto(
            long totalSamples,
            long samplingIntervalMs,
            List<HotMethodDto> hotMethods,
            List<SampleDto> topSamples
    ) {
        public static CpuProfileDto from(ProfileResult.CpuProfileData data) {
            List<HotMethodDto> hotMethods = data.hotMethods().stream()
                    .limit(20)
                    .map(HotMethodDto::from)
                    .toList();

            List<SampleDto> topSamples = data.topSamples(10).stream()
                    .map(SampleDto::from)
                    .toList();

            return new CpuProfileDto(
                    data.totalSamples(),
                    data.samplingInterval() != null ? data.samplingInterval().toMillis() : 0,
                    hotMethods,
                    topSamples
            );
        }
    }

    public record HotMethodDto(
            String className,
            String methodName,
            String fullName,
            long selfTime,
            long totalTime,
            double percentage
    ) {
        public static HotMethodDto from(FlameGraphNode.HotMethod method) {
            return new HotMethodDto(
                    method.className(),
                    method.methodName(),
                    method.fullName(),
                    method.selfTime(),
                    method.totalTime(),
                    method.percentage()
            );
        }
    }

    public record SampleDto(
            String topMethod,
            long sampleCount,
            double percentage,
            List<String> stackTrace
    ) {
        public static SampleDto from(CpuSample sample) {
            CpuSample.StackFrame top = sample.topFrame();
            return new SampleDto(
                    top != null ? top.format() : "unknown",
                    sample.sampleCount(),
                    sample.percentage(),
                    sample.stackTrace().stream()
                            .limit(10)
                            .map(CpuSample.StackFrame::format)
                            .toList()
            );
        }
    }

    public record MemoryInfoDto(
            HeapDto heap,
            HeapDto nonHeap,
            List<PoolDto> pools,
            List<GcDto> garbageCollectors,
            String capturedAt
    ) {
        public static MemoryInfoDto from(MemoryInfo info) {
            return new MemoryInfoDto(
                    HeapDto.from(info.heap(), "Heap"),
                    HeapDto.from(info.nonHeap(), "Non-Heap"),
                    info.memoryPools().stream().map(PoolDto::from).toList(),
                    info.garbageCollectors().stream().map(GcDto::from).toList(),
                    info.capturedAt().toString()
            );
        }
    }

    public record HeapDto(
            String name,
            long used,
            long committed,
            long max,
            String usedFormatted,
            String maxFormatted,
            double usagePercentage
    ) {
        public static HeapDto from(MemoryInfo.HeapMemory memory, String name) {
            return new HeapDto(
                    name,
                    memory.used(),
                    memory.committed(),
                    memory.max(),
                    memory.formatUsed(),
                    memory.formatMax(),
                    memory.usagePercentage()
            );
        }
    }

    public record PoolDto(
            String name,
            String type,
            long used,
            long max,
            String usedFormatted,
            double usagePercentage
    ) {
        public static PoolDto from(MemoryInfo.MemoryPool pool) {
            return new PoolDto(
                    pool.name(),
                    pool.type(),
                    pool.used(),
                    pool.max(),
                    pool.formatUsed(),
                    pool.usagePercentage()
            );
        }
    }

    public record GcDto(
            String name,
            long collectionCount,
            long collectionTimeMs,
            String formattedTime
    ) {
        public static GcDto from(MemoryInfo.GarbageCollector gc) {
            return new GcDto(
                    gc.name(),
                    gc.collectionCount(),
                    gc.collectionTimeMs(),
                    gc.formatCollectionTime()
            );
        }
    }

    public record ThreadDumpDto(
            int threadCount,
            Map<String, Long> stateCounts,
            List<ThreadSnapshotDto> threads,
            List<ThreadSnapshotDto> blockedThreads,
            String capturedAt
    ) {
        public static ThreadDumpDto from(ThreadDump dump) {
            Map<String, Long> counts = dump.stateCounts().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().name(),
                            Map.Entry::getValue
                    ));

            return new ThreadDumpDto(
                    dump.threadCount(),
                    counts,
                    dump.threads().stream()
                            .limit(100)
                            .map(ThreadSnapshotDto::from)
                            .toList(),
                    dump.blockedThreads().stream()
                            .map(ThreadSnapshotDto::from)
                            .toList(),
                    dump.capturedAt().toString()
            );
        }
    }

    public record ThreadSnapshotDto(
            long threadId,
            String threadName,
            String state,
            boolean daemon,
            String summary,
            boolean waitingOnLock,
            String lockName,
            List<String> stackTrace
    ) {
        public static ThreadSnapshotDto from(ThreadDump.ThreadSnapshot snapshot) {
            return new ThreadSnapshotDto(
                    snapshot.threadId(),
                    snapshot.threadName(),
                    snapshot.state().name(),
                    snapshot.daemon(),
                    snapshot.summary(),
                    snapshot.isWaitingOnLock(),
                    snapshot.lockName(),
                    snapshot.stackTrace().stream()
                            .limit(20)
                            .map(CpuSample.StackFrame::format)
                            .toList()
            );
        }
    }
}
