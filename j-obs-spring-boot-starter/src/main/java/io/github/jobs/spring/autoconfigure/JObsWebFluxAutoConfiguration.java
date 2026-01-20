package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.infrastructure.ClasspathDependencyChecker;
import io.github.jobs.spring.web.JObsApiController;
import io.github.jobs.spring.web.JObsController;
import io.github.jobs.spring.web.RateLimiter;
import io.github.jobs.spring.webflux.ReactiveRateLimitFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Auto-configuration for J-Obs observability dashboard in WebFlux applications.
 * <p>
 * This configuration is activated when the application is a reactive web application
 * (using Spring WebFlux instead of Spring MVC).
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsWebFluxAutoConfiguration implements WebFluxConfigurer {

    private final JObsProperties properties;

    public JObsWebFluxAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public JObsPropertiesValidator jObsPropertiesValidator() {
        return new JObsPropertiesValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DependencyChecker dependencyChecker() {
        return new ClasspathDependencyChecker(properties.getCheckInterval());
    }

    @Bean
    @ConditionalOnMissingBean
    public JObsController jObsController(DependencyChecker dependencyChecker) {
        return new JObsController(dependencyChecker, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JObsApiController jObsApiController(DependencyChecker dependencyChecker) {
        return new JObsApiController(dependencyChecker);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "j-obs.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
    public RateLimiter jObsRateLimiter() {
        JObsProperties.RateLimiting config = properties.getRateLimiting();
        return new RateLimiter(config.getMaxRequests(), config.getWindow());
    }

    @Bean
    @ConditionalOnProperty(name = "j-obs.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
    public ReactiveRateLimitFilter reactiveRateLimitFilter(RateLimiter rateLimiter) {
        return new ReactiveRateLimitFilter(rateLimiter, properties.getPath());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pathPattern = properties.getPath() + "/static/**";
        registry.addResourceHandler(pathPattern)
                .addResourceLocations("classpath:/static/j-obs/");
    }

    /**
     * Router function for serving static resources in WebFlux.
     * This provides an alternative to the resource handler for more control.
     */
    @Bean
    public RouterFunction<ServerResponse> jObsStaticResourceRouter() {
        String basePath = properties.getPath();
        return RouterFunctions.resources(
                basePath + "/static/**",
                new ClassPathResource("static/j-obs/")
        );
    }
}
