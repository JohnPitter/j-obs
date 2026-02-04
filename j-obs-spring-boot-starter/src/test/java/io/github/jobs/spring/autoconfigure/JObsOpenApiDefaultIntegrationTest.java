package io.github.jobs.spring.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for default OpenAPI/Swagger configuration.
 * <p>
 * Tests verify that Swagger works out of the box when J-Obs is added.
 */
@SpringBootTest(
        classes = JObsOpenApiDefaultIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {"j-obs.enabled=true"})
class JObsOpenApiDefaultIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("Swagger UI should be accessible")
    void swaggerUiShouldBeAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/swagger-ui/index.html",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("swagger-ui");
    }

    @Test
    @DisplayName("OpenAPI docs should include J-Obs info")
    void openApiDocsShouldIncludeJObsInfo() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("J-Obs Observability API");
    }

    @Test
    @DisplayName("J-Obs grouped API should be created")
    void jObsGroupedApiShouldBeCreated() {
        assertThat(context.containsBean("jObsGroupedOpenApi")).isTrue();
        GroupedOpenApi groupedApi = context.getBean("jObsGroupedOpenApi", GroupedOpenApi.class);
        assertThat(groupedApi.getGroup()).isEqualTo("j-obs");
    }

    @Test
    @DisplayName("Application grouped API should be created")
    void applicationGroupedApiShouldBeCreated() {
        assertThat(context.containsBean("applicationGroupedOpenApi")).isTrue();
        GroupedOpenApi groupedApi = context.getBean("applicationGroupedOpenApi", GroupedOpenApi.class);
        assertThat(groupedApi.getGroup()).isEqualTo("application");
    }

    @Test
    @DisplayName("Swagger config should list both groups")
    void swaggerConfigShouldListBothGroups() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs/swagger-config",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("j-obs");
        assertThat(response.getBody()).contains("application");
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {

        @RestController
        static class TestController {

            @GetMapping("/api/test")
            public String test() {
                return "test";
            }
        }
    }
}
