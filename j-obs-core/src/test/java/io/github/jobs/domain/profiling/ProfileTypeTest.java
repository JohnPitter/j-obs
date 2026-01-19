package io.github.jobs.domain.profiling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTypeTest {

    @Test
    void shouldHaveDisplayName() {
        assertEquals("CPU Profile", ProfileType.CPU.displayName());
        assertEquals("Memory Profile", ProfileType.MEMORY.displayName());
        assertEquals("Thread Dump", ProfileType.THREAD.displayName());
        assertEquals("Allocation Profile", ProfileType.ALLOCATION.displayName());
    }

    @Test
    void shouldHaveDescription() {
        assertNotNull(ProfileType.CPU.description());
        assertFalse(ProfileType.CPU.description().isEmpty());
    }

    @Test
    void shouldHaveAllTypes() {
        assertEquals(4, ProfileType.values().length);
    }
}
