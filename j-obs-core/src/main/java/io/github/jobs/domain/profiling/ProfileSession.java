package io.github.jobs.domain.profiling;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a profiling session.
 */
public record ProfileSession(
        String id,
        ProfileType type,
        ProfileStatus status,
        Duration duration,
        Duration samplingInterval,
        Instant startedAt,
        Instant completedAt,
        String error,
        ProfileResult result
) {
    public ProfileSession {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
    }

    /**
     * Creates a new profiling session.
     */
    public static ProfileSession create(ProfileType type, Duration duration, Duration samplingInterval) {
        return new ProfileSession(
                UUID.randomUUID().toString(),
                type,
                ProfileStatus.STARTING,
                duration,
                samplingInterval,
                Instant.now(),
                null,
                null,
                null
        );
    }

    /**
     * Creates a new CPU profiling session.
     */
    public static ProfileSession cpu(Duration duration, Duration samplingInterval) {
        return create(ProfileType.CPU, duration, samplingInterval);
    }

    /**
     * Creates a new memory profiling session.
     */
    public static ProfileSession memory() {
        return create(ProfileType.MEMORY, Duration.ZERO, Duration.ZERO);
    }

    /**
     * Creates a new thread dump session.
     */
    public static ProfileSession threadDump() {
        return create(ProfileType.THREAD, Duration.ZERO, Duration.ZERO);
    }

    /**
     * Transitions to running state.
     */
    public ProfileSession running() {
        return new ProfileSession(id, type, ProfileStatus.RUNNING, duration, samplingInterval,
                startedAt, null, null, null);
    }

    /**
     * Transitions to stopping state.
     */
    public ProfileSession stopping() {
        return new ProfileSession(id, type, ProfileStatus.STOPPING, duration, samplingInterval,
                startedAt, null, null, null);
    }

    /**
     * Completes the session with a result.
     */
    public ProfileSession complete(ProfileResult result) {
        return new ProfileSession(id, type, ProfileStatus.COMPLETED, duration, samplingInterval,
                startedAt, Instant.now(), null, result);
    }

    /**
     * Fails the session with an error.
     */
    public ProfileSession fail(String error) {
        return new ProfileSession(id, type, ProfileStatus.FAILED, duration, samplingInterval,
                startedAt, Instant.now(), error, null);
    }

    /**
     * Cancels the session.
     */
    public ProfileSession cancel() {
        return new ProfileSession(id, type, ProfileStatus.CANCELLED, duration, samplingInterval,
                startedAt, Instant.now(), null, null);
    }

    /**
     * Returns the elapsed time since the session started.
     */
    public Duration elapsed() {
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    /**
     * Returns the remaining time for the session.
     */
    public Duration remaining() {
        if (duration == null || duration.isZero()) {
            return Duration.ZERO;
        }
        Duration elapsed = elapsed();
        Duration remaining = duration.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Returns the progress percentage (0-100).
     */
    public int progressPercentage() {
        if (duration == null || duration.isZero()) {
            return status.isTerminal() ? 100 : 0;
        }
        long elapsed = elapsed().toMillis();
        long total = duration.toMillis();
        return (int) Math.min(100, (elapsed * 100) / total);
    }

    /**
     * Checks if this is a CPU profiling session.
     */
    public boolean isCpuProfile() {
        return type == ProfileType.CPU;
    }

    /**
     * Checks if this is a memory profiling session.
     */
    public boolean isMemoryProfile() {
        return type == ProfileType.MEMORY;
    }

    /**
     * Checks if this is a thread dump.
     */
    public boolean isThreadDump() {
        return type == ProfileType.THREAD;
    }

    /**
     * Checks if the session is active.
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Checks if the session has completed.
     */
    public boolean isCompleted() {
        return status == ProfileStatus.COMPLETED;
    }

    /**
     * Checks if the session has failed.
     */
    public boolean isFailed() {
        return status == ProfileStatus.FAILED;
    }
}
