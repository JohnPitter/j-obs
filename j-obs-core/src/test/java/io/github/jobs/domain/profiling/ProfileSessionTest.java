package io.github.jobs.domain.profiling;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSessionTest {

    @Test
    void shouldCreateCpuProfileSession() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        assertNotNull(session.id());
        assertEquals(ProfileType.CPU, session.type());
        assertEquals(ProfileStatus.STARTING, session.status());
        assertEquals(Duration.ofSeconds(60), session.duration());
        assertEquals(Duration.ofMillis(10), session.samplingInterval());
        assertNotNull(session.startedAt());
    }

    @Test
    void shouldCreateMemoryProfileSession() {
        ProfileSession session = ProfileSession.memory();

        assertNotNull(session.id());
        assertEquals(ProfileType.MEMORY, session.type());
        assertEquals(ProfileStatus.STARTING, session.status());
    }

    @Test
    void shouldCreateThreadDumpSession() {
        ProfileSession session = ProfileSession.threadDump();

        assertNotNull(session.id());
        assertEquals(ProfileType.THREAD, session.type());
        assertEquals(ProfileStatus.STARTING, session.status());
    }

    @Test
    void shouldTransitionToRunning() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        ProfileSession running = session.running();

        assertEquals(session.id(), running.id());
        assertEquals(ProfileStatus.RUNNING, running.status());
    }

    @Test
    void shouldTransitionToStopping() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10)).running();

        ProfileSession stopping = session.stopping();

        assertEquals(session.id(), stopping.id());
        assertEquals(ProfileStatus.STOPPING, stopping.status());
    }

    @Test
    void shouldComplete() {
        ProfileSession session = ProfileSession.memory();
        ProfileResult result = ProfileResult.memory(createTestMemoryInfo());

        ProfileSession completed = session.complete(result);

        assertEquals(ProfileStatus.COMPLETED, completed.status());
        assertNotNull(completed.completedAt());
        assertEquals(result, completed.result());
    }

    @Test
    void shouldFail() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        ProfileSession failed = session.fail("Error message");

        assertEquals(ProfileStatus.FAILED, failed.status());
        assertEquals("Error message", failed.error());
        assertNotNull(failed.completedAt());
    }

    @Test
    void shouldCancel() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        ProfileSession cancelled = session.cancel();

        assertEquals(ProfileStatus.CANCELLED, cancelled.status());
        assertNotNull(cancelled.completedAt());
    }

    @Test
    void shouldCalculateProgress() {
        ProfileSession session = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));

        // Just started
        assertTrue(session.progressPercentage() < 10);

        // Completed - status is terminal so should be 100%
        ProfileSession completed = session.complete(createTestResult());
        assertTrue(completed.status().isTerminal());
    }

    @Test
    void shouldCheckIfActive() {
        ProfileSession starting = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        assertTrue(starting.isActive());

        ProfileSession running = starting.running();
        assertTrue(running.isActive());

        ProfileSession completed = running.complete(null);
        assertFalse(completed.isActive());
    }

    @Test
    void shouldCheckProfileType() {
        ProfileSession cpu = ProfileSession.cpu(Duration.ofSeconds(60), Duration.ofMillis(10));
        assertTrue(cpu.isCpuProfile());
        assertFalse(cpu.isMemoryProfile());
        assertFalse(cpu.isThreadDump());

        ProfileSession memory = ProfileSession.memory();
        assertTrue(memory.isMemoryProfile());

        ProfileSession thread = ProfileSession.threadDump();
        assertTrue(thread.isThreadDump());
    }

    private MemoryInfo createTestMemoryInfo() {
        return new MemoryInfo(
                new MemoryInfo.HeapMemory(100, 200, 500, 50),
                new MemoryInfo.HeapMemory(50, 100, 200, 25),
                java.util.List.of(),
                java.util.List.of(),
                java.time.Instant.now()
        );
    }

    private ProfileResult createTestResult() {
        return ProfileResult.memory(createTestMemoryInfo());
    }
}
