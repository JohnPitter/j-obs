package io.github.jobs.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregates the results of checking all dependencies.
 * Provides convenient methods to query the overall status and filter by category.
 */
public final class DependencyCheckResult {

    public enum OverallStatus {
        /** All required dependencies are present */
        COMPLETE,
        /** One or more required dependencies are missing */
        INCOMPLETE,
        /** Error occurred during check */
        ERROR
    }

    private final List<DependencyStatus> statuses;
    private final Instant checkedAt;
    private final OverallStatus overallStatus;

    private DependencyCheckResult(List<DependencyStatus> statuses, Instant checkedAt) {
        this.statuses = Collections.unmodifiableList(Objects.requireNonNull(statuses, "statuses is required"));
        this.checkedAt = Objects.requireNonNull(checkedAt, "checkedAt is required");
        this.overallStatus = calculateOverallStatus(statuses);
    }

    public static DependencyCheckResult of(List<DependencyStatus> statuses) {
        return new DependencyCheckResult(statuses, Instant.now());
    }

    public static DependencyCheckResult of(List<DependencyStatus> statuses, Instant checkedAt) {
        return new DependencyCheckResult(statuses, checkedAt);
    }

    private static OverallStatus calculateOverallStatus(List<DependencyStatus> statuses) {
        boolean hasError = statuses.stream().anyMatch(DependencyStatus::isError);
        if (hasError) {
            return OverallStatus.ERROR;
        }

        boolean hasMissingRequired = statuses.stream().anyMatch(DependencyStatus::isMissingRequired);
        return hasMissingRequired ? OverallStatus.INCOMPLETE : OverallStatus.COMPLETE;
    }

    public List<DependencyStatus> statuses() {
        return statuses;
    }

    public Instant checkedAt() {
        return checkedAt;
    }

    public OverallStatus overallStatus() {
        return overallStatus;
    }

    public boolean isComplete() {
        return overallStatus == OverallStatus.COMPLETE;
    }

    public boolean isIncomplete() {
        return overallStatus == OverallStatus.INCOMPLETE;
    }

    public boolean hasError() {
        return overallStatus == OverallStatus.ERROR;
    }

    public List<DependencyStatus> found() {
        return statuses.stream()
                .filter(DependencyStatus::isFound)
                .collect(Collectors.toList());
    }

    public List<DependencyStatus> missingRequired() {
        return statuses.stream()
                .filter(DependencyStatus::isMissingRequired)
                .collect(Collectors.toList());
    }

    public List<DependencyStatus> missingOptional() {
        return statuses.stream()
                .filter(DependencyStatus::isMissingOptional)
                .collect(Collectors.toList());
    }

    public List<DependencyStatus> errors() {
        return statuses.stream()
                .filter(DependencyStatus::isError)
                .collect(Collectors.toList());
    }

    public int totalCount() {
        return statuses.size();
    }

    public int foundCount() {
        return (int) statuses.stream().filter(DependencyStatus::isFound).count();
    }

    public int missingRequiredCount() {
        return (int) statuses.stream().filter(DependencyStatus::isMissingRequired).count();
    }

    public int missingOptionalCount() {
        return (int) statuses.stream().filter(DependencyStatus::isMissingOptional).count();
    }

    @Override
    public String toString() {
        return "DependencyCheckResult{" +
                "overallStatus=" + overallStatus +
                ", found=" + foundCount() +
                ", missingRequired=" + missingRequiredCount() +
                ", missingOptional=" + missingOptionalCount() +
                ", checkedAt=" + checkedAt +
                '}';
    }
}
