package io.github.jobs.application;

import io.github.jobs.domain.profiling.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing profiling operations.
 */
public interface ProfilingService {

    /**
     * Starts a CPU profiling session.
     *
     * @param duration         the duration to profile
     * @param samplingInterval the interval between samples
     * @return the started session
     */
    ProfileSession startCpuProfile(Duration duration, Duration samplingInterval);

    /**
     * Stops a running CPU profiling session.
     *
     * @param sessionId the session ID
     * @return the completed session with results
     */
    ProfileSession stopCpuProfile(String sessionId);

    /**
     * Captures a memory snapshot.
     *
     * @return the memory info
     */
    MemoryInfo captureMemorySnapshot();

    /**
     * Captures a thread dump.
     *
     * @return the thread dump
     */
    ThreadDump captureThreadDump();

    /**
     * Gets a profiling session by ID.
     *
     * @param sessionId the session ID
     * @return the session if found
     */
    Optional<ProfileSession> getSession(String sessionId);

    /**
     * Gets all profiling sessions.
     *
     * @return list of all sessions
     */
    List<ProfileSession> getAllSessions();

    /**
     * Gets all active profiling sessions.
     *
     * @return list of active sessions
     */
    List<ProfileSession> getActiveSessions();

    /**
     * Gets the currently running CPU profile session, if any.
     *
     * @return the running session if present
     */
    Optional<ProfileSession> getRunningCpuProfile();

    /**
     * Cancels a running profiling session.
     *
     * @param sessionId the session ID
     * @return true if cancelled
     */
    boolean cancelSession(String sessionId);

    /**
     * Clears completed profiling sessions.
     */
    void clearCompletedSessions();

    /**
     * Checks if CPU profiling is supported on this JVM.
     *
     * @return true if supported
     */
    boolean isCpuProfilingSupported();

    /**
     * Gets profiling statistics.
     *
     * @return profiling stats
     */
    ProfilingStats getStats();

    /**
     * Profiling statistics.
     */
    record ProfilingStats(
            int totalSessions,
            int activeSessions,
            int completedSessions,
            int failedSessions,
            boolean cpuProfilingSupported,
            boolean memoryProfilingSupported,
            boolean threadDumpSupported
    ) {}
}
