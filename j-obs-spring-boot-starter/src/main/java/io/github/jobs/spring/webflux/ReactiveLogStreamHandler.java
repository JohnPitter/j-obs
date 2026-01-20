package io.github.jobs.spring.webflux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive handler for streaming logs via Server-Sent Events in WebFlux applications.
 * <p>
 * Provides real-time log streaming without requiring WebSocket support,
 * making it ideal for reactive applications.
 */
public class ReactiveLogStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveLogStreamHandler.class);

    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;

    public ReactiveLogStreamHandler(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Creates a Flux that streams log entries as Server-Sent Events.
     *
     * @param minLevel minimum log level to include (default: INFO)
     * @param loggerFilter optional logger name filter
     * @param messageFilter optional message content filter
     * @return Flux of ServerSentEvent containing log entries
     */
    public Flux<ServerSentEvent<String>> streamLogs(
            LogLevel minLevel,
            String loggerFilter,
            String messageFilter) {

        Sinks.Many<LogEntry> sink = Sinks.many().multicast().onBackpressureBuffer(1000);
        AtomicReference<LogRepository.Subscription> subscriptionRef = new AtomicReference<>();

        // Subscribe to log repository
        LogRepository.Subscription subscription = logRepository.subscribe(entry -> {
            if (shouldInclude(entry, minLevel, loggerFilter, messageFilter)) {
                sink.tryEmitNext(entry);
            }
        });
        subscriptionRef.set(subscription);

        return sink.asFlux()
                .map(this::toServerSentEvent)
                .mergeWith(heartbeat())
                .doOnCancel(() -> {
                    LogRepository.Subscription sub = subscriptionRef.get();
                    if (sub != null) {
                        sub.unsubscribe();
                        log.debug("Unsubscribed from log repository on client disconnect");
                    }
                })
                .doOnTerminate(() -> {
                    LogRepository.Subscription sub = subscriptionRef.get();
                    if (sub != null) {
                        sub.unsubscribe();
                    }
                });
    }

    /**
     * Creates a heartbeat Flux to keep the connection alive.
     */
    private Flux<ServerSentEvent<String>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("{\"type\":\"heartbeat\"}")
                        .build());
    }

    private boolean shouldInclude(
            LogEntry entry,
            LogLevel minLevel,
            String loggerFilter,
            String messageFilter) {

        if (!entry.level().isAtLeast(minLevel)) {
            return false;
        }

        if (loggerFilter != null && !loggerFilter.isEmpty()) {
            if (entry.loggerName() == null || !entry.loggerName().contains(loggerFilter)) {
                return false;
            }
        }

        if (messageFilter != null && !messageFilter.isEmpty()) {
            if (!entry.message().toLowerCase().contains(messageFilter.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private ServerSentEvent<String> toServerSentEvent(LogEntry entry) {
        try {
            Map<String, Object> data = Map.ofEntries(
                    Map.entry("id", entry.id()),
                    Map.entry("timestamp", entry.timestamp().toString()),
                    Map.entry("level", entry.level().name()),
                    Map.entry("levelCss", entry.level().textCssClass()),
                    Map.entry("levelBgCss", entry.level().bgCssClass()),
                    Map.entry("logger", entry.loggerName() != null ? entry.loggerName() : ""),
                    Map.entry("shortLogger", entry.shortLoggerName() != null ? entry.shortLoggerName() : ""),
                    Map.entry("message", entry.message()),
                    Map.entry("thread", entry.threadName() != null ? entry.threadName() : ""),
                    Map.entry("traceId", entry.traceId() != null ? entry.traceId() : ""),
                    Map.entry("hasThrowable", entry.hasThrowable()),
                    Map.entry("throwable", entry.throwable() != null ? entry.throwable() : "")
            );

            Map<String, Object> message = Map.of("type", "log", "data", data);
            String json = objectMapper.writeValueAsString(message);

            return ServerSentEvent.<String>builder()
                    .id(entry.id())
                    .event("log")
                    .data(json)
                    .build();
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize log entry: {}", e.getMessage());
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\":\"serialization_failed\"}")
                    .build();
        }
    }
}
