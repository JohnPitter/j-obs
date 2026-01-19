package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.spring.web.JObsApiController;
import io.github.jobs.spring.web.JObsController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JObsAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JObsAutoConfiguration.class));

    @Test
    void shouldAutoConfigureWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DependencyChecker.class);
            assertThat(context).hasSingleBean(JObsController.class);
            assertThat(context).hasSingleBean(JObsApiController.class);
        });
    }

    @Test
    void shouldNotAutoConfigureWhenDisabled() {
        contextRunner
                .withPropertyValues("j-obs.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DependencyChecker.class);
                    assertThat(context).doesNotHaveBean(JObsController.class);
                    assertThat(context).doesNotHaveBean(JObsApiController.class);
                });
    }

    @Test
    void shouldUseCustomPath() {
        contextRunner
                .withPropertyValues("j-obs.path=/custom-path")
                .run(context -> {
                    assertThat(context).hasSingleBean(JObsProperties.class);
                    JObsProperties props = context.getBean(JObsProperties.class);
                    assertThat(props.getPath()).isEqualTo("/custom-path");
                });
    }

    @Test
    void shouldUseDefaultPath() {
        contextRunner.run(context -> {
            JObsProperties props = context.getBean(JObsProperties.class);
            assertThat(props.getPath()).isEqualTo("/j-obs");
        });
    }

    @Test
    void shouldAllowCustomDependencyChecker() {
        contextRunner
                .withBean(DependencyChecker.class, () -> new CustomDependencyChecker())
                .run(context -> {
                    assertThat(context).hasSingleBean(DependencyChecker.class);
                    assertThat(context.getBean(DependencyChecker.class))
                            .isInstanceOf(CustomDependencyChecker.class);
                });
    }

    static class CustomDependencyChecker implements DependencyChecker {
        @Override
        public io.github.jobs.domain.DependencyCheckResult check() {
            return io.github.jobs.domain.DependencyCheckResult.of(java.util.List.of());
        }

        @Override
        public io.github.jobs.domain.DependencyCheckResult checkFresh() {
            return check();
        }

        @Override
        public void invalidateCache() {
            // no-op
        }
    }
}
