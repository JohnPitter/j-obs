package io.github.jobs.spring.autoconfigure;

import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Email;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Providers;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Slack;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Teams;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Telegram;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Throttling;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Webhook;
import io.github.jobs.spring.autoconfigure.JObsProperties.Dashboard;
import io.github.jobs.spring.autoconfigure.JObsProperties.Health;
import io.github.jobs.spring.autoconfigure.JObsProperties.Logs;
import io.github.jobs.spring.autoconfigure.JObsProperties.Metrics;
import io.github.jobs.spring.autoconfigure.JObsProperties.RateLimiting;
import io.github.jobs.spring.autoconfigure.JObsProperties.Security;
import io.github.jobs.spring.autoconfigure.JObsProperties.Traces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates JObsProperties at application startup.
 * Logs warnings for invalid configurations and provides sensible defaults.
 */
public class JObsPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(JObsPropertiesValidator.class);

    private static final Set<String> VALID_THEMES = Set.of("system", "light", "dark");
    private static final Set<String> VALID_LOG_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final Set<String> VALID_HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    private final JObsProperties properties;

    public JObsPropertiesValidator(JObsProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void validateOnStartup() {
        validate();
    }

    /**
     * Validates all properties and returns a list of validation errors.
     * Throws IllegalStateException for fatal misconfigurations to fail fast on startup.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        List<String> fatalErrors = new ArrayList<>();

        validatePath(errors);
        validateCheckInterval(errors);
        validateSecurity(errors, fatalErrors);
        validateRateLimiting(errors, fatalErrors);
        validateDashboard(errors);
        validateTraces(errors);
        validateLogs(errors);
        validateMetrics(errors);
        validateHealth(errors);
        validateAlerts(errors);

        if (!errors.isEmpty()) {
            log.warn("J-Obs configuration validation found {} issue(s):", errors.size());
            for (String error : errors) {
                log.warn("  - {}", error);
            }
        }

        if (!fatalErrors.isEmpty()) {
            String message = "J-Obs configuration is invalid and cannot start:\n" +
                    String.join("\n", fatalErrors.stream().map(e -> "  - " + e).toList());
            log.error(message);
            throw new IllegalStateException(message);
        }

        // Warn if security is disabled
        if (!properties.getSecurity().isEnabled()) {
            log.warn("J-Obs dashboard is accessible without authentication. " +
                     "Set j-obs.security.enabled=true for production use.");
        }

        return errors;
    }

    private void validateSecurity(List<String> errors, List<String> fatalErrors) {
        Security security = properties.getSecurity();
        if (!security.isEnabled()) {
            return;
        }

        String authType = security.getType() != null ? security.getType().toLowerCase() : "basic";
        boolean hasUsers = security.getUsers() != null && !security.getUsers().isEmpty();
        boolean hasApiKeys = security.getApiKeys() != null && !security.getApiKeys().isEmpty();

        switch (authType) {
            case "basic" -> {
                if (!hasUsers) {
                    fatalErrors.add("j-obs.security.enabled=true with type=basic but no users configured. " +
                            "Add at least one user via j-obs.security.users[0].username/password");
                }
            }
            case "api-key" -> {
                if (!hasApiKeys) {
                    fatalErrors.add("j-obs.security.enabled=true with type=api-key but no API keys configured. " +
                            "Add at least one key via j-obs.security.api-keys[0]");
                }
            }
            case "both" -> {
                if (!hasUsers && !hasApiKeys) {
                    fatalErrors.add("j-obs.security.enabled=true with type=both but neither users nor API keys configured. " +
                            "Add users via j-obs.security.users or API keys via j-obs.security.api-keys");
                }
            }
            default -> fatalErrors.add("j-obs.security.type must be one of: basic, api-key, both. Got: " + security.getType());
        }
    }

    private void validateRateLimiting(List<String> errors, List<String> fatalErrors) {
        RateLimiting rateLimiting = properties.getRateLimiting();
        if (!rateLimiting.isEnabled()) {
            return;
        }

        if (rateLimiting.getMaxRequests() < 1) {
            fatalErrors.add("j-obs.rate-limiting.max-requests must be at least 1. Got: " + rateLimiting.getMaxRequests());
        }

        Duration window = rateLimiting.getWindow();
        if (window == null || window.isNegative() || window.isZero()) {
            errors.add("j-obs.rate-limiting.window must be positive, using default '1m'");
            rateLimiting.setWindow(Duration.ofMinutes(1));
        }
    }

    private void validatePath(List<String> errors) {
        String path = properties.getPath();
        if (path == null || path.isBlank()) {
            errors.add("j-obs.path cannot be empty, using default '/j-obs'");
            properties.setPath("/j-obs");
        } else if (!path.startsWith("/")) {
            errors.add("j-obs.path must start with '/', correcting to '/" + path + "'");
            properties.setPath("/" + path);
        }
    }

    private void validateCheckInterval(List<String> errors) {
        Duration interval = properties.getCheckInterval();
        if (interval == null || interval.isNegative() || interval.isZero()) {
            errors.add("j-obs.check-interval must be positive, using default '5m'");
            properties.setCheckInterval(Duration.ofMinutes(5));
        }
    }

    private void validateDashboard(List<String> errors) {
        Dashboard dashboard = properties.getDashboard();

        Duration refreshInterval = dashboard.getRefreshInterval();
        if (refreshInterval == null || refreshInterval.isNegative() || refreshInterval.isZero()) {
            errors.add("j-obs.dashboard.refresh-interval must be positive, using default '5s'");
            dashboard.setRefreshInterval(Duration.ofSeconds(5));
        } else if (refreshInterval.toMillis() < 1000) {
            errors.add("j-obs.dashboard.refresh-interval should be at least 1s for performance");
        }

        String theme = dashboard.getTheme();
        if (theme == null || !VALID_THEMES.contains(theme.toLowerCase())) {
            errors.add("j-obs.dashboard.theme must be one of: " + VALID_THEMES + ", using 'system'");
            dashboard.setTheme("system");
        }
    }

    private void validateTraces(List<String> errors) {
        Traces traces = properties.getTraces();

        if (traces.getMaxTraces() <= 0) {
            errors.add("j-obs.traces.max-traces must be positive, using default '10000'");
            traces.setMaxTraces(10000);
        } else if (traces.getMaxTraces() < 100) {
            errors.add("j-obs.traces.max-traces=" + traces.getMaxTraces() +
                    " is very small and may cause traces to be discarded too quickly. Recommended minimum: 100");
        } else if (traces.getMaxTraces() > 1000000) {
            errors.add("j-obs.traces.max-traces is very high (" + traces.getMaxTraces() + "), may cause memory issues");
        }

        Duration retention = traces.getRetention();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            errors.add("j-obs.traces.retention must be positive, using default '1h'");
            traces.setRetention(Duration.ofHours(1));
        } else if (retention.toMinutes() < 1) {
            errors.add("j-obs.traces.retention=" + retention +
                    " is less than 1 minute, traces will be discarded almost immediately. Recommended minimum: 1m");
        }

        double sampleRate = traces.getSampleRate();
        if (sampleRate < 0.0 || sampleRate > 1.0) {
            errors.add("j-obs.traces.sample-rate must be between 0.0 and 1.0, using default '1.0'");
            traces.setSampleRate(1.0);
        }
    }

    private void validateLogs(List<String> errors) {
        Logs logs = properties.getLogs();

        if (logs.getMaxEntries() <= 0) {
            errors.add("j-obs.logs.max-entries must be positive, using default '10000'");
            logs.setMaxEntries(10000);
        } else if (logs.getMaxEntries() > 1000000) {
            errors.add("j-obs.logs.max-entries is very high (" + logs.getMaxEntries() + "), may cause memory issues");
        }

        String minLevel = logs.getMinLevel();
        if (minLevel == null || !VALID_LOG_LEVELS.contains(minLevel.toUpperCase())) {
            errors.add("j-obs.logs.min-level must be one of: " + VALID_LOG_LEVELS + ", using 'INFO'");
            logs.setMinLevel("INFO");
        }
    }

    private void validateMetrics(List<String> errors) {
        Metrics metrics = properties.getMetrics();

        if (metrics.getRefreshInterval() <= 0) {
            errors.add("j-obs.metrics.refresh-interval must be positive, using default '10000'");
            metrics.setRefreshInterval(10000);
        } else if (metrics.getRefreshInterval() < 1000) {
            errors.add("j-obs.metrics.refresh-interval should be at least 1000ms for performance");
        }

        if (metrics.getMaxHistoryPoints() <= 0) {
            errors.add("j-obs.metrics.max-history-points must be positive, using default '360'");
            metrics.setMaxHistoryPoints(360);
        }
    }

    private void validateHealth(List<String> errors) {
        Health health = properties.getHealth();

        if (health.getMaxHistoryEntries() <= 0) {
            errors.add("j-obs.health.max-history-entries must be positive, using default '100'");
            health.setMaxHistoryEntries(100);
        }
    }

    private void validateAlerts(List<String> errors) {
        Alerts alerts = properties.getAlerts();

        Duration evaluationInterval = alerts.getEvaluationInterval();
        if (evaluationInterval == null || evaluationInterval.isNegative() || evaluationInterval.isZero()) {
            errors.add("j-obs.alerts.evaluation-interval must be positive, using default '15s'");
            alerts.setEvaluationInterval(Duration.ofSeconds(15));
        }

        if (alerts.getMaxEvents() <= 0) {
            errors.add("j-obs.alerts.max-events must be positive, using default '10000'");
            alerts.setMaxEvents(10000);
        }

        Duration retention = alerts.getRetention();
        if (retention == null || retention.isNegative() || retention.isZero()) {
            errors.add("j-obs.alerts.retention must be positive, using default '7d'");
            alerts.setRetention(Duration.ofDays(7));
        }

        validateThrottling(alerts.getThrottling(), errors);
        validateProviders(alerts.getProviders(), errors);
    }

    private void validateThrottling(Throttling throttling, List<String> errors) {
        if (throttling.getRateLimit() <= 0) {
            errors.add("j-obs.alerts.throttling.rate-limit must be positive, using default '10'");
            throttling.setRateLimit(10);
        }

        Duration ratePeriod = throttling.getRatePeriod();
        if (ratePeriod == null || ratePeriod.isNegative() || ratePeriod.isZero()) {
            errors.add("j-obs.alerts.throttling.rate-period must be positive, using default '1m'");
            throttling.setRatePeriod(Duration.ofMinutes(1));
        }

        Duration cooldown = throttling.getCooldown();
        if (cooldown == null || cooldown.isNegative() || cooldown.isZero()) {
            errors.add("j-obs.alerts.throttling.cooldown must be positive, using default '5m'");
            throttling.setCooldown(Duration.ofMinutes(5));
        }

        Duration groupWait = throttling.getGroupWait();
        if (groupWait == null || groupWait.isNegative() || groupWait.isZero()) {
            errors.add("j-obs.alerts.throttling.group-wait must be positive, using default '30s'");
            throttling.setGroupWait(Duration.ofSeconds(30));
        }

        Duration repeatInterval = throttling.getRepeatInterval();
        if (repeatInterval == null || repeatInterval.isNegative() || repeatInterval.isZero()) {
            errors.add("j-obs.alerts.throttling.repeat-interval must be positive, using default '4h'");
            throttling.setRepeatInterval(Duration.ofHours(4));
        }
    }

    private void validateProviders(Providers providers, List<String> errors) {
        validateTelegram(providers.getTelegram(), errors);
        validateTeams(providers.getTeams(), errors);
        validateSlack(providers.getSlack(), errors);
        validateEmail(providers.getEmail(), errors);
        validateWebhook(providers.getWebhook(), errors);
    }

    private void validateTelegram(Telegram telegram, List<String> errors) {
        if (telegram.isEnabled()) {
            if (telegram.getBotToken() == null || telegram.getBotToken().isBlank()) {
                errors.add("j-obs.alerts.providers.telegram.bot-token is required when Telegram is enabled");
            }
            if (telegram.getChatIds() == null || telegram.getChatIds().isEmpty()) {
                errors.add("j-obs.alerts.providers.telegram.chat-ids must have at least one entry when Telegram is enabled");
            }
        }
    }

    private void validateTeams(Teams teams, List<String> errors) {
        if (teams.isEnabled()) {
            String webhookUrl = teams.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.isBlank()) {
                errors.add("j-obs.alerts.providers.teams.webhook-url is required when Teams is enabled");
            } else if (!URL_PATTERN.matcher(webhookUrl).matches()) {
                errors.add("j-obs.alerts.providers.teams.webhook-url must be a valid HTTP/HTTPS URL");
            }
        }
    }

    private void validateSlack(Slack slack, List<String> errors) {
        if (slack.isEnabled()) {
            String webhookUrl = slack.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.isBlank()) {
                errors.add("j-obs.alerts.providers.slack.webhook-url is required when Slack is enabled");
            } else if (!URL_PATTERN.matcher(webhookUrl).matches()) {
                errors.add("j-obs.alerts.providers.slack.webhook-url must be a valid HTTP/HTTPS URL");
            }
        }
    }

    private void validateEmail(Email email, List<String> errors) {
        if (email.isEnabled()) {
            if (email.getHost() == null || email.getHost().isBlank()) {
                errors.add("j-obs.alerts.providers.email.host is required when Email is enabled");
            }

            int port = email.getPort();
            if (port <= 0 || port > 65535) {
                errors.add("j-obs.alerts.providers.email.port must be between 1 and 65535, using default '587'");
                email.setPort(587);
            }

            String from = email.getFrom();
            if (from == null || from.isBlank()) {
                errors.add("j-obs.alerts.providers.email.from is required when Email is enabled");
            } else if (!EMAIL_PATTERN.matcher(from).matches()) {
                errors.add("j-obs.alerts.providers.email.from must be a valid email address");
            }

            if (email.getTo() == null || email.getTo().isEmpty()) {
                errors.add("j-obs.alerts.providers.email.to must have at least one recipient when Email is enabled");
            } else {
                for (String recipient : email.getTo()) {
                    if (recipient == null || !EMAIL_PATTERN.matcher(recipient).matches()) {
                        errors.add("j-obs.alerts.providers.email.to contains invalid email: " + recipient);
                    }
                }
            }

            if (email.isSsl() && email.isStartTls()) {
                errors.add("j-obs.alerts.providers.email: ssl and startTls are mutually exclusive, using ssl");
                email.setStartTls(false);
            }
        }
    }

    private void validateWebhook(Webhook webhook, List<String> errors) {
        if (webhook.isEnabled()) {
            String url = webhook.getUrl();
            if (url == null || url.isBlank()) {
                errors.add("j-obs.alerts.providers.webhook.url is required when Webhook is enabled");
            } else if (!URL_PATTERN.matcher(url).matches()) {
                errors.add("j-obs.alerts.providers.webhook.url must be a valid HTTP/HTTPS URL");
            }

            String method = webhook.getMethod();
            if (method == null || !VALID_HTTP_METHODS.contains(method.toUpperCase())) {
                errors.add("j-obs.alerts.providers.webhook.method must be one of: " + VALID_HTTP_METHODS + ", using 'POST'");
                webhook.setMethod("POST");
            }
        }
    }
}
