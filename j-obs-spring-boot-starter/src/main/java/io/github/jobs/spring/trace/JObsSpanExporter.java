package io.github.jobs.spring.trace;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.SpanEvent;
import io.github.jobs.domain.trace.SpanKind;
import io.github.jobs.domain.trace.SpanStatus;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenTelemetry SpanExporter that forwards spans to the J-Obs TraceRepository.
 */
public class JObsSpanExporter implements SpanExporter {

    private final TraceRepository traceRepository;

    public JObsSpanExporter(TraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        try {
            for (SpanData spanData : spans) {
                Span span = convertSpan(spanData);
                traceRepository.addSpan(span);
            }
            return CompletableResultCode.ofSuccess();
        } catch (Exception e) {
            return CompletableResultCode.ofFailure();
        }
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    private static final String INVALID_SPAN_ID = "0000000000000000";

    private Span convertSpan(SpanData spanData) {
        String parentSpanId = spanData.getParentSpanId();
        // Root spans have empty or all-zeros parent ID
        boolean isRootSpan = parentSpanId == null || parentSpanId.isEmpty() || INVALID_SPAN_ID.equals(parentSpanId);

        return Span.builder()
                .traceId(spanData.getTraceId())
                .spanId(spanData.getSpanId())
                .parentSpanId(isRootSpan ? null : parentSpanId)
                .name(spanData.getName())
                .kind(convertSpanKind(spanData.getKind()))
                .startTime(Instant.ofEpochSecond(0, spanData.getStartEpochNanos()))
                .endTime(Instant.ofEpochSecond(0, spanData.getEndEpochNanos()))
                .status(convertStatus(spanData.getStatus()))
                .statusMessage(spanData.getStatus().getDescription())
                .serviceName(extractServiceName(spanData))
                .attributes(convertAttributes(spanData))
                .events(convertEvents(spanData.getEvents()))
                .build();
    }

    private SpanKind convertSpanKind(io.opentelemetry.api.trace.SpanKind kind) {
        return switch (kind) {
            case SERVER -> SpanKind.SERVER;
            case CLIENT -> SpanKind.CLIENT;
            case PRODUCER -> SpanKind.PRODUCER;
            case CONSUMER -> SpanKind.CONSUMER;
            case INTERNAL -> SpanKind.INTERNAL;
        };
    }

    private SpanStatus convertStatus(io.opentelemetry.sdk.trace.data.StatusData status) {
        return switch (status.getStatusCode()) {
            case OK -> SpanStatus.OK;
            case ERROR -> SpanStatus.ERROR;
            case UNSET -> SpanStatus.UNSET;
        };
    }

    private String extractServiceName(SpanData spanData) {
        // Try resource attributes first
        String serviceName = spanData.getResource().getAttribute(
            io.opentelemetry.api.common.AttributeKey.stringKey("service.name")
        );
        if (serviceName != null && !serviceName.isEmpty()) {
            return serviceName;
        }
        return "unknown";
    }

    private Map<String, String> convertAttributes(SpanData spanData) {
        Map<String, String> attributes = new HashMap<>();

        spanData.getAttributes().forEach((key, value) -> {
            attributes.put(key.getKey(), String.valueOf(value));
        });

        return attributes;
    }

    private List<SpanEvent> convertEvents(List<EventData> events) {
        return events.stream()
            .map(this::convertEvent)
            .collect(Collectors.toList());
    }

    private SpanEvent convertEvent(EventData eventData) {
        Map<String, String> attributes = new HashMap<>();
        eventData.getAttributes().forEach((key, value) -> {
            attributes.put(key.getKey(), String.valueOf(value));
        });

        return SpanEvent.of(
            eventData.getName(),
            Instant.ofEpochSecond(0, eventData.getEpochNanos()),
            attributes
        );
    }
}
