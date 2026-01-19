package io.github.jobs.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyCheckResultTest {

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
    void shouldBeCompleteWhenAllRequiredFound() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.found(requiredDep, "1.0.0"),
                DependencyStatus.found(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.isComplete()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.overallStatus()).isEqualTo(DependencyCheckResult.OverallStatus.COMPLETE);
    }

    @Test
    void shouldBeIncompleteWhenRequiredMissing() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.notFound(requiredDep),
                DependencyStatus.found(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.isComplete()).isFalse();
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.overallStatus()).isEqualTo(DependencyCheckResult.OverallStatus.INCOMPLETE);
    }

    @Test
    void shouldBeCompleteWhenOnlyOptionalMissing() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.found(requiredDep),
                DependencyStatus.notFound(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.isComplete()).isTrue();
    }

    @Test
    void shouldHaveErrorStatusWhenErrorOccurred() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.error(requiredDep, "Error loading"),
                DependencyStatus.found(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.hasError()).isTrue();
        assertThat(result.overallStatus()).isEqualTo(DependencyCheckResult.OverallStatus.ERROR);
    }

    @Test
    void shouldFilterFoundDependencies() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.found(requiredDep, "1.0.0"),
                DependencyStatus.notFound(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.found()).hasSize(1);
        assertThat(result.found().get(0).dependency()).isEqualTo(requiredDep);
    }

    @Test
    void shouldFilterMissingRequiredDependencies() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.notFound(requiredDep),
                DependencyStatus.notFound(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.missingRequired()).hasSize(1);
        assertThat(result.missingRequired().get(0).dependency()).isEqualTo(requiredDep);
    }

    @Test
    void shouldFilterMissingOptionalDependencies() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.notFound(requiredDep),
                DependencyStatus.notFound(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.missingOptional()).hasSize(1);
        assertThat(result.missingOptional().get(0).dependency()).isEqualTo(optionalDep);
    }

    @Test
    void shouldCountCorrectly() {
        List<DependencyStatus> statuses = List.of(
                DependencyStatus.found(requiredDep, "1.0.0"),
                DependencyStatus.notFound(optionalDep)
        );

        DependencyCheckResult result = DependencyCheckResult.of(statuses);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.foundCount()).isEqualTo(1);
        assertThat(result.missingRequiredCount()).isEqualTo(0);
        assertThat(result.missingOptionalCount()).isEqualTo(1);
    }

    @Test
    void shouldHaveCheckedAtTimestamp() {
        DependencyCheckResult result = DependencyCheckResult.of(List.of());

        assertThat(result.checkedAt()).isNotNull();
    }
}
