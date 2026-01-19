package io.github.jobs.domain.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertGroupStatusTest {

    @Test
    void shouldIdentifyTerminalStatuses() {
        assertFalse(AlertGroupStatus.PENDING.isTerminal());
        assertTrue(AlertGroupStatus.SENT.isTerminal());
        assertTrue(AlertGroupStatus.FAILED.isTerminal());
    }

    @Test
    void shouldIdentifyAcceptingEventsStatuses() {
        assertTrue(AlertGroupStatus.PENDING.isAcceptingEvents());
        assertFalse(AlertGroupStatus.SENT.isAcceptingEvents());
        assertFalse(AlertGroupStatus.FAILED.isAcceptingEvents());
    }

    @Test
    void shouldHaveThreeStatuses() {
        assertEquals(3, AlertGroupStatus.values().length);
    }
}
