package io.github.jobs.domain.profiling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileStatusTest {

    @Test
    void shouldIdentifyTerminalStates() {
        assertTrue(ProfileStatus.COMPLETED.isTerminal());
        assertTrue(ProfileStatus.FAILED.isTerminal());
        assertTrue(ProfileStatus.CANCELLED.isTerminal());

        assertFalse(ProfileStatus.STARTING.isTerminal());
        assertFalse(ProfileStatus.RUNNING.isTerminal());
        assertFalse(ProfileStatus.STOPPING.isTerminal());
    }

    @Test
    void shouldIdentifyActiveStates() {
        assertTrue(ProfileStatus.STARTING.isActive());
        assertTrue(ProfileStatus.RUNNING.isActive());
        assertTrue(ProfileStatus.STOPPING.isActive());

        assertFalse(ProfileStatus.COMPLETED.isActive());
        assertFalse(ProfileStatus.FAILED.isActive());
        assertFalse(ProfileStatus.CANCELLED.isActive());
    }

    @Test
    void shouldHaveDisplayName() {
        assertEquals("Running", ProfileStatus.RUNNING.displayName());
        assertEquals("Completed", ProfileStatus.COMPLETED.displayName());
    }

    @Test
    void shouldHaveAllStatuses() {
        assertEquals(6, ProfileStatus.values().length);
    }
}
