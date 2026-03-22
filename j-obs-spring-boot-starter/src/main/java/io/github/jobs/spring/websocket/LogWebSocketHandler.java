package io.github.jobs.spring.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogEntry;
import io.github.jobs.domain.log.LogLevel;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket handler for real-time log streaming.
 * Limits concurrent sessions to prevent DoS attacks.
 */
public class LogWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketHandler.class);
    private static final int MAX_SESSIONS = 50;
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    private static final CloseStatus AUTHENTICATION_REQUIRED = new CloseStatus(4001, "Authentication required");

    private final LogRepository logRepository;
    private final JObsProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public LogWebSocketHandler(LogRepository logRepository) {
        this(logRepository, null);
    }

    public LogWebSocketHandler(LogRepository logRepository, JObsProperties properties) {
        this.logRepository = logRepository;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Start periodic cleanup of stale sessions
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "j-obs-websocket-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupStaleSessions,
                CLEANUP_INTERVAL.toMinutes(),
                CLEANUP_INTERVAL.toMinutes(),
                TimeUnit.MINUTES
        );
    }

    /**
     * Shuts down the cleanup executor. Should be called on application shutdown.
     */
    public void shutdown() {
        cleanupExecutor.shutdownNow();
        // Clean up all remaining sessions
        sessions.forEach((id, context) -> {
            if (context.getSubscription() != null) {
                context.getSubscription().unsubscribe();
            }
        });
        sessions.clear();
    }

    /**
     * Cleans up sessions that have been inactive for longer than SESSION_TIMEOUT.
     * This handles cases where afterConnectionClosed is not called due to errors.
     */
    private void cleanupStaleSessions() {
        Instant cutoff = Instant.now().minus(SESSION_TIMEOUT);
        int cleanedCount = 0;

        for (Map.Entry<String, SessionContext> entry : sessions.entrySet()) {
            SessionContext context = entry.getValue();

            boolean isStale = context.getLastActivity().isBefore(cutoff);
            boolean isClosed = !context.getSession().isOpen();

            if (isStale || isClosed) {
                String sessionId = entry.getKey();
                sessions.remove(sessionId);

                if (context.getSubscription() != null) {
                    context.getSubscription().unsubscribe();
                }

                if (!isClosed) {
                    safeClose(context.getSession(), new CloseStatus(4000, "Session timeout"));
                }

                cleanedCount++;
                log.debug("Cleaned up stale WebSocket session: {} (inactive for {})",
                        sessionId, Duration.between(context.getLastActivity(), Instant.now()));
            }
        }

        if (cleanedCount > 0) {
            log.info("Cleaned up {} stale WebSocket session(s), {} active sessions remaining",
                    cleanedCount, sessions.size());
        }
    }

    /**
     * Creates a HandshakeInterceptor that propagates HTTP session attributes
     * to WebSocket sessions for authentication verification.
     */
    public HandshakeInterceptor createAuthHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    var httpSession = servletRequest.getServletRequest().getSession(false);
                    if (httpSession != null) {
                        attributes.put("HTTP.SESSION", httpSession);
                        Object authenticated = httpSession.getAttribute("j-obs.authenticated");
                        if (Boolean.TRUE.equals(authenticated)) {
                            attributes.put("j-obs.authenticated", true);
                        }
                    }

                    // Also check for API key in query parameters
                    URI uri = request.getURI();
                    String query = uri.getQuery();
                    if (query != null && properties != null) {
                        String apiKey = extractQueryParam(query, "apiKey");
                        if (apiKey != null && isValidApiKey(apiKey)) {
                            attributes.put("j-obs.authenticated", true);
                        }
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                // No action needed
            }
        };
    }

    private String extractQueryParam(String query, String paramName) {
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(paramName)) {
                return parts[1];
            }
        }
        return null;
    }

    private boolean isValidApiKey(String apiKey) {
        if (properties == null) {
            return false;
        }
        JObsProperties.Security security = properties.getSecurity();
        List<String> validKeys = security.getApiKeys();
        return validKeys != null && validKeys.contains(apiKey);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();

        // Check authentication if security is enabled
        if (isSecurityEnabled() && !isAuthenticated(session)) {
            log.debug("WebSocket connection rejected (not authenticated): {}", sessionId);
            safeClose(session, AUTHENTICATION_REQUIRED);
            return;
        }

        // Reject if too many concurrent sessions (DoS protection)
        if (sessions.size() >= MAX_SESSIONS) {
            log.warn("WebSocket session limit reached ({}), rejecting connection: {}", MAX_SESSIONS, sessionId);
            safeClose(session, new CloseStatus(4029, "Too many connections"));
            return;
        }

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

        // Update last activity timestamp
        context.updateActivity();

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
        safeClose(session, CloseStatus.SERVER_ERROR);
    }

    /**
     * Safely close a WebSocket session, handling Tomcat version compatibility issues.
     * Some Tomcat versions (pre-10.1.1) may throw NoSuchMethodError when closing sessions
     * due to missing SocketWrapperBase.getLock() method.
     */
    private void safeClose(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (NoSuchMethodError e) {
            // Tomcat version compatibility issue - the session will be cleaned up anyway
            log.debug("WebSocket close failed due to Tomcat compatibility, session will be cleaned up: {}",
                    session.getId());
        } catch (Exception e) {
            log.debug("WebSocket close failed for session {}: {}", session.getId(), e.getMessage());
        }
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
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message: {}", e.getMessage());
        } catch (Throwable t) {
            // Handles NoSuchMethodError from Tomcat version incompatibility
            // and other fatal errors during WebSocket send
            log.error("WebSocket send failed fatally for session {}: {}", session.getId(), t.getMessage());
            safeClose(session, CloseStatus.SERVER_ERROR);
        }
    }

    private boolean isSecurityEnabled() {
        return properties != null && properties.getSecurity().isEnabled();
    }

    private boolean isAuthenticated(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();

        // Check if authenticated via HTTP session or API key during handshake
        Object authenticated = attributes.get("j-obs.authenticated");
        return Boolean.TRUE.equals(authenticated);
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
        private String messageFilterLower;
        private volatile boolean paused = false;
        private volatile Instant lastActivity;

        SessionContext(WebSocketSession session) {
            this.session = session;
            this.lastActivity = Instant.now();
        }

        void updateActivity() {
            this.lastActivity = Instant.now();
        }

        Instant getLastActivity() {
            return lastActivity;
        }

        WebSocketSession getSession() {
            return session;
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
            if (messageFilterLower != null && !messageFilterLower.isEmpty()) {
                if (!entry.message().toLowerCase().contains(messageFilterLower)) {
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
            this.messageFilterLower = (messageFilter != null) ? messageFilter.toLowerCase() : null;
        }

        void setPaused(boolean paused) {
            this.paused = paused;
        }
    }
}
