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
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

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
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
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
     * <p>
     * This configuration ensures J-Obs span exporter is always registered,
     * even when other libraries (like Spring Boot Actuator or micrometer-tracing)
     * are present.
     * <p>
     * Key design decisions:
     * <ul>
     *   <li>Uses @Primary on key beans to ensure J-Obs takes precedence</li>
     *   <li>Auto-configuration order set to HIGHEST_PRECEDENCE to run first</li>
     *   <li>Always registers JObsSpanExporter to capture traces</li>
     * </ul>
     */
    @ConditionalOnClass(name = "io.opentelemetry.sdk.trace.SdkTracerProvider")
    static class OpenTelemetryConfiguration {

        /**
         * Creates the J-Obs span exporter.
         * This bean is always created to ensure trace capture works.
         */
        @Bean
        public JObsSpanExporter jObsSpanExporter(TraceRepository traceRepository) {
            log.info("Creating JObsSpanExporter for trace repository");
            return new JObsSpanExporter(traceRepository);
        }

        /**
         * Creates the SdkTracerProvider with J-Obs exporter.
         * Marked as @Primary to ensure this provider is used even if others exist.
         */
        @Bean
        @Primary
        public SdkTracerProvider sdkTracerProvider(
                JObsSpanExporter jObsSpanExporter,
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
            log.info("Added J-Obs span exporter to SdkTracerProvider");

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

        /**
         * Creates the OpenTelemetry instance.
         * Marked as @Primary to ensure this instance is used.
         */
        @Bean
        @Primary
        public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
            // Reset GlobalOpenTelemetry if it was previously initialized
            // This ensures J-Obs's TracerProvider is used
            try {
                GlobalOpenTelemetry.resetForTest();
                log.debug("Reset GlobalOpenTelemetry for J-Obs initialization");
            } catch (Exception e) {
                // resetForTest might not be available in all versions
                log.debug("Could not reset GlobalOpenTelemetry: {}", e.getMessage());
            }

            // Build and register our OpenTelemetry instance
            try {
                OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                        .setTracerProvider(sdkTracerProvider)
                        .buildAndRegisterGlobal();
                log.info("J-Obs initialized GlobalOpenTelemetry with JObsSpanExporter");
                return sdk;
            } catch (IllegalStateException e) {
                // GlobalOpenTelemetry was already set - get it and log warning
                log.warn("GlobalOpenTelemetry was already registered by another component. " +
                        "J-Obs will use its own OpenTelemetry instance for the Tracer bean.");
                // Return our own SDK without registering it globally
                // The @Primary annotation ensures our beans are injected
                return OpenTelemetrySdk.builder()
                        .setTracerProvider(sdkTracerProvider)
                        .build();
            }
        }

        /**
         * Creates the Tracer for J-Obs instrumentation.
         * Marked as @Primary to ensure this tracer is injected into HttpTracingFilter.
         */
        @Bean
        @Primary
        public Tracer tracer(OpenTelemetry openTelemetry) {
            Tracer tracer = openTelemetry.getTracer("j-obs", "1.0.0");
            log.info("Created J-Obs Tracer from OpenTelemetry instance");
            return tracer;
        }
    }
}
