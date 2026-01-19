package io.github.jobs.domain.trace;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpanTest {

    @Test
    void shouldCreateSpanWithBuilder() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(100);

        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .parentSpanId("span-parent")
                .name("GET /api/users")
                .serviceName("user-service")
                .kind(SpanKind.SERVER)
                .status(SpanStatus.OK)
                .startTime(start)
                .endTime(end)
                .attribute("http.method", "GET")
                .attribute("http.url", "/api/users")
                .build();

        assertThat(span.traceId()).isEqualTo("trace-123");
        assertThat(span.spanId()).isEqualTo("span-456");
        assertThat(span.parentSpanId()).isEqualTo("span-parent");
        assertThat(span.name()).isEqualTo("GET /api/users");
        assertThat(span.serviceName()).isEqualTo("user-service");
        assertThat(span.kind()).isEqualTo(SpanKind.SERVER);
        assertThat(span.status()).isEqualTo(SpanStatus.OK);
        assertThat(span.startTime()).isEqualTo(start);
        assertThat(span.endTime()).isEqualTo(end);
        assertThat(span.attributes()).containsEntry("http.method", "GET");
        assertThat(span.attributes()).containsEntry("http.url", "/api/users");
    }

    @Test
    void shouldCalculateDuration() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(250);

        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .name("test")
                .startTime(start)
                .endTime(end)
                .build();

        assertThat(span.durationMs()).isEqualTo(250);
        assertThat(span.duration()).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    void shouldDetectErrorStatus() {
        Span errorSpan = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .name("test")
                .status(SpanStatus.ERROR)
                .statusMessage("Connection timeout")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .build();

        assertThat(errorSpan.hasError()).isTrue();
        assertThat(errorSpan.statusMessage()).isEqualTo("Connection timeout");
    }

    @Test
    void shouldDetectRootSpan() {
        Span rootSpan = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .parentSpanId(null)
                .name("root")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .build();

        Span childSpan = Span.builder()
                .traceId("trace-123")
                .spanId("span-789")
                .parentSpanId("span-456")
                .name("child")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(50))
                .build();

        assertThat(rootSpan.isRoot()).isTrue();
        assertThat(childSpan.isRoot()).isFalse();
    }

    @Test
    void shouldExtractHttpAttributes() {
        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .name("GET /api/users")
                .kind(SpanKind.SERVER)
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .attribute("http.method", "GET")
                .attribute("http.url", "http://localhost:8080/api/users")
                .attribute("http.status_code", "200")
                .build();

        assertThat(span.httpMethod()).isEqualTo("GET");
        assertThat(span.httpUrl()).isEqualTo("http://localhost:8080/api/users");
        assertThat(span.httpStatusCode()).isEqualTo(200);
    }

    @Test
    void shouldExtractDbAttributes() {
        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .name("SELECT users")
                .kind(SpanKind.CLIENT)
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(50))
                .attribute("db.system", "postgresql")
                .attribute("db.statement", "SELECT * FROM users WHERE id = ?")
                .attribute("db.operation", "SELECT")
                .build();

        assertThat(span.dbSystem()).isEqualTo("postgresql");
        assertThat(span.dbStatement()).isEqualTo("SELECT * FROM users WHERE id = ?");
        assertThat(span.dbOperation()).isEqualTo("SELECT");
    }

    @Test
    void shouldHandleEvents() {
        SpanEvent event = SpanEvent.of(
                "exception",
                Instant.now(),
                Map.of(
                        "exception.type", "java.lang.NullPointerException",
                        "exception.message", "Value cannot be null"
                )
        );

        Span span = Span.builder()
                .traceId("trace-123")
                .spanId("span-456")
                .name("test")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .events(List.of(event))
                .build();

        assertThat(span.events()).hasSize(1);
        assertThat(span.events().get(0).isException()).isTrue();
        assertThat(span.events().get(0).exceptionType()).isEqualTo("java.lang.NullPointerException");
        assertThat(span.events().get(0).exceptionMessage()).isEqualTo("Value cannot be null");
    }
}
