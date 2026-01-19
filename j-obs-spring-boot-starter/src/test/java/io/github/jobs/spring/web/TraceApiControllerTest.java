package io.github.jobs.spring.web;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.*;
import io.github.jobs.infrastructure.InMemoryTraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TraceApiController.class)
class TraceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TraceRepository traceRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TraceRepository traceRepository() {
            return new InMemoryTraceRepository(Duration.ofHours(1), 1000);
        }
    }

    @BeforeEach
    void setUp() {
        traceRepository.clear();
    }

    @Test
    void shouldReturnEmptyTracesWhenNoneExist() throws Exception {
        mockMvc.perform(get("/j-obs/api/traces")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(0)))
                .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    void shouldReturnTraces() throws Exception {
        addSpan("trace-1", "span-1", null, "GET /api/users", "user-service", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "POST /api/orders", "order-service", SpanStatus.OK, 200);

        mockMvc.perform(get("/j-obs/api/traces")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(2)))
                .andExpect(jsonPath("$.total", is(2)));
    }

    @Test
    void shouldFilterTracesByService() throws Exception {
        addSpan("trace-1", "span-1", null, "GET /api/users", "user-service", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "POST /api/orders", "order-service", SpanStatus.OK, 200);

        mockMvc.perform(get("/j-obs/api/traces")
                        .param("service", "user-service")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(1)))
                .andExpect(jsonPath("$.traces[0].serviceName", is("user-service")));
    }

    @Test
    void shouldFilterTracesByStatus() throws Exception {
        addSpan("trace-1", "span-1", null, "GET /api/users", "user-service", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "POST /api/orders", "order-service", SpanStatus.ERROR, 200);

        mockMvc.perform(get("/j-obs/api/traces")
                        .param("status", "ERROR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(1)))
                .andExpect(jsonPath("$.traces[0].status", is("ERROR")));
    }

    @Test
    void shouldFilterTracesByMinDuration() throws Exception {
        addSpan("trace-1", "span-1", null, "fast", "service", SpanStatus.OK, 50);
        addSpan("trace-2", "span-2", null, "slow", "service", SpanStatus.OK, 200);

        mockMvc.perform(get("/j-obs/api/traces")
                        .param("minDurationMs", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(1)))
                .andExpect(jsonPath("$.traces[0].name", is("slow")));
    }

    @Test
    void shouldGetTraceById() throws Exception {
        addSpan("trace-123", "span-1", null, "GET /api/users", "user-service", SpanStatus.OK, 100);

        mockMvc.perform(get("/j-obs/api/traces/trace-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId", is("trace-123")))
                .andExpect(jsonPath("$.name", is("GET /api/users")))
                .andExpect(jsonPath("$.serviceName", is("user-service")))
                .andExpect(jsonPath("$.spansIncluded", is(true)));
    }

    @Test
    void shouldGetTraceWithoutSpans() throws Exception {
        Instant start = Instant.now();
        addSpanWithTime("trace-no-spans", "span-1", null, "GET /api", "service", SpanStatus.OK, start, 100);
        addSpanWithTime("trace-no-spans", "span-2", "span-1", "DB query", "service", SpanStatus.OK, start.plusMillis(10), 50);

        mockMvc.perform(get("/j-obs/api/traces/trace-no-spans")
                        .param("includeSpans", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId", is("trace-no-spans")))
                .andExpect(jsonPath("$.spanCount", is(2)))
                .andExpect(jsonPath("$.spansIncluded", is(false)))
                .andExpect(jsonPath("$.spans", hasSize(0)));
    }

    @Test
    void shouldGetTraceWithLimitedSpans() throws Exception {
        Instant start = Instant.now();
        addSpanWithTime("trace-limited", "span-1", null, "Span 1", "service", SpanStatus.OK, start, 100);
        addSpanWithTime("trace-limited", "span-2", "span-1", "Span 2", "service", SpanStatus.OK, start.plusMillis(10), 50);
        addSpanWithTime("trace-limited", "span-3", "span-1", "Span 3", "service", SpanStatus.OK, start.plusMillis(20), 30);

        mockMvc.perform(get("/j-obs/api/traces/trace-limited")
                        .param("maxSpans", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spanCount", is(3)))
                .andExpect(jsonPath("$.spans", hasSize(2)))
                .andExpect(jsonPath("$.spansIncluded", is(true)))
                .andExpect(jsonPath("$.hasMoreSpans", is(true)));
    }

    @Test
    void shouldReturn404ForNonExistentTrace() throws Exception {
        mockMvc.perform(get("/j-obs/api/traces/non-existent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetSpansForTrace() throws Exception {
        Instant start = Instant.now();
        addSpanWithTime("trace-123", "span-1", null, "GET /api/users", "user-service", SpanStatus.OK, start, 100);
        addSpanWithTime("trace-123", "span-2", "span-1", "SELECT users", "user-service", SpanStatus.OK, start.plusMillis(10), 50);

        mockMvc.perform(get("/j-obs/api/traces/trace-123/spans")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spans", hasSize(2)))
                .andExpect(jsonPath("$.spans[0].name", is("GET /api/users")))
                .andExpect(jsonPath("$.spans[0].depth", is(0)))
                .andExpect(jsonPath("$.spans[1].name", is("SELECT users")))
                .andExpect(jsonPath("$.spans[1].depth", is(1)))
                .andExpect(jsonPath("$.total", is(2)))
                .andExpect(jsonPath("$.hasMore", is(false)));
    }

    @Test
    void shouldGetSpansWithPagination() throws Exception {
        Instant start = Instant.now();
        addSpanWithTime("trace-paged", "span-1", null, "Span 1", "service", SpanStatus.OK, start, 100);
        addSpanWithTime("trace-paged", "span-2", "span-1", "Span 2", "service", SpanStatus.OK, start.plusMillis(10), 50);
        addSpanWithTime("trace-paged", "span-3", "span-1", "Span 3", "service", SpanStatus.OK, start.plusMillis(20), 30);

        // Get first page with limit 2
        mockMvc.perform(get("/j-obs/api/traces/trace-paged/spans")
                        .param("limit", "2")
                        .param("offset", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spans", hasSize(2)))
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.hasMore", is(true)));

        // Get second page
        mockMvc.perform(get("/j-obs/api/traces/trace-paged/spans")
                        .param("limit", "2")
                        .param("offset", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spans", hasSize(1)))
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.hasMore", is(false)));
    }

    @Test
    void shouldGetTraceStats() throws Exception {
        addSpan("trace-1", "span-1", null, "name", "service-a", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "name", "service-b", SpanStatus.ERROR, 200);

        mockMvc.perform(get("/j-obs/api/traces/stats")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTraces", is(2)))
                .andExpect(jsonPath("$.errorTraces", is(1)));
    }

    @Test
    void shouldGetServices() throws Exception {
        addSpan("trace-1", "span-1", null, "name", "user-service", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "name", "order-service", SpanStatus.OK, 200);
        addSpan("trace-3", "span-3", null, "name", "user-service", SpanStatus.OK, 150);

        mockMvc.perform(get("/j-obs/api/traces/services")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("order-service", "user-service")));
    }

    @Test
    void shouldClearTraces() throws Exception {
        addSpan("trace-1", "span-1", null, "name", "service", SpanStatus.OK, 100);
        addSpan("trace-2", "span-2", null, "name", "service", SpanStatus.OK, 200);

        mockMvc.perform(delete("/j-obs/api/traces"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/j-obs/api/traces")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traces", hasSize(0)));
    }

    private void addSpan(String traceId, String spanId, String parentSpanId, String name, String serviceName, SpanStatus status, long durationMs) {
        Instant start = Instant.now();
        addSpanWithTime(traceId, spanId, parentSpanId, name, serviceName, status, start, durationMs);
    }

    private void addSpanWithTime(String traceId, String spanId, String parentSpanId, String name, String serviceName, SpanStatus status, Instant start, long durationMs) {
        Span span = Span.builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .name(name)
                .serviceName(serviceName)
                .kind(SpanKind.SERVER)
                .status(status)
                .startTime(start)
                .endTime(start.plusMillis(durationMs))
                .build();
        traceRepository.addSpan(span);
    }
}
