package io.github.jobs.spring.alert.provider;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Telegram;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Alert notification provider for Telegram using the Bot API.
 *
 * <p>Sends alert notifications to one or more Telegram chats using a bot token.
 * Messages are formatted using Markdown for rich text display.</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * j-obs:
 *   alerts:
 *     providers:
 *       telegram:
 *         enabled: true
 *         bot-token: ${TELEGRAM_BOT_TOKEN}
 *         chat-ids:
 *           - "-1001234567890"
 *           - "987654321"
 * }</pre>
 *
 * <h2>Message Format</h2>
 * <p>Messages include: status emoji, alert name (bold), message text, severity,
 * status, timestamp, and labels (if any).</p>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AbstractAlertProvider
 */
public class TelegramAlertProvider extends AbstractAlertProvider {

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final Telegram config;

    /**
     * Creates a new Telegram provider with default 30 second timeout.
     *
     * @param config the Telegram configuration containing bot token and chat IDs
     */
    public TelegramAlertProvider(Telegram config) {
        this(config, Duration.ofSeconds(30));
    }

    /**
     * Creates a new Telegram provider with custom timeout.
     *
     * @param config the Telegram configuration containing bot token and chat IDs
     * @param timeout the HTTP request timeout
     */
    public TelegramAlertProvider(Telegram config, Duration timeout) {
        super(timeout);
        this.config = config;
    }

    @Override
    public String getName() {
        return "telegram";
    }

    @Override
    public String getDisplayName() {
        return "Telegram";
    }

    @Override
    public boolean isConfigured() {
        return config.isConfigured();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    protected CompletableFuture<AlertNotificationResult> doSend(AlertEvent event) {
        String message = formatMessage(event);
        List<String> chatIds = config.getChatIds();

        if (chatIds.isEmpty()) {
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(getName(), "No chat IDs configured")
            );
        }

        List<CompletableFuture<AlertNotificationResult>> futures = chatIds.stream()
                .map(chatId -> sendToChat(chatId, message))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    boolean allSuccess = futures.stream()
                            .map(CompletableFuture::join)
                            .allMatch(AlertNotificationResult::success);
                    if (allSuccess) {
                        return AlertNotificationResult.success(getName(),
                                "Sent to " + chatIds.size() + " chat(s)");
                    } else {
                        return AlertNotificationResult.failure(getName(),
                                "Some messages failed to send");
                    }
                });
    }

    private CompletableFuture<AlertNotificationResult> sendToChat(String chatId, String message) {
        String url = TELEGRAM_API_BASE + config.getBotToken() + "/sendMessage";
        String body = String.format(
                "{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"Markdown\"}",
                escapeJson(chatId),
                escapeJson(message)
        );

        return sendHttpPost(url, body, "application/json");
    }

    private String formatMessage(AlertEvent event) {
        StringBuilder sb = new StringBuilder();

        sb.append(getStatusEmoji(event));
        sb.append(" *").append(escapeMarkdown(event.alertName())).append("*\n\n");

        sb.append(escapeMarkdown(event.message())).append("\n\n");

        sb.append("*Severity:* `").append(event.severity().displayName()).append("`\n");
        sb.append("*Status:* `").append(event.status().displayName()).append("`\n");
        sb.append("*Time:* `").append(formatTimestamp(event)).append("`\n");

        if (!event.labels().isEmpty()) {
            sb.append("\n*Labels:*\n");
            event.labels().forEach((key, value) ->
                    sb.append("  • ").append(escapeMarkdown(key)).append(": `")
                            .append(escapeMarkdown(value)).append("`\n")
            );
        }

        return sb.toString();
    }
}
