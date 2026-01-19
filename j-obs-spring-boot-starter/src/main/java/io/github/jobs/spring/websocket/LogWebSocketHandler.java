package io.github.jobs.spring.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time log streaming.
 */
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketHandler.class);

    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public LogWebSocketHandler(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.debug("WebSocket connection established: {}", sessionId);

        SessionContext context = new SessionContext(session);
        sessions.put(sessionId, context);

        // Subscribe to new log entries
        LogRepository.Subscription subscription = logRepository.subscribe(entry -> {
            if (context.shouldSend(entry)) {
                sendLogEntry(session, entry);
            }
        });
        context.setSubscription(subscription);

        // Send connection confirmation
        sendMessage(session, Map.of(
                "type", "connected",
                "sessionId", sessionId
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        SessionContext context = sessions.get(sessionId);

        if (context == null) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            if ("filter".equals(type)) {
                // Update filter settings
                String minLevel = (String) payload.get("minLevel");
                String loggerFilter = (String) payload.get("logger");
                String messageFilter = (String) payload.get("message");

                if (minLevel != null) {
                    context.setMinLevel(LogLevel.fromString(minLevel));
                }
                context.setLoggerFilter(loggerFilter);
                context.setMessageFilter(messageFilter);

                sendMessage(session, Map.of(
                        "type", "filterApplied",
                        "minLevel", context.getMinLevel().name(),
                        "logger", context.getLoggerFilter() != null ? context.getLoggerFilter() : "",
                        "message", context.getMessageFilter() != null ? context.getMessageFilter() : ""
                ));
            } else if ("pause".equals(type)) {
                context.setPaused(true);
                sendMessage(session, Map.of("type", "paused"));
            } else if ("resume".equals(type)) {
                context.setPaused(false);
                sendMessage(session, Map.of("type", "resumed"));
            }
        } catch (Exception e) {
            log.warn("Error handling WebSocket message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.debug("WebSocket connection closed: {} ({})", sessionId, status);

        SessionContext context = sessions.remove(sessionId);
        if (context != null && context.getSubscription() != null) {
            context.getSubscription().unsubscribe();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    private void sendLogEntry(WebSocketSession session, LogEntry entry) {
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
            sendMessage(session, message);
        } catch (Exception e) {
            log.warn("Failed to send log entry to WebSocket: {}", e.getMessage());
        }
    }

    private void sendMessage(WebSocketSession session, Object message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        }
    }

    /**
     * Context for a WebSocket session including filter settings.
     */
    private static class SessionContext {
        private final WebSocketSession session;
        private LogRepository.Subscription subscription;
        private LogLevel minLevel = LogLevel.INFO;
        private String loggerFilter;
        private String messageFilter;
        private volatile boolean paused = false;

        SessionContext(WebSocketSession session) {
            this.session = session;
        }

        boolean shouldSend(LogEntry entry) {
            if (paused) {
                return false;
            }
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

        LogRepository.Subscription getSubscription() {
            return subscription;
        }

        void setSubscription(LogRepository.Subscription subscription) {
            this.subscription = subscription;
        }

        LogLevel getMinLevel() {
            return minLevel;
        }

        void setMinLevel(LogLevel minLevel) {
            this.minLevel = minLevel;
        }

        String getLoggerFilter() {
            return loggerFilter;
        }

        void setLoggerFilter(String loggerFilter) {
            this.loggerFilter = loggerFilter;
        }

        String getMessageFilter() {
            return messageFilter;
        }

        void setMessageFilter(String messageFilter) {
            this.messageFilter = messageFilter;
        }

        void setPaused(boolean paused) {
            this.paused = paused;
        }
    }
}
