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

    // Circuit Breaker Dependencies (all optional)

    public static final Dependency RESILIENCE4J_CIRCUITBREAKER = Dependency.builder()
            .className("io.github.resilience4j.circuitbreaker.CircuitBreaker")
            .displayName("Resilience4j Circuit Breaker")
            .groupId("io.github.resilience4j")
            .artifactId("resilience4j-circuitbreaker")
            .required(false)
            .description("Lightweight fault tolerance library with circuit breaker pattern")
            .documentationUrl("https://resilience4j.readme.io/docs/circuitbreaker")
            .build();

    public static final Dependency RESILIENCE4J_SPRING_BOOT = Dependency.builder()
            .className("io.github.resilience4j.springboot3.autoconfigure.CircuitBreakerAutoConfiguration")
            .alternativeClassNames("io.github.resilience4j.springboot2.autoconfigure.CircuitBreakerAutoConfiguration")
            .displayName("Resilience4j Spring Boot")
            .groupId("io.github.resilience4j")
            .artifactId("resilience4j-spring-boot3")
            .required(false)
            .description("Resilience4j Spring Boot integration with auto-configuration")
            .documentationUrl("https://resilience4j.readme.io/docs/getting-started-3")
            .build();

    public static final Dependency SPRING_CLOUD_CIRCUITBREAKER = Dependency.builder()
            .className("org.springframework.cloud.client.circuitbreaker.CircuitBreaker")
            .displayName("Spring Cloud Circuit Breaker")
            .groupId("org.springframework.cloud")
            .artifactId("spring-cloud-starter-circuitbreaker-resilience4j")
            .required(false)
            .description("Spring Cloud abstraction for circuit breaker implementations")
            .documentationUrl("https://spring.io/projects/spring-cloud-circuitbreaker")
            .build();

    /**
     * Hystrix is deprecated but still used in legacy applications.
     */
    public static final Dependency HYSTRIX = Dependency.builder()
            .className("com.netflix.hystrix.Hystrix")
            .displayName("Netflix Hystrix (Deprecated)")
            .groupId("com.netflix.hystrix")
            .artifactId("hystrix-core")
            .required(false)
            .description("Legacy circuit breaker (deprecated, consider migrating to Resilience4j)")
            .documentationUrl("https://github.com/Netflix/Hystrix")
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
                LOGBACK,
                RESILIENCE4J_CIRCUITBREAKER,
                RESILIENCE4J_SPRING_BOOT,
                SPRING_CLOUD_CIRCUITBREAKER,
                HYSTRIX
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

    /**
     * Returns circuit breaker related dependencies.
     */
    public static List<Dependency> circuitBreakers() {
        return List.of(
                RESILIENCE4J_CIRCUITBREAKER,
                RESILIENCE4J_SPRING_BOOT,
                SPRING_CLOUD_CIRCUITBREAKER,
                HYSTRIX
        );
    }
}
