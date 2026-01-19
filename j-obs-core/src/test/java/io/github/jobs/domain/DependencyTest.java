package io.github.jobs.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyTest {

    @Test
    void shouldCreateDependencyWithAllFields() {
        Dependency dep = Dependency.builder()
                .className("com.example.Test")
                .displayName("Test Library")
                .groupId("com.example")
                .artifactId("test-lib")
                .required(true)
                .description("A test library")
                .documentationUrl("https://example.com/docs")
                .build();

        assertThat(dep.className()).isEqualTo("com.example.Test");
        assertThat(dep.displayName()).isEqualTo("Test Library");
        assertThat(dep.groupId()).isEqualTo("com.example");
        assertThat(dep.artifactId()).isEqualTo("test-lib");
        assertThat(dep.isRequired()).isTrue();
        assertThat(dep.description()).isEqualTo("A test library");
        assertThat(dep.documentationUrl()).isEqualTo("https://example.com/docs");
    }

    @Test
    void shouldCreateOptionalDependency() {
        Dependency dep = Dependency.builder()
                .className("com.example.Optional")
                .displayName("Optional Library")
                .groupId("com.example")
                .artifactId("optional-lib")
                .required(false)
                .build();

        assertThat(dep.isRequired()).isFalse();
    }

    @Test
    void shouldReturnMavenCoordinates() {
        Dependency dep = Dependency.builder()
                .className("com.example.Test")
                .displayName("Test")
                .groupId("com.example")
                .artifactId("test-lib")
                .build();

        assertThat(dep.mavenCoordinates()).isEqualTo("com.example:test-lib");
    }

    @Test
    void shouldRequireClassName() {
        assertThatThrownBy(() -> Dependency.builder()
                .displayName("Test")
                .groupId("com.example")
                .artifactId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("className");
    }

    @Test
    void shouldRequireDisplayName() {
        assertThatThrownBy(() -> Dependency.builder()
                .className("com.example.Test")
                .groupId("com.example")
                .artifactId("test")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("displayName");
    }

    @Test
    void shouldBeEqualByClassName() {
        Dependency dep1 = Dependency.builder()
                .className("com.example.Test")
                .displayName("Test 1")
                .groupId("com.example")
                .artifactId("test1")
                .build();

        Dependency dep2 = Dependency.builder()
                .className("com.example.Test")
                .displayName("Test 2")
                .groupId("com.example")
                .artifactId("test2")
                .build();

        assertThat(dep1).isEqualTo(dep2);
        assertThat(dep1.hashCode()).isEqualTo(dep2.hashCode());
    }

    @Test
    void shouldNotBeEqualWithDifferentClassName() {
        Dependency dep1 = Dependency.builder()
                .className("com.example.Test1")
                .displayName("Test")
                .groupId("com.example")
                .artifactId("test")
                .build();

        Dependency dep2 = Dependency.builder()
                .className("com.example.Test2")
                .displayName("Test")
                .groupId("com.example")
                .artifactId("test")
                .build();

        assertThat(dep1).isNotEqualTo(dep2);
    }
}
