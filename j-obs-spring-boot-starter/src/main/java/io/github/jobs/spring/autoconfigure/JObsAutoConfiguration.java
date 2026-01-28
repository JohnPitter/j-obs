package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.infrastructure.ClasspathDependencyChecker;
import io.github.jobs.spring.web.CapabilitiesController;
import io.github.jobs.spring.web.JObsApiController;
import io.github.jobs.spring.web.JObsController;
import io.github.jobs.spring.web.RateLimiter;
import io.github.jobs.spring.web.RateLimitInterceptor;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
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
 * Main auto-configuration for the J-Obs observability dashboard.
 * <p>
 * This configuration is automatically activated when:
 * <ul>
 *   <li>The application is a servlet-based web application</li>
 *   <li>The property {@code j-obs.enabled} is {@code true} (default)</li>
 * </ul>
 * <p>
 * Features configured:
 * <ul>
 *   <li>{@link JObsController} - Main dashboard UI controller</li>
 *   <li>{@link JObsApiController} - REST API endpoints</li>
 *   <li>{@link DependencyChecker} - Runtime dependency verification</li>
 *   <li>{@link RateLimiter} - API rate limiting protection</li>
 *   <li>Static resource handling for dashboard assets</li>
 * </ul>
 * <p>
 * Configuration properties are bound via {@link JObsProperties} with prefix {@code j-obs}.
 *
 * @see JObsProperties
 * @see JObsLogAutoConfiguration
 * @see JObsTraceAutoConfiguration
 * @see JObsMetricAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsAutoConfiguration implements WebMvcConfigurer {

    private final JObsProperties properties;
    private final ObjectProvider<RateLimiter> rateLimiterProvider;
    private volatile RateLimiter rateLimiterRef;

    public JObsAutoConfiguration(JObsProperties properties, ObjectProvider<RateLimiter> rateLimiterProvider) {
        this.properties = properties;
        this.rateLimiterProvider = rateLimiterProvider;
    }

    @PreDestroy
    public void destroy() {
        if (rateLimiterRef != null) {
            rateLimiterRef.shutdown();
        }
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
    public CapabilitiesController capabilitiesController(ListableBeanFactory beanFactory) {
        return new CapabilitiesController(beanFactory);
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
            RateLimiter rateLimiter = rateLimiterProvider.getIfAvailable();
            if (rateLimiter != null) {
                this.rateLimiterRef = rateLimiter;
                registry.addInterceptor(new RateLimitInterceptor(rateLimiter, properties.getPath()))
                        .addPathPatterns(properties.getPath() + "/api/**");
            }
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
