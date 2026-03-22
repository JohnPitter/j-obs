package io.github.jobs.spring.security;

/**
 * Roles for J-Obs dashboard access control.
 * <p>
 * Defines three levels of access:
 * <ul>
 *   <li>{@link #ADMIN} - Full access to all features including configuration</li>
 *   <li>{@link #OPERATOR} - Can view data and start profiling, but cannot modify alerts/SLOs</li>
 *   <li>{@link #VIEWER} - Read-only access to dashboard, traces, logs, metrics</li>
 * </ul>
 */
public enum JObsRole {

    /** Full access - can configure alerts, SLOs, profiling, and view all data. */
    ADMIN,

    /** Can view all data, start profiling, but cannot modify alerts/SLOs. */
    OPERATOR,

    /** Read-only access to dashboard, traces, logs, metrics. */
    VIEWER;

    public boolean canModifyAlerts() {
        return this == ADMIN;
    }

    public boolean canModifySlos() {
        return this == ADMIN;
    }

    public boolean canStartProfiling() {
        return this == ADMIN || this == OPERATOR;
    }

    public boolean canViewData() {
        return true;
    }

    public boolean canExportData() {
        return this == ADMIN || this == OPERATOR;
    }
}
