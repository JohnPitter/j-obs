package io.github.jobs.spring.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for J-Obs WebSocket log streaming endpoint.
 * Validates connection, handshake messages, and filter functionality.
 */
@SpringBootTest(
        classes = E2ETestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "j-obs.enabled=true",
        "j-obs.path=/j-obs",
        "j-obs.security.enabled=false",
        "j-obs.rate-limiting.enabled=false",
        "j-obs.logs.enabled=true",
        "spring.main.allow-bean-definition-overriding=true",
        "j-obs.persistence.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
class WebSocketE2ETest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String wsUrl() {
        return "ws://localhost:" + port + "/j-obs/ws/logs";
    }

    @Test
    @DisplayName("WebSocket endpoint accepts connections")
    void webSocketEndpointAcceptsConnections() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<Boolean> connected = new CompletableFuture<>();

        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                connected.complete(true);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                connected.completeExceptionally(exception);
            }
        }, wsUrl()).get(5, TimeUnit.SECONDS);

        try {
            assertThat(connected.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(session.isOpen()).isTrue();
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("Server sends connected message after WebSocket handshake")
    @SuppressWarnings("unchecked")
    void serverSendsConnectedMessage() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<String> firstMessage = new CompletableFuture<>();

        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                firstMessage.complete(message.getPayload());
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                firstMessage.completeExceptionally(exception);
            }
        }, wsUrl()).get(5, TimeUnit.SECONDS);

        try {
            String payload = firstMessage.get(5, TimeUnit.SECONDS);
            Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);

            assertThat(parsed).containsKey("type");
            assertThat(parsed.get("type")).isEqualTo("connected");
            assertThat(parsed).containsKey("sessionId");
        } finally {
            session.close();
        }
    }

    @Test
    @DisplayName("Server acknowledges filter message with filterApplied response")
    @SuppressWarnings("unchecked")
    void filterMessageProducesFilterAppliedResponse() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<String> connectedMessage = new CompletableFuture<>();
        CompletableFuture<String> filterResponse = new CompletableFuture<>();

        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            private boolean receivedConnected = false;

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                if (!receivedConnected) {
                    receivedConnected = true;
                    connectedMessage.complete(message.getPayload());
                } else {
                    filterResponse.complete(message.getPayload());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                connectedMessage.completeExceptionally(exception);
                filterResponse.completeExceptionally(exception);
            }
        }, wsUrl()).get(5, TimeUnit.SECONDS);

        try {
            // Wait for the initial connected message
            connectedMessage.get(5, TimeUnit.SECONDS);

            // Send a filter command
            String filterPayload = objectMapper.writeValueAsString(Map.of(
                    "type", "filter",
                    "minLevel", "ERROR"
            ));
            session.sendMessage(new TextMessage(filterPayload));

            // Wait for the filterApplied response
            String responsePayload = filterResponse.get(5, TimeUnit.SECONDS);
            Map<String, Object> parsed = objectMapper.readValue(responsePayload, Map.class);

            assertThat(parsed).containsKey("type");
            assertThat(parsed.get("type")).isEqualTo("filterApplied");
            assertThat(parsed.get("minLevel")).isEqualTo("ERROR");
        } finally {
            session.close();
        }
    }
}
