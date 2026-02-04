package io.github.jobs.spring.autoconfigure;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JObsOpenApiAutoConfiguration}.
 */
class JObsOpenApiAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JObsAutoConfiguration.class,
                    JObsOpenApiAutoConfiguration.class
            ));

    @Nested
    @DisplayName("When Springdoc is available and no user configuration exists")
    class DefaultConfiguration {

        @Test
        @DisplayName("should create OpenAPI bean")
        void shouldCreateOpenApiBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(OpenAPI.class);
                OpenAPI openAPI = context.getBean(OpenAPI.class);
                assertThat(openAPI.getInfo()).isNotNull();
                assertThat(openAPI.getInfo().getTitle()).isEqualTo("J-Obs Observability API");
            });
        }

        @Test
        @DisplayName("should create J-Obs grouped API")
        void shouldCreateJObsGroupedApi() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("jObsGroupedOpenApi");
                GroupedOpenApi groupedApi = context.getBean("jObsGroupedOpenApi", GroupedOpenApi.class);
                assertThat(groupedApi.getGroup()).isEqualTo("j-obs");
            });
        }

        @Test
        @DisplayName("should create application grouped API")
        void shouldCreateApplicationGroupedApi() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("applicationGroupedOpenApi");
                GroupedOpenApi groupedApi = context.getBean("applicationGroupedOpenApi", GroupedOpenApi.class);
                assertThat(groupedApi.getGroup()).isEqualTo("application");
            });
        }
    }

    @Nested
    @DisplayName("When user already has OpenAPI configured")
    class UserConfiguredOpenApi {

        @Test
        @DisplayName("should not override user's OpenAPI bean")
        void shouldNotOverrideUserOpenApi() {
            contextRunner
                    .withUserConfiguration(CustomOpenApiConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(OpenAPI.class);
                        OpenAPI openAPI = context.getBean(OpenAPI.class);
                        // Should be the user's custom OpenAPI, not J-Obs's
                        assertThat(openAPI.getInfo().getTitle()).isEqualTo("My Custom API");
                    });
        }

        @Test
        @DisplayName("should not override user's grouped API beans")
        void shouldNotOverrideUserGroupedApis() {
            contextRunner
                    .withUserConfiguration(CustomGroupedApiConfiguration.class)
                    .run(context -> {
                        // User's beans should be present
                        assertThat(context).hasBean("jObsGroupedOpenApi");
                        assertThat(context).hasBean("applicationGroupedOpenApi");

                        // Should be the user's custom configurations
                        GroupedOpenApi jObsGroup = context.getBean("jObsGroupedOpenApi", GroupedOpenApi.class);
                        assertThat(jObsGroup.getGroup()).isEqualTo("custom-j-obs");

                        GroupedOpenApi appGroup = context.getBean("applicationGroupedOpenApi", GroupedOpenApi.class);
                        assertThat(appGroup.getGroup()).isEqualTo("custom-app");
                    });
        }
    }

    @Nested
    @DisplayName("When OpenAPI is disabled")
    class DisabledConfiguration {

        @Test
        @DisplayName("should not create OpenAPI beans when j-obs.openapi.enabled=false")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("j-obs.openapi.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenAPI.class);
                        assertThat(context).doesNotHaveBean("jObsGroupedOpenApi");
                    });
        }

        @Test
        @DisplayName("should not create application group when disabled")
        void shouldNotCreateApplicationGroupWhenDisabled() {
            contextRunner
                    .withPropertyValues("j-obs.openapi.application-group.enabled=false")
                    .run(context -> {
                        // OpenAPI and j-obs group should still be created
                        assertThat(context).hasSingleBean(OpenAPI.class);
                        assertThat(context).hasBean("jObsGroupedOpenApi");
                        // But application group should not
                        assertThat(context).doesNotHaveBean("applicationGroupedOpenApi");
                    });
        }

        @Test
        @DisplayName("should not create any beans when j-obs is disabled")
        void shouldNotCreateBeansWhenJObsDisabled() {
            contextRunner
                    .withPropertyValues("j-obs.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(OpenAPI.class);
                        assertThat(context).doesNotHaveBean("jObsGroupedOpenApi");
                        assertThat(context).doesNotHaveBean("applicationGroupedOpenApi");
                    });
        }
    }

    @Nested
    @DisplayName("Security integration")
    class SecurityIntegration {

        @Test
        @DisplayName("should add security scheme when security is enabled")
        void shouldAddSecuritySchemeWhenSecurityEnabled() {
            contextRunner
                    .withPropertyValues(
                            "j-obs.security.enabled=true",
                            "j-obs.security.users[0].username=admin",
                            "j-obs.security.users[0].password=admin123"
                    )
                    .run(context -> {
                        OpenAPI openAPI = context.getBean(OpenAPI.class);
                        assertThat(openAPI.getComponents()).isNotNull();
                        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("basicAuth");
                        assertThat(openAPI.getSecurity()).isNotEmpty();
                    });
        }

        @Test
        @DisplayName("should not add security scheme when security is disabled")
        void shouldNotAddSecuritySchemeWhenSecurityDisabled() {
            contextRunner
                    .withPropertyValues("j-obs.security.enabled=false")
                    .run(context -> {
                        OpenAPI openAPI = context.getBean(OpenAPI.class);
                        // Components may be null or security schemes may be empty
                        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
                            assertThat(openAPI.getComponents().getSecuritySchemes()).isEmpty();
                        }
                    });
        }
    }

    @Nested
    @DisplayName("Custom path configuration")
    class CustomPathConfiguration {

        @Test
        @DisplayName("should use custom j-obs path in grouped API")
        void shouldUseCustomPath() {
            contextRunner
                    .withPropertyValues("j-obs.path=/custom-observability")
                    .run(context -> {
                        GroupedOpenApi groupedApi = context.getBean("jObsGroupedOpenApi", GroupedOpenApi.class);
                        assertThat(groupedApi.getPathsToMatch()).contains("/custom-observability/api/**");
                    });
        }
    }

    // Test configurations

    @Configuration(proxyBeanMethods = false)
    static class CustomOpenApiConfiguration {

        @Bean
        public OpenAPI customOpenApi() {
            return new OpenAPI()
                    .info(new io.swagger.v3.oas.models.info.Info()
                            .title("My Custom API")
                            .version("1.0.0"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomGroupedApiConfiguration {

        @Bean
        public GroupedOpenApi jObsGroupedOpenApi() {
            return GroupedOpenApi.builder()
                    .group("custom-j-obs")
                    .pathsToMatch("/my-custom-path/**")
                    .build();
        }

        @Bean
        public GroupedOpenApi applicationGroupedOpenApi() {
            return GroupedOpenApi.builder()
                    .group("custom-app")
                    .pathsToMatch("/api/**")
                    .build();
        }
    }
}
