package io.github.jobs.infrastructure;

import io.github.jobs.domain.profiling.ProfileSession;
import io.github.jobs.domain.profiling.ProfileStatus;
import io.github.jobs.domain.profiling.ProfileType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for profiling sessions.
 */
public class InMemoryProfilingRepository {

    private static final int DEFAULT_MAX_SESSIONS = 100;

    private final Map<String, ProfileSession> sessions = new ConcurrentHashMap<>();
    private final int maxSessions;

    public InMemoryProfilingRepository(int maxSessions) {
        this.maxSessions = maxSessions;
    }

    public InMemoryProfilingRepository() {
        this(DEFAULT_MAX_SESSIONS);
    }

    /**
     * Saves a profiling session.
     */
    public void save(ProfileSession session) {
        Objects.requireNonNull(session, "session cannot be null");
        sessions.put(session.id(), session);
        evictOldSessions();
    }

    /**
     * Finds a session by ID.
     */
    public Optional<ProfileSession> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    /**
     * Returns all sessions.
     */
    public List<ProfileSession> findAll() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(ProfileSession::startedAt).reversed())
                .toList();
    }

    /**
     * Returns all active sessions.
     */
    public List<ProfileSession> findActive() {
        return sessions.values().stream()
                .filter(ProfileSession::isActive)
                .sorted(Comparator.comparing(ProfileSession::startedAt).reversed())
                .toList();
    }

    /**
     * Returns all completed sessions.
     */
    public List<ProfileSession> findCompleted() {
        return sessions.values().stream()
                .filter(ProfileSession::isCompleted)
                .sorted(Comparator.comparing(ProfileSession::startedAt).reversed())
                .toList();
    }

    /**
     * Finds the running CPU profile session.
     */
    public Optional<ProfileSession> findRunningCpuProfile() {
        return sessions.values().stream()
                .filter(s -> s.type() == ProfileType.CPU)
                .filter(s -> s.status() == ProfileStatus.RUNNING || s.status() == ProfileStatus.STARTING)
                .findFirst();
    }

    /**
     * Finds sessions by type.
     */
    public List<ProfileSession> findByType(ProfileType type) {
        return sessions.values().stream()
                .filter(s -> s.type() == type)
                .sorted(Comparator.comparing(ProfileSession::startedAt).reversed())
                .toList();
    }

    /**
     * Deletes a session.
     */
    public boolean delete(String id) {
        return sessions.remove(id) != null;
    }

    /**
     * Clears all completed sessions.
     */
    public void clearCompleted() {
        sessions.entrySet().removeIf(e -> e.getValue().status().isTerminal());
    }

    /**
     * Clears all sessions.
     */
    public void clearAll() {
        sessions.clear();
    }

    /**
     * Returns the count of sessions.
     */
    public int count() {
        return sessions.size();
    }

    /**
     * Returns counts by status.
     */
    public Map<ProfileStatus, Long> countByStatus() {
        return sessions.values().stream()
                .collect(Collectors.groupingBy(ProfileSession::status, Collectors.counting()));
    }

    private void evictOldSessions() {
        if (sessions.size() <= maxSessions) {
            return;
        }

        // Remove oldest completed sessions first
        List<String> toRemove = sessions.values().stream()
                .filter(s -> s.status().isTerminal())
                .sorted(Comparator.comparing(ProfileSession::startedAt))
                .limit(sessions.size() - maxSessions)
                .map(ProfileSession::id)
                .toList();

        toRemove.forEach(sessions::remove);
    }
}
