package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.infrastructure.ClasspathDependencyChecker;
import io.github.jobs.spring.web.JObsApiController;
import io.github.jobs.spring.web.JObsController;
import io.github.jobs.spring.web.RateLimiter;
import io.github.jobs.spring.web.RateLimitInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for J-Obs observability dashboard.
 * Automatically configures the dashboard when the starter is on the classpath.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsAutoConfiguration implements WebMvcConfigurer {

    private final JObsProperties properties;

    public JObsAutoConfiguration(JObsProperties properties) {
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (properties.getRateLimiting().isEnabled()) {
            RateLimiter rateLimiter = new RateLimiter(
                    properties.getRateLimiting().getMaxRequests(),
                    properties.getRateLimiting().getWindow()
            );
            registry.addInterceptor(new RateLimitInterceptor(rateLimiter, properties.getPath()))
                    .addPathPatterns(properties.getPath() + "/api/**");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets for J-Obs dashboard
        String pathPattern = properties.getPath() + "/static/**";
        registry.addResourceHandler(pathPattern)
                .addResourceLocations("classpath:/static/j-obs/");
    }
}
