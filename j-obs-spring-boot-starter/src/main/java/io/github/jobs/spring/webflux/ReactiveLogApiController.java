package io.github.jobs.spring.webflux;

import io.github.jobs.domain.log.LogLevel;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Reactive REST controller for log streaming via Server-Sent Events.
 * <p>
 * This controller provides real-time log streaming in WebFlux applications
 * using SSE instead of WebSocket.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/logs")
public class ReactiveLogApiController {

    private final ReactiveLogStreamHandler streamHandler;

    public ReactiveLogApiController(ReactiveLogStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * Streams log entries as Server-Sent Events.
     * <p>
     * Example usage with JavaScript:
     * <pre>
     * const eventSource = new EventSource('/j-obs/api/logs/stream?minLevel=INFO');
     * eventSource.addEventListener('log', (event) => {
     *     const log = JSON.parse(event.data);
     *     console.log(log.data.message);
     * });
     * </pre>
     *
     * @param minLevel minimum log level to stream (default: INFO)
     * @param logger filter by logger name (contains match)
     * @param message filter by message content (case-insensitive contains match)
     * @return Flux of ServerSentEvent containing log entries
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLogs(
            @RequestParam(defaultValue = "INFO") String minLevel,
            @RequestParam(required = false) String logger,
            @RequestParam(required = false) String message) {

        LogLevel level = LogLevel.fromString(minLevel);
        return streamHandler.streamLogs(level, logger, message);
    }
}
