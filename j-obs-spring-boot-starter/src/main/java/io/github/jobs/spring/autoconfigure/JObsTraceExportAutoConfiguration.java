package io.github.jobs.spring.autoconfigure;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration for external trace exporters (OTLP, Zipkin, Jaeger).
 * <p>
 * Configures exporters to send traces to external observability platforms
 * like Grafana Tempo, Jaeger, Zipkin, or any OTLP-compatible collector.
 */
@AutoConfiguration(after = JObsTraceAutoConfiguration.class)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsTraceExportAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsTraceExportAutoConfiguration.class);

    /**
     * OTLP exporter configuration for Grafana Tempo and generic OTLP collectors.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
    @ConditionalOnProperty(name = "j-obs.traces.export.otlp.enabled", havingValue = "true")
    static class OtlpExporterConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "otlpSpanExporter")
        public SpanExporter otlpSpanExporter(JObsProperties properties) {
            JObsProperties.Traces.Export.Otlp config = properties.getTraces().getExport().getOtlp();

            log.info("Configuring OTLP trace exporter to: {}", config.getEndpoint());

            var builder = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(config.getEndpoint())
                    .setTimeout(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS);

            // Add custom headers if configured
            config.getHeaders().forEach(builder::addHeader);

            return builder.build();
        }
    }

    /**
     * Zipkin exporter configuration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.opentelemetry.exporter.zipkin.ZipkinSpanExporter")
    @ConditionalOnProperty(name = "j-obs.traces.export.zipkin.enabled", havingValue = "true")
    static class ZipkinExporterConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zipkinSpanExporter")
        public SpanExporter zipkinSpanExporter(JObsProperties properties) {
            JObsProperties.Traces.Export.Zipkin config = properties.getTraces().getExport().getZipkin();

            log.info("Configuring Zipkin trace exporter to: {}", config.getEndpoint());

            return ZipkinSpanExporter.builder()
                    .setEndpoint(config.getEndpoint())
                    .setReadTimeout(config.getTimeout())
                    .build();
        }
    }

    /**
     * Jaeger exporter configuration (uses OTLP protocol).
     * <p>
     * Note: Modern Jaeger versions support OTLP natively, so we use OtlpGrpcSpanExporter
     * with the Jaeger OTLP endpoint. For legacy Jaeger (Thrift protocol), consider
     * using the deprecated jaeger-thrift exporter.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
    @ConditionalOnProperty(name = "j-obs.traces.export.jaeger.enabled", havingValue = "true")
    static class JaegerExporterConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "jaegerSpanExporter")
        public SpanExporter jaegerSpanExporter(JObsProperties properties) {
            JObsProperties.Traces.Export.Jaeger config = properties.getTraces().getExport().getJaeger();

            log.info("Configuring Jaeger trace exporter to: {}", config.getEndpoint());

            // Jaeger now supports OTLP natively (port 4317 by default)
            // For legacy Jaeger, endpoint would be http://localhost:14250
            return OtlpGrpcSpanExporter.builder()
                    .setEndpoint(config.getEndpoint())
                    .setTimeout(config.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .build();
        }
    }
}
