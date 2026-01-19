package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.infrastructure.InMemoryTraceRepository;
import io.github.jobs.spring.trace.JObsSpanExporter;
import io.github.jobs.spring.web.TraceApiController;
import io.github.jobs.spring.web.TraceController;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs trace collection and visualization.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.traces.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsTraceAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public InMemoryTraceRepository traceRepository(JObsProperties properties) {
        return new InMemoryTraceRepository(
            properties.getTraces().getRetention(),
            properties.getTraces().getMaxTraces()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceController traceController(TraceRepository traceRepository, JObsProperties properties) {
        return new TraceController(traceRepository, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceApiController traceApiController(TraceRepository traceRepository) {
        return new TraceApiController(traceRepository);
    }

    /**
     * Configuration for OpenTelemetry integration.
     */
    @ConditionalOnClass(name = "io.opentelemetry.sdk.trace.SdkTracerProvider")
    static class OpenTelemetryConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "jObsSpanExporter")
        public SpanExporter jObsSpanExporter(TraceRepository traceRepository) {
            return new JObsSpanExporter(traceRepository);
        }

        @Bean
        @ConditionalOnMissingBean
        public SdkTracerProvider sdkTracerProvider(
                SpanExporter jObsSpanExporter,
                @Value("${spring.application.name:j-obs-app}") String serviceName) {
            Resource resource = Resource.getDefault()
                    .merge(Resource.create(Attributes.of(
                            AttributeKey.stringKey("service.name"), serviceName
                    )));

            return SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(jObsSpanExporter))
                    .setResource(resource)
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
            return OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .buildAndRegisterGlobal();
        }

        @Bean
        @ConditionalOnMissingBean
        public Tracer tracer(OpenTelemetry openTelemetry) {
            return openTelemetry.getTracer("j-obs", "1.0.0");
        }
    }
}
