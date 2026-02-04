package io.github.jobs.spring.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for OpenAPI/Swagger UI integration with J-Obs.
 * <p>
 * This configuration is automatically activated when:
 * <ul>
 *   <li>Springdoc OpenAPI is on the classpath</li>
 *   <li>The application is a servlet-based web application</li>
 *   <li>The property {@code j-obs.openapi.enabled} is {@code true} (default)</li>
 * </ul>
 * <p>
 * If the user already has OpenAPI/Swagger configured, this configuration will NOT
 * override their settings due to {@link ConditionalOnMissingBean} conditions.
 * <p>
 * Features:
 * <ul>
 *   <li>Auto-generates OpenAPI documentation for all J-Obs endpoints</li>
 *   <li>Provides Swagger UI at {@code /j-obs/swagger-ui.html}</li>
 *   <li>Groups J-Obs APIs separately from application APIs</li>
 *   <li>Configures security schemes if J-Obs security is enabled</li>
 * </ul>
 *
 * @see JObsProperties.OpenApi
 */
@AutoConfiguration(after = JObsAutoConfiguration.class)
@ConditionalOnClass(name = "org.springdoc.core.models.GroupedOpenApi")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsOpenApiAutoConfiguration {

    private final JObsProperties properties;

    public JObsOpenApiAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates the J-Obs OpenAPI specification.
     * <p>
     * This bean is only created if:
     * <ul>
     *   <li>No existing OpenAPI bean is present</li>
     *   <li>OpenAPI is enabled via {@code j-obs.openapi.enabled=true}</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    @ConditionalOnProperty(name = "j-obs.openapi.enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI jObsOpenApi() {
        OpenAPI openAPI = new OpenAPI()
                .info(buildInfo())
                .servers(List.of(new Server().url("/").description("Default Server")))
                .tags(buildTags());

        // Add security scheme if J-Obs security is enabled
        if (properties.getSecurity().isEnabled()) {
            openAPI.components(new Components()
                    .addSecuritySchemes("basicAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("basic")
                            .description("Basic authentication for J-Obs dashboard")));
            openAPI.addSecurityItem(new SecurityRequirement().addList("basicAuth"));
        }

        return openAPI;
    }

    /**
     * Creates a grouped OpenAPI for J-Obs endpoints only.
     * <p>
     * This allows users to have separate Swagger UI groups for their
     * application APIs and J-Obs APIs.
     */
    @Bean
    @ConditionalOnMissingBean(name = "jObsGroupedOpenApi")
    @ConditionalOnProperty(name = "j-obs.openapi.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi jObsGroupedOpenApi() {
        String basePath = properties.getPath();
        return GroupedOpenApi.builder()
                .group("j-obs")
                .displayName("J-Obs Observability API")
                .pathsToMatch(basePath + "/api/**")
                .build();
    }

    /**
     * Creates a grouped OpenAPI for application endpoints (excluding J-Obs).
     * <p>
     * This bean is only created if no existing application group is defined.
     */
    @Bean
    @ConditionalOnMissingBean(name = "applicationGroupedOpenApi")
    @ConditionalOnProperty(name = "j-obs.openapi.application-group.enabled", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi applicationGroupedOpenApi() {
        String basePath = properties.getPath();
        return GroupedOpenApi.builder()
                .group("application")
                .displayName("Application API")
                .pathsToMatch("/**")
                .pathsToExclude(basePath + "/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
                .build();
    }

    private Info buildInfo() {
        return new Info()
                .title("J-Obs Observability API")
                .description("""
                        J-Obs provides comprehensive observability for Spring Boot applications.

                        ## Features

                        - **Traces**: Distributed tracing with full request journey visualization
                        - **Logs**: Real-time log streaming with filtering and search
                        - **Metrics**: JVM and application metrics with history
                        - **Health**: Component health checks and dependency status
                        - **Alerts**: Configurable alerts with multiple notification providers
                        - **SQL Analyzer**: N+1 query detection and slow query analysis
                        - **Anomaly Detection**: Automatic detection of performance anomalies
                        - **Service Map**: Visual dependency graph between services
                        - **SLO/SLI**: Service Level Objective tracking
                        - **Profiling**: CPU and memory profiling on demand

                        ## Authentication

                        If security is enabled, use Basic Authentication with the configured credentials.
                        """)
                .version(getVersion())
                .contact(new Contact()
                        .name("J-Obs")
                        .url("https://github.com/JohnPitter/j-obs")
                        .email("joaopitter@users.noreply.github.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag().name("Requirements").description("Dependency verification and system requirements"),
                new Tag().name("Traces").description("Distributed tracing - view request journeys and span details"),
                new Tag().name("Logs").description("Real-time log streaming and log history"),
                new Tag().name("Metrics").description("JVM metrics, custom metrics, and metric history"),
                new Tag().name("Health").description("Component health checks and dependency status"),
                new Tag().name("Alerts").description("Alert rules configuration and management"),
                new Tag().name("Alert Providers").description("Notification provider configuration (Telegram, Slack, etc.)"),
                new Tag().name("Alert Events").description("Alert event history and acknowledgment"),
                new Tag().name("SQL Analyzer").description("SQL query analysis - N+1 detection, slow queries"),
                new Tag().name("Anomalies").description("Automatic anomaly detection results"),
                new Tag().name("Service Map").description("Service dependency visualization"),
                new Tag().name("SLO").description("Service Level Objective tracking and error budgets"),
                new Tag().name("Profiling").description("CPU and memory profiling"),
                new Tag().name("Tools").description("Utility tools and capabilities")
        );
    }

    private String getVersion() {
        // Try to get version from package, fallback to 1.0.0
        Package pkg = JObsOpenApiAutoConfiguration.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;
        return version != null ? version : "1.0.0";
    }
}
