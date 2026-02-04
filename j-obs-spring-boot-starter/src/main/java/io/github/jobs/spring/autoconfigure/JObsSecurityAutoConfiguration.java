package io.github.jobs.spring.autoconfigure;

import io.github.jobs.spring.security.SecurityHeadersFilter;
import io.github.jobs.spring.web.JObsAuthenticationFilter;
import io.github.jobs.spring.webflux.ReactiveAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for J-Obs dashboard security.
 * Configures authentication filters and login controller when security is enabled.
 */
@AutoConfiguration(after = JObsAutoConfiguration.class)
@ConditionalOnProperty(name = "j-obs.security.enabled", havingValue = "true")
@EnableConfigurationProperties(JObsProperties.class)
public class JObsSecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsSecurityAutoConfiguration.class);

    /**
     * Configuration for Servlet-based applications.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "j-obs.security.enabled", havingValue = "true")
    static class ServletSecurityConfiguration implements WebMvcConfigurer {

        private final JObsProperties properties;

        ServletSecurityConfiguration(JObsProperties properties) {
            this.properties = properties;
            log.info("J-Obs security enabled with type: {}", properties.getSecurity().getType());

            if (!properties.getSecurity().isConfigured()) {
                log.warn("J-Obs security is enabled but not properly configured. " +
                         "Please configure users and/or API keys.");
            }
        }

        @Bean
        @ConditionalOnMissingBean
        public JObsAuthenticationFilter jObsAuthenticationFilter() {
            return new JObsAuthenticationFilter(
                    properties.getSecurity(),
                    properties.getPath()
            );
        }

        @Bean
        @ConditionalOnMissingBean(name = "jObsSecurityHeadersFilter")
        public FilterRegistrationBean<SecurityHeadersFilter> jObsSecurityHeadersFilter() {
            FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new SecurityHeadersFilter());
            registration.addUrlPatterns(properties.getPath() + "/*");
            registration.setName("jObsSecurityHeadersFilter");
            registration.setOrder(1); // Run early in the filter chain
            log.debug("Registered security headers filter for path: {}", properties.getPath());
            return registration;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(jObsAuthenticationFilter())
                    .addPathPatterns(properties.getPath() + "/**")
                    .order(0); // Run before rate limiting
        }
    }

    /**
     * Configuration for WebFlux-based applications.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(name = "j-obs.security.enabled", havingValue = "true")
    static class ReactiveSecurityConfiguration {

        private final JObsProperties properties;

        ReactiveSecurityConfiguration(JObsProperties properties) {
            this.properties = properties;
            log.info("J-Obs reactive security enabled with type: {}", properties.getSecurity().getType());

            if (!properties.getSecurity().isConfigured()) {
                log.warn("J-Obs security is enabled but not properly configured. " +
                         "Please configure users and/or API keys.");
            }
        }

        @Bean
        @ConditionalOnMissingBean
        public ReactiveAuthenticationFilter jObsReactiveAuthenticationFilter() {
            return new ReactiveAuthenticationFilter(
                    properties.getSecurity(),
                    properties.getPath()
            );
        }
    }
}
