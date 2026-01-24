package io.github.jobs.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for J-Obs observability dashboard.
 * <p>
 * All properties are prefixed with {@code j-obs} in application.yml or application.properties.
 * <p>
 * Example configuration:
 * <pre>{@code
 * j-obs:
 *   enabled: true
 *   path: /j-obs
 *   logs:
 *     enabled: true
 *     max-entries: 10000
 *   traces:
 *     enabled: true
 *     sample-rate: 1.0
 *   security:
 *     enabled: true
 *     type: basic
 *     users:
 *       - username: admin
 *         password: ${J_OBS_PASSWORD}
 * }</pre>
 * <p>
 * Main configuration sections:
 * <ul>
 *   <li>{@link Security} - Authentication and authorization settings</li>
 *   <li>{@link Logs} - Log collection and streaming configuration</li>
 *   <li>{@link Traces} - Distributed tracing settings</li>
 *   <li>{@link Metrics} - Metrics collection configuration</li>
 *   <li>{@link Alerts} - Alert notification providers and rules</li>
 *   <li>{@link Dashboard} - UI preferences and refresh rates</li>
 * </ul>
 *
 * @see JObsAutoConfiguration
 */
@ConfigurationProperties(prefix = "j-obs")
public class JObsProperties {

    /**
     * Enable or disable J-Obs entirely.
     */
    private boolean enabled = true;

    /**
     * Base path for J-Obs endpoints.
     */
    private String path = "/j-obs";

    /**
     * Cache duration for dependency check results.
     */
    private Duration checkInterval = Duration.ofMinutes(5);

    /**
     * Security configuration.
     */
    private Security security = new Security();

    /**
     * Rate limiting configuration.
     */
    private RateLimiting rateLimiting = new RateLimiting();

    /**
     * Dashboard configuration.
     */
    private Dashboard dashboard = new Dashboard();

    /**
     * Traces configuration.
     */
    private Traces traces = new Traces();

    /**
     * Logs configuration.
     */
    private Logs logs = new Logs();

    /**
     * Metrics configuration.
     */
    private Metrics metrics = new Metrics();

    /**
     * Health configuration.
     */
    private Health health = new Health();

    /**
     * Instrumentation configuration.
     */
    private Instrumentation instrumentation = new Instrumentation();

    /**
     * Alerts configuration.
     */
    private Alerts alerts = new Alerts();

    /**
     * SQL Analyzer configuration.
     */
    private SqlAnalyzer sqlAnalyzer = new SqlAnalyzer();

    /**
     * Anomaly Detection configuration.
     */
    private AnomalyDetection anomalyDetection = new AnomalyDetection();

    /**
     * Service Map configuration.
     */
    private ServiceMap serviceMap = new ServiceMap();

    /**
     * SLO/SLI configuration.
     */
    private Slo slo = new Slo();

    /**
     * Profiling configuration.
     */
    private Profiling profiling = new Profiling();

    /**
     * Observability compatibility configuration.
     * Controls how J-Obs integrates with Spring Boot Actuator's observability features.
     */
    private Observability observability = new Observability();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Duration getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Duration checkInterval) {
        this.checkInterval = checkInterval;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Traces getTraces() {
        return traces;
    }

    public void setTraces(Traces traces) {
        this.traces = traces;
    }

    public Logs getLogs() {
        return logs;
    }

    public void setLogs(Logs logs) {
        this.logs = logs;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public Alerts getAlerts() {
        return alerts;
    }

    public void setAlerts(Alerts alerts) {
        this.alerts = alerts;
    }

    public SqlAnalyzer getSqlAnalyzer() {
        return sqlAnalyzer;
    }

    public void setSqlAnalyzer(SqlAnalyzer sqlAnalyzer) {
        this.sqlAnalyzer = sqlAnalyzer;
    }

    public AnomalyDetection getAnomalyDetection() {
        return anomalyDetection;
    }

    public void setAnomalyDetection(AnomalyDetection anomalyDetection) {
        this.anomalyDetection = anomalyDetection;
    }

    public ServiceMap getServiceMap() {
        return serviceMap;
    }

    public void setServiceMap(ServiceMap serviceMap) {
        this.serviceMap = serviceMap;
    }

    public Slo getSlo() {
        return slo;
    }

    public void setSlo(Slo slo) {
        this.slo = slo;
    }

    public Profiling getProfiling() {
        return profiling;
    }

    public void setProfiling(Profiling profiling) {
        this.profiling = profiling;
    }

    public Observability getObservability() {
        return observability;
    }

    public void setObservability(Observability observability) {
        this.observability = observability;
    }

    /**
     * Observability compatibility settings for integration with Spring Boot Actuator.
     * <p>
     * These settings help resolve conflicts between J-Obs and Spring Boot's built-in
     * observability features (Micrometer Tracing, Observation API).
     */
    public static class Observability {

        /**
         * Use NOOP ObservationRegistry for @Scheduled tasks.
         * <p>
         * When enabled (default), scheduled tasks will use ObservationRegistry.NOOP,
         * preventing TracingContext errors that occur when Spring Boot Actuator's
         * tracing is disabled or conflicts with J-Obs.
         * <p>
         * Set to false if you want scheduled tasks to be observed by Spring Boot Actuator.
         */
        private boolean scheduledTasksNoop = true;

        /**
         * Filter out conflicting observations.
         * <p>
         * When enabled, an ObservationPredicate is registered to filter out
         * observations that may conflict with J-Obs tracing.
         */
        private boolean filterConflicting = false;

        public boolean isScheduledTasksNoop() {
            return scheduledTasksNoop;
        }

        public void setScheduledTasksNoop(boolean scheduledTasksNoop) {
            this.scheduledTasksNoop = scheduledTasksNoop;
        }

        public boolean isFilterConflicting() {
            return filterConflicting;
        }

        public void setFilterConflicting(boolean filterConflicting) {
            this.filterConflicting = filterConflicting;
        }
    }

    /**
     * Security settings for J-Obs endpoints.
     */
    public static class Security {

        /**
         * Enable security for J-Obs endpoints.
         */
        private boolean enabled = false;

        /**
         * Authentication type: "basic", "api-key", or "both".
         */
        private String type = "basic";

        /**
         * List of users for basic authentication.
         */
        private List<User> users = new ArrayList<>();

        /**
         * List of valid API keys for API key authentication.
         */
        private List<String> apiKeys = new ArrayList<>();

        /**
         * Header name for API key authentication.
         */
        private String apiKeyHeader = "X-API-Key";

        /**
         * Session timeout for authenticated users.
         */
        private Duration sessionTimeout = Duration.ofHours(8);

        /**
         * Exempt paths from authentication (relative to j-obs path).
         * By default, static assets are exempt.
         */
        private List<String> exemptPaths = new ArrayList<>(List.of("/static/**"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }

        public List<String> getApiKeys() {
            return apiKeys;
        }

        public void setApiKeys(List<String> apiKeys) {
            this.apiKeys = apiKeys;
        }

        public String getApiKeyHeader() {
            return apiKeyHeader;
        }

        public void setApiKeyHeader(String apiKeyHeader) {
            this.apiKeyHeader = apiKeyHeader;
        }

        public Duration getSessionTimeout() {
            return sessionTimeout;
        }

        public void setSessionTimeout(Duration sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        public List<String> getExemptPaths() {
            return exemptPaths;
        }

        public void setExemptPaths(List<String> exemptPaths) {
            this.exemptPaths = exemptPaths;
        }

        /**
         * Checks if the security configuration is valid.
         */
        public boolean isConfigured() {
            if (!enabled) return true;

            String authType = type.toLowerCase();
            boolean hasUsers = users != null && !users.isEmpty();
            boolean hasApiKeys = apiKeys != null && !apiKeys.isEmpty();

            return switch (authType) {
                case "basic" -> hasUsers;
                case "api-key" -> hasApiKeys;
                case "both" -> hasUsers || hasApiKeys;
                default -> false;
            };
        }

        /**
         * User configuration for basic authentication.
         */
        public static class User {

            private String username;
            private String password;
            private String role = "USER";

            public User() {
            }

            public User(String username, String password) {
                this.username = username;
                this.password = password;
            }

            public User(String username, String password, String role) {
                this.username = username;
                this.password = password;
                this.role = role;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }
        }
    }

    /**
     * Rate limiting settings for API endpoints.
     */
    public static class RateLimiting {

        /**
         * Enable rate limiting for API endpoints.
         */
        private boolean enabled = true;

        /**
         * Maximum requests per window.
         * Default is 600 (10 req/sec) to support dashboard auto-refresh with multiple widgets.
         */
        private int maxRequests = 600;

        /**
         * Time window for rate limiting.
         */
        private Duration window = Duration.ofMinutes(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    /**
     * Dashboard UI settings.
     */
    public static class Dashboard {

        /**
         * Refresh interval for real-time data.
         */
        private Duration refreshInterval = Duration.ofSeconds(5);

        /**
         * Theme preference: "system", "light", or "dark".
         */
        private String theme = "system";

        public Duration getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }
    }

    /**
     * Traces configuration.
     */
    public static class Traces {

        /**
         * Enable or disable trace collection.
         */
        private boolean enabled = true;

        /**
         * Maximum number of traces to keep in memory.
         */
        private int maxTraces = 10000;

        /**
         * How long to retain traces.
         */
        private Duration retention = Duration.ofHours(1);

        /**
         * Sample rate for traces (1.0 = 100%).
         */
        private double sampleRate = 1.0;

        /**
         * Export configuration for external systems.
         */
        private Export export = new Export();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxTraces() {
            return maxTraces;
        }

        public void setMaxTraces(int maxTraces) {
            this.maxTraces = maxTraces;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }

        public double getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(double sampleRate) {
            this.sampleRate = sampleRate;
        }

        public Export getExport() {
            return export;
        }

        public void setExport(Export export) {
            this.export = export;
        }

        /**
         * Export configuration for external tracing systems.
         */
        public static class Export {

            /**
             * OTLP exporter configuration (supports Tempo, generic OTLP collectors).
             */
            private Otlp otlp = new Otlp();

            /**
             * Zipkin exporter configuration.
             */
            private Zipkin zipkin = new Zipkin();

            /**
             * Jaeger exporter configuration.
             */
            private Jaeger jaeger = new Jaeger();

            public Otlp getOtlp() {
                return otlp;
            }

            public void setOtlp(Otlp otlp) {
                this.otlp = otlp;
            }

            public Zipkin getZipkin() {
                return zipkin;
            }

            public void setZipkin(Zipkin zipkin) {
                this.zipkin = zipkin;
            }

            public Jaeger getJaeger() {
                return jaeger;
            }

            public void setJaeger(Jaeger jaeger) {
                this.jaeger = jaeger;
            }

            /**
             * OTLP exporter configuration.
             */
            public static class Otlp {
                private boolean enabled = false;
                private String endpoint = "http://localhost:4317";
                private Duration timeout = Duration.ofSeconds(10);
                private Map<String, String> headers = new HashMap<>();

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getEndpoint() {
                    return endpoint;
                }

                public void setEndpoint(String endpoint) {
                    this.endpoint = endpoint;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }

                public Map<String, String> getHeaders() {
                    return headers;
                }

                public void setHeaders(Map<String, String> headers) {
                    this.headers = headers;
                }
            }

            /**
             * Zipkin exporter configuration.
             */
            public static class Zipkin {
                private boolean enabled = false;
                private String endpoint = "http://localhost:9411/api/v2/spans";
                private Duration timeout = Duration.ofSeconds(10);

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getEndpoint() {
                    return endpoint;
                }

                public void setEndpoint(String endpoint) {
                    this.endpoint = endpoint;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }
            }

            /**
             * Jaeger exporter configuration.
             */
            public static class Jaeger {
                private boolean enabled = false;
                private String endpoint = "http://localhost:14250";
                private Duration timeout = Duration.ofSeconds(10);

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public String getEndpoint() {
                    return endpoint;
                }

                public void setEndpoint(String endpoint) {
                    this.endpoint = endpoint;
                }

                public Duration getTimeout() {
                    return timeout;
                }

                public void setTimeout(Duration timeout) {
                    this.timeout = timeout;
                }
            }
        }
    }

    /**
     * Logs configuration.
     */
    public static class Logs {

        /**
         * Enable or disable log collection.
         */
        private boolean enabled = true;

        /**
         * Maximum number of log entries to keep in memory.
         */
        private int maxEntries = 10000;

        /**
         * Minimum log level to capture.
         */
        private String minLevel = "INFO";

        /**
         * WebSocket configuration for log streaming.
         */
        private WebSocket websocket = new WebSocket();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }

        public String getMinLevel() {
            return minLevel;
        }

        public void setMinLevel(String minLevel) {
            this.minLevel = minLevel;
        }

        public WebSocket getWebsocket() {
            return websocket;
        }

        public void setWebsocket(WebSocket websocket) {
            this.websocket = websocket;
        }

        /**
         * WebSocket configuration for log streaming.
         */
        public static class WebSocket {

            /**
             * Enable per-message compression for WebSocket connections.
             * Requires container support (Tomcat 8.5+, Jetty 9.4+).
             */
            private boolean compressionEnabled = true;

            /**
             * Maximum size of a single WebSocket text message in bytes.
             */
            private int maxTextMessageSize = 65536;

            /**
             * Buffer size in bytes for outgoing messages.
             */
            private int sendBufferSize = 16384;

            public boolean isCompressionEnabled() {
                return compressionEnabled;
            }

            public void setCompressionEnabled(boolean compressionEnabled) {
                this.compressionEnabled = compressionEnabled;
            }

            public int getMaxTextMessageSize() {
                return maxTextMessageSize;
            }

            public void setMaxTextMessageSize(int maxTextMessageSize) {
                this.maxTextMessageSize = maxTextMessageSize;
            }

            public int getSendBufferSize() {
                return sendBufferSize;
            }

            public void setSendBufferSize(int sendBufferSize) {
                this.sendBufferSize = sendBufferSize;
            }
        }
    }

    /**
     * Metrics configuration.
     */
    public static class Metrics {

        /**
         * Enable or disable metrics collection.
         */
        private boolean enabled = true;

        /**
         * Refresh interval for metrics collection in milliseconds.
         */
        private long refreshInterval = 10000;

        /**
         * Maximum number of history data points to keep per metric.
         */
        private int maxHistoryPoints = 360;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getRefreshInterval() {
            return refreshInterval;
        }

        public void setRefreshInterval(long refreshInterval) {
            this.refreshInterval = refreshInterval;
        }

        public int getMaxHistoryPoints() {
            return maxHistoryPoints;
        }

        public void setMaxHistoryPoints(int maxHistoryPoints) {
            this.maxHistoryPoints = maxHistoryPoints;
        }
    }

    /**
     * Health configuration.
     */
    public static class Health {

        /**
         * Enable or disable health check integration.
         */
        private boolean enabled = true;

        /**
         * Maximum number of history entries to keep per component.
         */
        private int maxHistoryEntries = 100;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxHistoryEntries() {
            return maxHistoryEntries;
        }

        public void setMaxHistoryEntries(int maxHistoryEntries) {
            this.maxHistoryEntries = maxHistoryEntries;
        }
    }

    /**
     * Automatic instrumentation configuration.
     */
    public static class Instrumentation {

        /**
         * Enable @Traced annotation support.
         */
        private boolean tracedAnnotation = true;

        /**
         * Enable @Measured annotation support.
         */
        private boolean measuredAnnotation = true;

        /**
         * Enable automatic HTTP request tracing.
         */
        private boolean httpTracing = true;

        /**
         * Enable automatic instrumentation of all @Service, @Repository, @Component classes.
         * This is disabled by default to avoid overhead. When enabled, all public methods
         * in Spring components will be automatically traced and measured.
         */
        private boolean autoInstrument = false;

        public boolean isTracedAnnotation() {
            return tracedAnnotation;
        }

        public void setTracedAnnotation(boolean tracedAnnotation) {
            this.tracedAnnotation = tracedAnnotation;
        }

        public boolean isMeasuredAnnotation() {
            return measuredAnnotation;
        }

        public void setMeasuredAnnotation(boolean measuredAnnotation) {
            this.measuredAnnotation = measuredAnnotation;
        }

        public boolean isHttpTracing() {
            return httpTracing;
        }

        public void setHttpTracing(boolean httpTracing) {
            this.httpTracing = httpTracing;
        }

        public boolean isAutoInstrument() {
            return autoInstrument;
        }

        public void setAutoInstrument(boolean autoInstrument) {
            this.autoInstrument = autoInstrument;
        }
    }

    /**
     * Alert notification configuration.
     */
    public static class Alerts {

        /**
         * Enable or disable alert notifications.
         */
        private boolean enabled = true;

        /**
         * Interval for evaluating alert conditions.
         */
        private Duration evaluationInterval = Duration.ofSeconds(15);

        /**
         * Maximum number of alert events to keep in memory.
         */
        private int maxEvents = 10000;

        /**
         * Retention period for alert events.
         */
        private Duration retention = Duration.ofDays(7);

        /**
         * HTTP client timeout for sending notifications to providers.
         */
        private Duration httpTimeout = Duration.ofSeconds(30);

        /**
         * Throttling configuration.
         */
        private Throttling throttling = new Throttling();

        /**
         * Provider configurations.
         */
        private Providers providers = new Providers();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getEvaluationInterval() {
            return evaluationInterval;
        }

        public void setEvaluationInterval(Duration evaluationInterval) {
            this.evaluationInterval = evaluationInterval;
        }

        public int getMaxEvents() {
            return maxEvents;
        }

        public void setMaxEvents(int maxEvents) {
            this.maxEvents = maxEvents;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }

        public Duration getHttpTimeout() {
            return httpTimeout;
        }

        public void setHttpTimeout(Duration httpTimeout) {
            this.httpTimeout = httpTimeout;
        }

        public Throttling getThrottling() {
            return throttling;
        }

        public void setThrottling(Throttling throttling) {
            this.throttling = throttling;
        }

        public Providers getProviders() {
            return providers;
        }

        public void setProviders(Providers providers) {
            this.providers = providers;
        }

        /**
         * Throttling configuration for alerts.
         */
        public static class Throttling {

            /**
             * Maximum number of alerts per rate period.
             */
            private int rateLimit = 10;

            /**
             * Rate limit period.
             */
            private Duration ratePeriod = Duration.ofMinutes(1);

            /**
             * Cooldown period between same alerts.
             */
            private Duration cooldown = Duration.ofMinutes(5);

            /**
             * Enable grouping of similar alerts.
             */
            private boolean grouping = true;

            /**
             * Time to wait for grouping alerts.
             */
            private Duration groupWait = Duration.ofSeconds(30);

            /**
             * Maximum number of alerts in a single group.
             */
            private int maxGroupSize = 100;

            /**
             * Labels to use for grouping alerts.
             * Alerts with the same values for these labels will be grouped together.
             */
            private List<String> groupByLabels = List.of("service", "instance");

            /**
             * Interval for repeating unresolved alerts.
             */
            private Duration repeatInterval = Duration.ofHours(4);

            public int getRateLimit() {
                return rateLimit;
            }

            public void setRateLimit(int rateLimit) {
                this.rateLimit = rateLimit;
            }

            public Duration getRatePeriod() {
                return ratePeriod;
            }

            public void setRatePeriod(Duration ratePeriod) {
                this.ratePeriod = ratePeriod;
            }

            public Duration getCooldown() {
                return cooldown;
            }

            public void setCooldown(Duration cooldown) {
                this.cooldown = cooldown;
            }

            public boolean isGrouping() {
                return grouping;
            }

            public void setGrouping(boolean grouping) {
                this.grouping = grouping;
            }

            public Duration getGroupWait() {
                return groupWait;
            }

            public void setGroupWait(Duration groupWait) {
                this.groupWait = groupWait;
            }

            public int getMaxGroupSize() {
                return maxGroupSize;
            }

            public void setMaxGroupSize(int maxGroupSize) {
                this.maxGroupSize = maxGroupSize;
            }

            public List<String> getGroupByLabels() {
                return groupByLabels;
            }

            public void setGroupByLabels(List<String> groupByLabels) {
                this.groupByLabels = groupByLabels;
            }

            public Duration getRepeatInterval() {
                return repeatInterval;
            }

            public void setRepeatInterval(Duration repeatInterval) {
                this.repeatInterval = repeatInterval;
            }
        }

        /**
         * Alert notification providers configuration.
         */
        public static class Providers {

            private Telegram telegram = new Telegram();
            private Teams teams = new Teams();
            private Slack slack = new Slack();
            private Email email = new Email();
            private Webhook webhook = new Webhook();

            public Telegram getTelegram() {
                return telegram;
            }

            public void setTelegram(Telegram telegram) {
                this.telegram = telegram;
            }

            public Teams getTeams() {
                return teams;
            }

            public void setTeams(Teams teams) {
                this.teams = teams;
            }

            public Slack getSlack() {
                return slack;
            }

            public void setSlack(Slack slack) {
                this.slack = slack;
            }

            public Email getEmail() {
                return email;
            }

            public void setEmail(Email email) {
                this.email = email;
            }

            public Webhook getWebhook() {
                return webhook;
            }

            public void setWebhook(Webhook webhook) {
                this.webhook = webhook;
            }
        }

        /**
         * Telegram provider configuration.
         */
        public static class Telegram {

            private boolean enabled = false;
            private String botToken;
            private List<String> chatIds = new ArrayList<>();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getBotToken() {
                return botToken;
            }

            public void setBotToken(String botToken) {
                this.botToken = botToken;
            }

            public List<String> getChatIds() {
                return chatIds;
            }

            public void setChatIds(List<String> chatIds) {
                this.chatIds = chatIds;
            }

            public boolean isConfigured() {
                return enabled && botToken != null && !botToken.isBlank() && !chatIds.isEmpty();
            }
        }

        /**
         * Microsoft Teams provider configuration.
         */
        public static class Teams {

            private boolean enabled = false;
            private String webhookUrl;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getWebhookUrl() {
                return webhookUrl;
            }

            public void setWebhookUrl(String webhookUrl) {
                this.webhookUrl = webhookUrl;
            }

            public boolean isConfigured() {
                return enabled && webhookUrl != null && !webhookUrl.isBlank();
            }
        }

        /**
         * Slack provider configuration.
         */
        public static class Slack {

            private boolean enabled = false;
            private String webhookUrl;
            private String channel;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getWebhookUrl() {
                return webhookUrl;
            }

            public void setWebhookUrl(String webhookUrl) {
                this.webhookUrl = webhookUrl;
            }

            public String getChannel() {
                return channel;
            }

            public void setChannel(String channel) {
                this.channel = channel;
            }

            public boolean isConfigured() {
                return enabled && webhookUrl != null && !webhookUrl.isBlank();
            }
        }

        /**
         * Email provider configuration.
         */
        public static class Email {

            private boolean enabled = false;
            private String host;
            private int port = 587;
            private String username;
            private String password;
            private String from;
            private List<String> to = new ArrayList<>();
            private boolean startTls = true;
            private boolean ssl = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public int getPort() {
                return port;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getFrom() {
                return from;
            }

            public void setFrom(String from) {
                this.from = from;
            }

            public List<String> getTo() {
                return to;
            }

            public void setTo(List<String> to) {
                this.to = to;
            }

            public boolean isStartTls() {
                return startTls;
            }

            public void setStartTls(boolean startTls) {
                this.startTls = startTls;
            }

            public boolean isSsl() {
                return ssl;
            }

            public void setSsl(boolean ssl) {
                this.ssl = ssl;
            }

            public boolean isConfigured() {
                return enabled && host != null && !host.isBlank() && from != null && !to.isEmpty();
            }
        }

        /**
         * Generic webhook provider configuration.
         */
        public static class Webhook {

            private boolean enabled = false;
            private String url;
            private Map<String, String> headers = new HashMap<>();
            private String template;
            private String method = "POST";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public Map<String, String> getHeaders() {
                return headers;
            }

            public void setHeaders(Map<String, String> headers) {
                this.headers = headers;
            }

            public String getTemplate() {
                return template;
            }

            public void setTemplate(String template) {
                this.template = template;
            }

            public String getMethod() {
                return method;
            }

            public void setMethod(String method) {
                this.method = method;
            }

            public boolean isConfigured() {
                return enabled && url != null && !url.isBlank();
            }
        }
    }

    /**
     * SQL Analyzer configuration.
     */
    public static class SqlAnalyzer {

        /**
         * Enable or disable SQL analysis.
         */
        private boolean enabled = true;

        /**
         * Threshold for slow queries.
         */
        private Duration slowQueryThreshold = Duration.ofSeconds(1);

        /**
         * Threshold for very slow queries (critical).
         */
        private Duration verySlowQueryThreshold = Duration.ofSeconds(5);

        /**
         * Minimum number of similar queries to detect N+1.
         */
        private int nPlusOneMinQueries = 5;

        /**
         * Similarity threshold for N+1 detection (0.0 to 1.0).
         */
        private double nPlusOneSimilarity = 0.9;

        /**
         * Threshold for large result sets.
         */
        private int largeResultSetThreshold = 1000;

        /**
         * Detect SELECT * queries.
         */
        private boolean detectSelectStar = true;

        /**
         * Detect SELECT without LIMIT.
         */
        private boolean detectMissingLimit = true;

        /**
         * Time window for analysis.
         */
        private Duration analysisWindow = Duration.ofHours(1);

        /**
         * Patterns to ignore (regex).
         */
        private List<String> ignorePatterns = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getSlowQueryThreshold() {
            return slowQueryThreshold;
        }

        public void setSlowQueryThreshold(Duration slowQueryThreshold) {
            this.slowQueryThreshold = slowQueryThreshold;
        }

        public Duration getVerySlowQueryThreshold() {
            return verySlowQueryThreshold;
        }

        public void setVerySlowQueryThreshold(Duration verySlowQueryThreshold) {
            this.verySlowQueryThreshold = verySlowQueryThreshold;
        }

        public int getNPlusOneMinQueries() {
            return nPlusOneMinQueries;
        }

        public void setNPlusOneMinQueries(int nPlusOneMinQueries) {
            this.nPlusOneMinQueries = nPlusOneMinQueries;
        }

        public double getNPlusOneSimilarity() {
            return nPlusOneSimilarity;
        }

        public void setNPlusOneSimilarity(double nPlusOneSimilarity) {
            this.nPlusOneSimilarity = nPlusOneSimilarity;
        }

        public int getLargeResultSetThreshold() {
            return largeResultSetThreshold;
        }

        public void setLargeResultSetThreshold(int largeResultSetThreshold) {
            this.largeResultSetThreshold = largeResultSetThreshold;
        }

        public boolean isDetectSelectStar() {
            return detectSelectStar;
        }

        public void setDetectSelectStar(boolean detectSelectStar) {
            this.detectSelectStar = detectSelectStar;
        }

        public boolean isDetectMissingLimit() {
            return detectMissingLimit;
        }

        public void setDetectMissingLimit(boolean detectMissingLimit) {
            this.detectMissingLimit = detectMissingLimit;
        }

        public Duration getAnalysisWindow() {
            return analysisWindow;
        }

        public void setAnalysisWindow(Duration analysisWindow) {
            this.analysisWindow = analysisWindow;
        }

        public List<String> getIgnorePatterns() {
            return ignorePatterns;
        }

        public void setIgnorePatterns(List<String> ignorePatterns) {
            this.ignorePatterns = ignorePatterns;
        }
    }

    /**
     * Anomaly Detection configuration.
     */
    public static class AnomalyDetection {

        /**
         * Enable or disable anomaly detection.
         */
        private boolean enabled = true;

        /**
         * Interval for running anomaly detection.
         */
        private Duration detectionInterval = Duration.ofMinutes(1);

        /**
         * Window for baseline calculation.
         */
        private Duration baselineWindow = Duration.ofDays(7);

        /**
         * Minimum samples required for baseline.
         */
        private int minSamplesForBaseline = 100;

        /**
         * Z-score threshold for latency anomalies.
         */
        private double latencyZScoreThreshold = 3.0;

        /**
         * Minimum percentage increase for latency anomaly.
         */
        private double latencyMinIncreasePercent = 100.0;

        /**
         * Z-score threshold for error rate anomalies.
         */
        private double errorRateZScoreThreshold = 2.5;

        /**
         * Minimum absolute error rate to trigger anomaly.
         */
        private double errorRateMinAbsolute = 1.0;

        /**
         * Z-score threshold for traffic anomalies.
         */
        private double trafficZScoreThreshold = 3.0;

        /**
         * Alert on traffic decrease.
         */
        private boolean alertOnTrafficDecrease = true;

        /**
         * Retention period for resolved anomalies.
         */
        private Duration retentionPeriod = Duration.ofDays(7);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDetectionInterval() {
            return detectionInterval;
        }

        public void setDetectionInterval(Duration detectionInterval) {
            this.detectionInterval = detectionInterval;
        }

        public Duration getBaselineWindow() {
            return baselineWindow;
        }

        public void setBaselineWindow(Duration baselineWindow) {
            this.baselineWindow = baselineWindow;
        }

        public int getMinSamplesForBaseline() {
            return minSamplesForBaseline;
        }

        public void setMinSamplesForBaseline(int minSamplesForBaseline) {
            this.minSamplesForBaseline = minSamplesForBaseline;
        }

        public double getLatencyZScoreThreshold() {
            return latencyZScoreThreshold;
        }

        public void setLatencyZScoreThreshold(double latencyZScoreThreshold) {
            this.latencyZScoreThreshold = latencyZScoreThreshold;
        }

        public double getLatencyMinIncreasePercent() {
            return latencyMinIncreasePercent;
        }

        public void setLatencyMinIncreasePercent(double latencyMinIncreasePercent) {
            this.latencyMinIncreasePercent = latencyMinIncreasePercent;
        }

        public double getErrorRateZScoreThreshold() {
            return errorRateZScoreThreshold;
        }

        public void setErrorRateZScoreThreshold(double errorRateZScoreThreshold) {
            this.errorRateZScoreThreshold = errorRateZScoreThreshold;
        }

        public double getErrorRateMinAbsolute() {
            return errorRateMinAbsolute;
        }

        public void setErrorRateMinAbsolute(double errorRateMinAbsolute) {
            this.errorRateMinAbsolute = errorRateMinAbsolute;
        }

        public double getTrafficZScoreThreshold() {
            return trafficZScoreThreshold;
        }

        public void setTrafficZScoreThreshold(double trafficZScoreThreshold) {
            this.trafficZScoreThreshold = trafficZScoreThreshold;
        }

        public boolean isAlertOnTrafficDecrease() {
            return alertOnTrafficDecrease;
        }

        public void setAlertOnTrafficDecrease(boolean alertOnTrafficDecrease) {
            this.alertOnTrafficDecrease = alertOnTrafficDecrease;
        }

        public Duration getRetentionPeriod() {
            return retentionPeriod;
        }

        public void setRetentionPeriod(Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }
    }

    /**
     * Service Map configuration.
     */
    public static class ServiceMap {

        /**
         * Default time window for building the service map.
         */
        private Duration defaultWindow = Duration.ofHours(1);

        /**
         * Cache expiration for the service map.
         */
        private Duration cacheExpiration = Duration.ofMinutes(1);

        /**
         * Error rate threshold for unhealthy status.
         */
        private double errorThreshold = 5.0;

        /**
         * Latency threshold in ms for degraded status.
         */
        private double latencyThreshold = 1000;

        /**
         * Minimum requests required for health calculation.
         */
        private int minRequestsForHealth = 10;

        public Duration getDefaultWindow() {
            return defaultWindow;
        }

        public void setDefaultWindow(Duration defaultWindow) {
            this.defaultWindow = defaultWindow;
        }

        public Duration getCacheExpiration() {
            return cacheExpiration;
        }

        public void setCacheExpiration(Duration cacheExpiration) {
            this.cacheExpiration = cacheExpiration;
        }

        public double getErrorThreshold() {
            return errorThreshold;
        }

        public void setErrorThreshold(double errorThreshold) {
            this.errorThreshold = errorThreshold;
        }

        public double getLatencyThreshold() {
            return latencyThreshold;
        }

        public void setLatencyThreshold(double latencyThreshold) {
            this.latencyThreshold = latencyThreshold;
        }

        public int getMinRequestsForHealth() {
            return minRequestsForHealth;
        }

        public void setMinRequestsForHealth(int minRequestsForHealth) {
            this.minRequestsForHealth = minRequestsForHealth;
        }
    }

    /**
     * SLO/SLI configuration.
     */
    public static class Slo {

        /**
         * Enable or disable SLO tracking.
         */
        private boolean enabled = true;

        /**
         * Interval for evaluating SLOs.
         */
        private Duration evaluationInterval = Duration.ofMinutes(1);

        /**
         * Maximum evaluation history entries per SLO.
         */
        private int maxEvaluationHistory = 100;

        /**
         * Default SLO window.
         */
        private Duration defaultWindow = Duration.ofDays(30);

        /**
         * Error budget warning threshold (percentage).
         */
        private double errorBudgetWarningThreshold = 25.0;

        /**
         * Burn rate threshold for short window alerts.
         */
        private double shortWindowBurnRate = 14.4;

        /**
         * Short window duration for burn rate calculation.
         */
        private Duration shortWindowDuration = Duration.ofHours(1);

        /**
         * Burn rate threshold for long window alerts.
         */
        private double longWindowBurnRate = 6.0;

        /**
         * Long window duration for burn rate calculation.
         */
        private Duration longWindowDuration = Duration.ofHours(6);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getEvaluationInterval() {
            return evaluationInterval;
        }

        public void setEvaluationInterval(Duration evaluationInterval) {
            this.evaluationInterval = evaluationInterval;
        }

        public int getMaxEvaluationHistory() {
            return maxEvaluationHistory;
        }

        public void setMaxEvaluationHistory(int maxEvaluationHistory) {
            this.maxEvaluationHistory = maxEvaluationHistory;
        }

        public Duration getDefaultWindow() {
            return defaultWindow;
        }

        public void setDefaultWindow(Duration defaultWindow) {
            this.defaultWindow = defaultWindow;
        }

        public double getErrorBudgetWarningThreshold() {
            return errorBudgetWarningThreshold;
        }

        public void setErrorBudgetWarningThreshold(double errorBudgetWarningThreshold) {
            this.errorBudgetWarningThreshold = errorBudgetWarningThreshold;
        }

        public double getShortWindowBurnRate() {
            return shortWindowBurnRate;
        }

        public void setShortWindowBurnRate(double shortWindowBurnRate) {
            this.shortWindowBurnRate = shortWindowBurnRate;
        }

        public Duration getShortWindowDuration() {
            return shortWindowDuration;
        }

        public void setShortWindowDuration(Duration shortWindowDuration) {
            this.shortWindowDuration = shortWindowDuration;
        }

        public double getLongWindowBurnRate() {
            return longWindowBurnRate;
        }

        public void setLongWindowBurnRate(double longWindowBurnRate) {
            this.longWindowBurnRate = longWindowBurnRate;
        }

        public Duration getLongWindowDuration() {
            return longWindowDuration;
        }

        public void setLongWindowDuration(Duration longWindowDuration) {
            this.longWindowDuration = longWindowDuration;
        }
    }

    /**
     * Profiling configuration.
     */
    public static class Profiling {

        /**
         * Enable profiling features.
         */
        private boolean enabled = true;

        /**
         * Default duration for CPU profiling.
         */
        private Duration defaultDuration = Duration.ofSeconds(60);

        /**
         * Default sampling interval for CPU profiling.
         */
        private Duration defaultSamplingInterval = Duration.ofMillis(10);

        /**
         * Maximum number of profiling sessions to keep in history.
         */
        private int maxSessions = 100;

        /**
         * Maximum duration allowed for CPU profiling.
         */
        private Duration maxDuration = Duration.ofMinutes(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDefaultDuration() {
            return defaultDuration;
        }

        public void setDefaultDuration(Duration defaultDuration) {
            this.defaultDuration = defaultDuration;
        }

        public Duration getDefaultSamplingInterval() {
            return defaultSamplingInterval;
        }

        public void setDefaultSamplingInterval(Duration defaultSamplingInterval) {
            this.defaultSamplingInterval = defaultSamplingInterval;
        }

        public int getMaxSessions() {
            return maxSessions;
        }

        public void setMaxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
        }

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }
    }
}
