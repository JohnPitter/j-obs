package io.github.jobs.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the status of a dependency check.
 * Contains the dependency info and whether it was found, along with version if detected.
 */
public final class DependencyStatus {

    public enum Status {
        FOUND,
        NOT_FOUND,
        ERROR
    }

    private final Dependency dependency;
    private final Status status;
    private final String version;
    private final String errorMessage;

    private DependencyStatus(Dependency dependency, Status status, String version, String errorMessage) {
        this.dependency = Objects.requireNonNull(dependency, "dependency is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.version = version;
        this.errorMessage = errorMessage;
    }

    public static DependencyStatus found(Dependency dependency, String version) {
        return new DependencyStatus(dependency, Status.FOUND, version, null);
    }

    public static DependencyStatus found(Dependency dependency) {
        return found(dependency, null);
    }

    public static DependencyStatus notFound(Dependency dependency) {
        return new DependencyStatus(dependency, Status.NOT_FOUND, null, null);
    }

    public static DependencyStatus error(Dependency dependency, String errorMessage) {
        return new DependencyStatus(dependency, Status.ERROR, null, errorMessage);
    }

    public Dependency dependency() {
        return dependency;
    }

    public Status status() {
        return status;
    }

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public boolean isFound() {
        return status == Status.FOUND;
    }

    public boolean isNotFound() {
        return status == Status.NOT_FOUND;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isMissingRequired() {
        return !isFound() && dependency.isRequired();
    }

    public boolean isMissingOptional() {
        return !isFound() && !dependency.isRequired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyStatus that = (DependencyStatus) o;
        return Objects.equals(dependency, that.dependency) && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, status);
    }

    @Override
    public String toString() {
        return "DependencyStatus{" +
                "dependency=" + dependency.displayName() +
                ", status=" + status +
                ", version=" + version +
                '}';
    }
}
