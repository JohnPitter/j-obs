package io.github.jobs.domain;

import java.util.List;

/**
 * Registry of all dependencies that J-Obs checks for.
 * Centralizes the dependency definitions to ensure consistency.
 */
public final class KnownDependencies {

    private KnownDependencies() {
        // Utility class
    }

    public static final Dependency OPENTELEMETRY_API = Dependency.builder()
            .className("io.opentelemetry.api.OpenTelemetry")
            .displayName("OpenTelemetry API")
            .groupId("io.opentelemetry")
            .artifactId("opentelemetry-api")
            .required(true)
            .description("Core OpenTelemetry API for tracing")
            .documentationUrl("https://opentelemetry.io/docs/java/")
            .build();

    public static final Dependency OPENTELEMETRY_SDK = Dependency.builder()
            .className("io.opentelemetry.sdk.OpenTelemetrySdk")
            .displayName("OpenTelemetry SDK")
            .groupId("io.opentelemetry")
            .artifactId("opentelemetry-sdk")
            .required(true)
            .description("OpenTelemetry SDK for instrumentation")
            .documentationUrl("https://opentelemetry.io/docs/java/")
            .build();

    public static final Dependency MICROMETER_CORE = Dependency.builder()
            .className("io.micrometer.core.instrument.MeterRegistry")
            .displayName("Micrometer Core")
            .groupId("io.micrometer")
            .artifactId("micrometer-core")
            .required(true)
            .description("Application metrics facade")
            .documentationUrl("https://micrometer.io/docs")
            .build();

    /**
     * Micrometer Prometheus registry.
     * <p>
     * Note: In Micrometer 1.13+, the package changed from {@code io.micrometer.prometheus}
     * to {@code io.micrometer.prometheusmetrics}. We check for both to support all versions.
     */
    public static final Dependency MICROMETER_PROMETHEUS = Dependency.builder()
            .className("io.micrometer.prometheusmetrics.PrometheusMeterRegistry")
            .alternativeClassNames("io.micrometer.prometheus.PrometheusMeterRegistry")
            .displayName("Micrometer Prometheus")
            .groupId("io.micrometer")
            .artifactId("micrometer-registry-prometheus")
            .required(true)
            .description("Prometheus metrics export")
            .documentationUrl("https://micrometer.io/docs/registry/prometheus")
            .build();

    public static final Dependency SPRING_ACTUATOR = Dependency.builder()
            .className("org.springframework.boot.actuate.endpoint.annotation.Endpoint")
            .displayName("Spring Boot Actuator")
            .groupId("org.springframework.boot")
            .artifactId("spring-boot-starter-actuator")
            .required(true)
            .description("Production-ready features for Spring Boot")
            .documentationUrl("https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html")
            .build();

    public static final Dependency LOGBACK = Dependency.builder()
            .className("ch.qos.logback.classic.Logger")
            .displayName("Logback")
            .groupId("ch.qos.logback")
            .artifactId("logback-classic")
            .required(false)
            .description("Logging framework (optional, falls back to java.util.logging)")
            .documentationUrl("https://logback.qos.ch/documentation.html")
            .build();

    /**
     * Returns all dependencies that J-Obs checks for.
     * Order matters for display purposes.
     */
    public static List<Dependency> all() {
        return List.of(
                OPENTELEMETRY_API,
                OPENTELEMETRY_SDK,
                MICROMETER_CORE,
                MICROMETER_PROMETHEUS,
                SPRING_ACTUATOR,
                LOGBACK
        );
    }

    /**
     * Returns only required dependencies.
     */
    public static List<Dependency> required() {
        return all().stream()
                .filter(Dependency::isRequired)
                .toList();
    }

    /**
     * Returns only optional dependencies.
     */
    public static List<Dependency> optional() {
        return all().stream()
                .filter(d -> !d.isRequired())
                .toList();
    }
}
