package io.github.jobs.domain.profiling;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a thread dump snapshot.
 */
public record ThreadDump(
        List<ThreadSnapshot> threads,
        Instant capturedAt
) {
    public ThreadDump {
        Objects.requireNonNull(threads, "threads cannot be null");
        Objects.requireNonNull(capturedAt, "capturedAt cannot be null");
    }

    /**
     * Returns the total number of threads.
     */
    public int threadCount() {
        return threads.size();
    }

    /**
     * Returns threads grouped by state.
     */
    public Map<Thread.State, List<ThreadSnapshot>> byState() {
        return threads.stream()
                .collect(Collectors.groupingBy(ThreadSnapshot::state));
    }

    /**
     * Returns the count of threads in each state.
     */
    public Map<Thread.State, Long> stateCounts() {
        return threads.stream()
                .collect(Collectors.groupingBy(ThreadSnapshot::state, Collectors.counting()));
    }

    /**
     * Returns threads that are blocked.
     */
    public List<ThreadSnapshot> blockedThreads() {
        return threads.stream()
                .filter(t -> t.state() == Thread.State.BLOCKED)
                .toList();
    }

    /**
     * Returns threads that are waiting (WAITING or TIMED_WAITING).
     */
    public List<ThreadSnapshot> waitingThreads() {
        return threads.stream()
                .filter(t -> t.state() == Thread.State.WAITING || t.state() == Thread.State.TIMED_WAITING)
                .toList();
    }

    /**
     * Returns threads that are runnable.
     */
    public List<ThreadSnapshot> runnableThreads() {
        return threads.stream()
                .filter(t -> t.state() == Thread.State.RUNNABLE)
                .toList();
    }

    /**
     * Detects potential deadlocks by finding cycles in lock ownership.
     */
    public List<ThreadSnapshot> detectDeadlocks() {
        // Simplified deadlock detection - look for blocked threads waiting on locks held by other blocked threads
        List<ThreadSnapshot> blocked = blockedThreads();
        return blocked.stream()
                .filter(t -> {
                    if (t.lockOwnerId() <= 0) return false;
                    return blocked.stream()
                            .anyMatch(other -> other.threadId() == t.lockOwnerId());
                })
                .toList();
    }

    /**
     * Represents a snapshot of a single thread.
     */
    public record ThreadSnapshot(
            long threadId,
            String threadName,
            Thread.State state,
            boolean daemon,
            int priority,
            List<CpuSample.StackFrame> stackTrace,
            String lockName,
            long lockOwnerId,
            String lockOwnerName,
            List<String> lockedMonitors,
            List<String> lockedSynchronizers
    ) {
        public ThreadSnapshot {
            Objects.requireNonNull(threadName, "threadName cannot be null");
            Objects.requireNonNull(state, "state cannot be null");
            Objects.requireNonNull(stackTrace, "stackTrace cannot be null");
        }

        /**
         * Checks if this thread is waiting on a lock.
         */
        public boolean isWaitingOnLock() {
            return lockName != null && !lockName.isEmpty();
        }

        /**
         * Checks if this thread holds any locks.
         */
        public boolean holdsLocks() {
            return (lockedMonitors != null && !lockedMonitors.isEmpty()) ||
                   (lockedSynchronizers != null && !lockedSynchronizers.isEmpty());
        }

        /**
         * Returns the top of the stack trace.
         */
        public CpuSample.StackFrame topFrame() {
            return stackTrace.isEmpty() ? null : stackTrace.get(0);
        }

        /**
         * Returns a summary of what this thread is doing.
         */
        public String summary() {
            if (stackTrace.isEmpty()) {
                return state.name();
            }
            CpuSample.StackFrame top = topFrame();
            return state.name() + " at " + top.simpleClassName() + "." + top.methodName();
        }

        /**
         * Creates a ThreadSnapshot from a ThreadInfo.
         */
        public static ThreadSnapshot from(java.lang.management.ThreadInfo info) {
            List<CpuSample.StackFrame> frames = java.util.Arrays.stream(info.getStackTrace())
                    .map(CpuSample.StackFrame::from)
                    .toList();

            List<String> monitors = java.util.Arrays.stream(info.getLockedMonitors())
                    .map(m -> m.getClassName() + "@" + Integer.toHexString(m.getIdentityHashCode()))
                    .toList();

            List<String> synchronizers = java.util.Arrays.stream(info.getLockedSynchronizers())
                    .map(s -> s.getClassName() + "@" + Integer.toHexString(System.identityHashCode(s)))
                    .toList();

            return new ThreadSnapshot(
                    info.getThreadId(),
                    info.getThreadName(),
                    info.getThreadState(),
                    info.isDaemon(),
                    info.getPriority(),
                    frames,
                    info.getLockName(),
                    info.getLockOwnerId(),
                    info.getLockOwnerName(),
                    monitors,
                    synchronizers
            );
        }
    }
}
