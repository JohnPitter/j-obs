package io.github.jobs.domain.alert;

/**
 * Represents the lifecycle status of an {@link AlertGroup}.
 *
 * <p>Groups transition through these states:</p>
 * <pre>
 * PENDING ──┬──→ SENT
 *           └──→ FAILED
 * </pre>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertGroup
 */
public enum AlertGroupStatus {
    /**
     * Group is waiting for more events or for the group wait time to expire.
     */
    PENDING,

    /**
     * Group notification has been sent.
     */
    SENT,

    /**
     * Group notification failed to send.
     */
    FAILED;

    /**
     * Checks if this status is terminal (no more changes expected).
     */
    public boolean isTerminal() {
        return this == SENT || this == FAILED;
    }

    /**
     * Checks if the group is still accepting new events.
     */
    public boolean isAcceptingEvents() {
        return this == PENDING;
    }
}
