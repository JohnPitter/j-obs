package io.github.jobs.infrastructure;

import io.github.jobs.domain.profiling.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryProfilingRepositoryTest {

    private InMemoryProfilingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProfilingRepository(10);
    }

    @Test
    void shouldSaveAndFindSession() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        repository.save(session);

        Optional<ProfileSession> found = repository.findById(session.id());
        assertTrue(found.isPresent());
        assertEquals(session.id(), found.get().id());
    }

    @Test
    void shouldReturnEmptyForUnknownSession() {
        Optional<ProfileSession> found = repository.findById("unknown");
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindAllSessions() {
        repository.save(ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)));
        repository.save(ProfileSession.memory());
        repository.save(ProfileSession.threadDump());

        List<ProfileSession> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void shouldFindActiveSessions() {
        ProfileSession active = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        ProfileSession completed = ProfileSession.memory().complete(null);

        repository.save(active);
        repository.save(completed);

        List<ProfileSession> activeSessions = repository.findActive();
        assertEquals(1, activeSessions.size());
        assertEquals(active.id(), activeSessions.get(0).id());
    }

    @Test
    void shouldFindCompletedSessions() {
        ProfileSession active = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        ProfileSession completed = ProfileSession.memory().complete(null);

        repository.save(active);
        repository.save(completed);

        List<ProfileSession> completedSessions = repository.findCompleted();
        assertEquals(1, completedSessions.size());
        assertEquals(completed.id(), completedSessions.get(0).id());
    }

    @Test
    void shouldFindRunningCpuProfile() {
        ProfileSession running = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)).running();
        repository.save(running);

        Optional<ProfileSession> found = repository.findRunningCpuProfile();
        assertTrue(found.isPresent());
        assertEquals(running.id(), found.get().id());
    }

    @Test
    void shouldReturnEmptyWhenNoCpuProfileRunning() {
        repository.save(ProfileSession.memory());

        Optional<ProfileSession> found = repository.findRunningCpuProfile();
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindSessionsByType() {
        repository.save(ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)));
        repository.save(ProfileSession.memory());
        repository.save(ProfileSession.memory());

        List<ProfileSession> memorySessions = repository.findByType(ProfileType.MEMORY);
        assertEquals(2, memorySessions.size());
    }

    @Test
    void shouldDeleteSession() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        repository.save(session);

        assertTrue(repository.delete(session.id()));
        assertTrue(repository.findById(session.id()).isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistent() {
        assertFalse(repository.delete("unknown"));
    }

    @Test
    void shouldClearCompletedSessions() {
        ProfileSession active = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        ProfileSession completed = ProfileSession.memory().complete(null);

        repository.save(active);
        repository.save(completed);

        repository.clearCompleted();

        assertEquals(1, repository.count());
        assertTrue(repository.findById(active.id()).isPresent());
        assertTrue(repository.findById(completed.id()).isEmpty());
    }

    @Test
    void shouldClearAllSessions() {
        repository.save(ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)));
        repository.save(ProfileSession.memory());

        repository.clearAll();

        assertEquals(0, repository.count());
    }

    @Test
    void shouldCountSessions() {
        assertEquals(0, repository.count());

        repository.save(ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)));
        assertEquals(1, repository.count());

        repository.save(ProfileSession.memory());
        assertEquals(2, repository.count());
    }

    @Test
    void shouldCountByStatus() {
        repository.save(ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)));
        repository.save(ProfileSession.memory().complete(null));
        repository.save(ProfileSession.threadDump().fail("error"));

        var counts = repository.countByStatus();

        assertEquals(1L, counts.getOrDefault(ProfileStatus.STARTING, 0L));
        assertEquals(1L, counts.getOrDefault(ProfileStatus.COMPLETED, 0L));
        assertEquals(1L, counts.getOrDefault(ProfileStatus.FAILED, 0L));
    }

    @Test
    void shouldEvictOldSessionsWhenMaxReached() {
        InMemoryProfilingRepository smallRepo = new InMemoryProfilingRepository(3);

        // Add 5 completed sessions
        for (int i = 0; i < 5; i++) {
            ProfileSession session = ProfileSession.memory().complete(null);
            smallRepo.save(session);
        }

        // Should have evicted down to max
        assertTrue(smallRepo.count() <= 3);
    }
}
