package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.infrastructure.InMemoryTraceRepository;
import io.github.jobs.spring.trace.JObsSpanExporter;
import io.github.jobs.spring.web.TraceApiController;
import io.github.jobs.spring.web.TraceController;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for J-Obs distributed trace collection and visualization.
 * <p>
 * This configuration is activated when {@code j-obs.traces.enabled} is {@code true} (default).
 * <p>
 * Components configured:
 * <ul>
 *   <li>{@link TraceRepository} - In-memory trace storage with TTL cleanup</li>
 *   <li>{@link TraceController} - UI controller for trace viewer</li>
 *   <li>{@link TraceApiController} - REST API for trace queries</li>
 * </ul>
 * <p>
 * OpenTelemetry integration (when SDK is present):
 * <ul>
 *   <li>{@link JObsSpanExporter} - Exports spans to internal storage</li>
 *   <li>{@link SdkTracerProvider} - Configured with service name and exporters</li>
 *   <li>{@link OpenTelemetry} - Global OpenTelemetry instance</li>
 *   <li>{@link Tracer} - Ready-to-use tracer for custom instrumentation</li>
 * </ul>
 * <p>
 * Configuration properties under {@code j-obs.traces}:
 * <ul>
 *   <li>{@code enabled} - Enable/disable trace collection (default: true)</li>
 *   <li>{@code max-traces} - Maximum traces in memory (default: 10000)</li>
 *   <li>{@code retention} - How long to keep traces (default: 1h)</li>
 *   <li>{@code sample-rate} - Sampling rate 0.0-1.0 (default: 1.0)</li>
 *   <li>{@code export.*} - External exporter configuration (OTLP, Zipkin, Jaeger)</li>
 * </ul>
 *
 * @see TraceRepository
 * @see JObsTraceExportAutoConfiguration
 * @see JObsProperties.Traces
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsTraceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsTraceAutoConfiguration.class);

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
                ObjectProvider<List<SpanExporter>> additionalExporters,
                @Value("${spring.application.name:j-obs-app}") String serviceName) {

            Resource resource = Resource.getDefault()
                    .merge(Resource.create(Attributes.of(
                            AttributeKey.stringKey("service.name"), serviceName
                    )));

            SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                    .setResource(resource);

            // Always add the J-Obs internal exporter
            builder.addSpanProcessor(SimpleSpanProcessor.create(jObsSpanExporter));
            log.debug("Added J-Obs span exporter");

            // Add any additional configured exporters (OTLP, Zipkin, Jaeger)
            List<SpanExporter> exporters = additionalExporters.getIfAvailable();
            if (exporters != null) {
                for (SpanExporter exporter : exporters) {
                    // Skip the J-Obs exporter to avoid duplicate registration
                    if (!(exporter instanceof JObsSpanExporter)) {
                        builder.addSpanProcessor(SimpleSpanProcessor.create(exporter));
                        log.info("Added external span exporter: {}", exporter.getClass().getSimpleName());
                    }
                }
            }

            return builder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
            // Check if GlobalOpenTelemetry is already initialized (e.g., by Spring Boot Actuator)
            try {
                OpenTelemetry existing = GlobalOpenTelemetry.get();
                // If we get here without exception, it means GlobalOpenTelemetry was already set
                // Check if it's not the default noop instance
                if (existing != OpenTelemetry.noop()) {
                    log.info("Using existing GlobalOpenTelemetry instance (already initialized by another library)");
                    return existing;
                }
            } catch (IllegalStateException e) {
                // GlobalOpenTelemetry.get() throws if not initialized - this is expected
                log.debug("GlobalOpenTelemetry not yet initialized, J-Obs will initialize it");
            }

            // Build and register our OpenTelemetry instance
            try {
                OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                        .setTracerProvider(sdkTracerProvider)
                        .buildAndRegisterGlobal();
                log.info("J-Obs initialized GlobalOpenTelemetry");
                return sdk;
            } catch (IllegalStateException e) {
                // Race condition: another thread registered GlobalOpenTelemetry between our check and registration
                log.warn("GlobalOpenTelemetry was registered by another component, using existing instance");
                return GlobalOpenTelemetry.get();
            }
        }

        @Bean
        @ConditionalOnMissingBean
        public Tracer tracer(OpenTelemetry openTelemetry) {
            return openTelemetry.getTracer("j-obs", "1.0.0");
        }
    }
}
