package io.github.jobs.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyStatusTest {

    private final Dependency requiredDep = Dependency.builder()
            .className("com.example.Required")
            .displayName("Required Dep")
            .groupId("com.example")
            .artifactId("required")
            .required(true)
            .build();

    private final Dependency optionalDep = Dependency.builder()
            .className("com.example.Optional")
            .displayName("Optional Dep")
            .groupId("com.example")
            .artifactId("optional")
            .required(false)
            .build();

    @Test
    void shouldCreateFoundStatusWithVersion() {
        DependencyStatus status = DependencyStatus.found(requiredDep, "1.0.0");

        assertThat(status.isFound()).isTrue();
        assertThat(status.isNotFound()).isFalse();
        assertThat(status.isError()).isFalse();
        assertThat(status.version()).hasValue("1.0.0");
        assertThat(status.dependency()).isEqualTo(requiredDep);
        assertThat(status.status()).isEqualTo(DependencyStatus.Status.FOUND);
    }

    @Test
    void shouldCreateFoundStatusWithoutVersion() {
        DependencyStatus status = DependencyStatus.found(requiredDep);

        assertThat(status.isFound()).isTrue();
        assertThat(status.version()).isEmpty();
    }

    @Test
    void shouldCreateNotFoundStatus() {
        DependencyStatus status = DependencyStatus.notFound(requiredDep);

        assertThat(status.isNotFound()).isTrue();
        assertThat(status.isFound()).isFalse();
        assertThat(status.status()).isEqualTo(DependencyStatus.Status.NOT_FOUND);
    }

    @Test
    void shouldCreateErrorStatus() {
        DependencyStatus status = DependencyStatus.error(requiredDep, "Load failed");

        assertThat(status.isError()).isTrue();
        assertThat(status.errorMessage()).hasValue("Load failed");
        assertThat(status.status()).isEqualTo(DependencyStatus.Status.ERROR);
    }

    @Test
    void shouldIdentifyMissingRequiredDependency() {
        DependencyStatus status = DependencyStatus.notFound(requiredDep);

        assertThat(status.isMissingRequired()).isTrue();
        assertThat(status.isMissingOptional()).isFalse();
    }

    @Test
    void shouldIdentifyMissingOptionalDependency() {
        DependencyStatus status = DependencyStatus.notFound(optionalDep);

        assertThat(status.isMissingRequired()).isFalse();
        assertThat(status.isMissingOptional()).isTrue();
    }

    @Test
    void shouldNotBeMissingWhenFound() {
        DependencyStatus status = DependencyStatus.found(requiredDep);

        assertThat(status.isMissingRequired()).isFalse();
        assertThat(status.isMissingOptional()).isFalse();
    }
}
