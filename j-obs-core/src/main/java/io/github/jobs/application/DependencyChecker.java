package io.github.jobs.application;

import io.github.jobs.domain.DependencyCheckResult;

/**
 * Port interface for checking dependencies.
 * Implementations handle the actual classpath inspection.
 */
public interface DependencyChecker {

    /**
     * Checks all known dependencies and returns the result.
     * May be cached by implementations for performance.
     *
     * @return the result of checking all dependencies
     */
    DependencyCheckResult check();

    /**
     * Forces a fresh check, bypassing any cache.
     *
     * @return the result of checking all dependencies
     */
    DependencyCheckResult checkFresh();

    /**
     * Invalidates any cached results.
     */
    void invalidateCache();
}
