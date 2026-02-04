package io.github.jobs.spring.autoconfigure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OpenAPI when user already has Swagger configured.
 * <p>
 * Tests verify that J-Obs does NOT override user's existing Swagger configuration.
 */
@SpringBootTest(
        classes = JObsOpenApiUserConfiguredIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {"j-obs.enabled=true"})
class JObsOpenApiUserConfiguredIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Should use user's OpenAPI configuration, not J-Obs default")
    void shouldUseUserOpenApiConfig() {
        OpenAPI openAPI = context.getBean(OpenAPI.class);
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("User's Custom API");
    }

    @Test
    @DisplayName("OpenAPI docs should reflect user's config, not J-Obs")
    void openApiDocsShouldReflectUserConfig() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("User's Custom API");
        // J-Obs title should NOT be present since user defined their own
        assertThat(response.getBody()).doesNotContain("J-Obs Observability API");
    }

    @Test
    @DisplayName("Should use user's grouped API beans")
    void shouldUseUserGroupedApis() {
        GroupedOpenApi groupedApi = context.getBean("jObsGroupedOpenApi", GroupedOpenApi.class);
        // Should be user's custom config, not J-Obs default
        assertThat(groupedApi.getGroup()).isEqualTo("my-custom-group");
    }

    @Test
    @DisplayName("Swagger UI should still be accessible")
    void swaggerUiShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/swagger-ui/index.html",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        /**
         * User's custom OpenAPI configuration.
         * J-Obs should NOT override this.
         */
        @Bean
        public OpenAPI customOpenApi() {
            return new OpenAPI()
                    .info(new Info()
                            .title("User's Custom API")
                            .version("2.0.0")
                            .description("This is the user's custom OpenAPI config"));
        }

        /**
         * User's custom grouped API.
         * J-Obs should NOT override this.
         */
        @Bean
        public GroupedOpenApi jObsGroupedOpenApi() {
            return GroupedOpenApi.builder()
                    .group("my-custom-group")
                    .pathsToMatch("/custom/**")
                    .build();
        }

        /**
         * User's custom application grouped API.
         * J-Obs should NOT override this.
         */
        @Bean
        public GroupedOpenApi applicationGroupedOpenApi() {
            return GroupedOpenApi.builder()
                    .group("my-app")
                    .pathsToMatch("/api/**")
                    .build();
        }

        @RestController
        static class TestController {

            @GetMapping("/api/test")
            public String test() {
                return "test";
            }

            @GetMapping("/custom/endpoint")
            public String custom() {
                return "custom";
            }
        }
    }
}
