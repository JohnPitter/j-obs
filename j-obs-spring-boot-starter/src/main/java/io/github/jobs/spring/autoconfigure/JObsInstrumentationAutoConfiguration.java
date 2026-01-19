package io.github.jobs.spring.autoconfigure;

import io.github.jobs.spring.aop.AutoInstrumentationAspect;
import io.github.jobs.spring.aop.MetricsAspect;
import io.github.jobs.spring.aop.TracingAspect;
import io.github.jobs.spring.filter.HttpTracingFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for J-Obs automatic instrumentation.
 *
 * <p>Enables automatic tracing and metrics for:</p>
 * <ul>
 *   <li>HTTP requests (via filter)</li>
 *   <li>Methods annotated with @Traced, @Measured, @Observable</li>
 *   <li>All methods in @Service, @Repository, @Component classes (if auto-instrumentation enabled)</li>
 * </ul>
 */
@AutoConfiguration(after = JObsTraceAutoConfiguration.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "j-obs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({Tracer.class, MeterRegistry.class})
public class JObsInstrumentationAutoConfiguration {

    /**
     * Aspect for @Traced annotation support.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Tracer.class)
    @ConditionalOnProperty(prefix = "j-obs.instrumentation", name = "traced-annotation", havingValue = "true", matchIfMissing = true)
    public TracingAspect tracingAspect(Tracer tracer) {
        return new TracingAspect(tracer);
    }

    /**
     * Aspect for @Measured annotation support.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "j-obs.instrumentation", name = "measured-annotation", havingValue = "true", matchIfMissing = true)
    public MetricsAspect metricsAspect(MeterRegistry meterRegistry) {
        return new MetricsAspect(meterRegistry);
    }

    /**
     * Aspect for automatic instrumentation of all Spring components.
     * This is disabled by default and must be explicitly enabled.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracer.class, MeterRegistry.class})
    @ConditionalOnProperty(prefix = "j-obs.instrumentation", name = "auto-instrument", havingValue = "true")
    public AutoInstrumentationAspect autoInstrumentationAspect(Tracer tracer, MeterRegistry meterRegistry) {
        return new AutoInstrumentationAspect(tracer, meterRegistry);
    }

    /**
     * Filter for automatic HTTP request tracing.
     */
    @Bean
    @ConditionalOnBean({Tracer.class, MeterRegistry.class})
    @ConditionalOnProperty(prefix = "j-obs.instrumentation", name = "http-tracing", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<HttpTracingFilter> httpTracingFilter(Tracer tracer, MeterRegistry meterRegistry) {
        FilterRegistrationBean<HttpTracingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpTracingFilter(tracer, meterRegistry));
        registration.addUrlPatterns("/*");
        registration.setName("httpTracingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
