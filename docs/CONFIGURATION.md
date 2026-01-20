# J-Obs Configuration Reference

This document provides a comprehensive reference for all J-Obs configuration properties.

## Table of Contents

- [Basic Configuration](#basic-configuration)
- [Security](#security)
- [Rate Limiting](#rate-limiting)
- [Dashboard](#dashboard)
- [Traces](#traces)
- [Logs](#logs)
- [Metrics](#metrics)
- [Health](#health)
- [Instrumentation](#instrumentation)
- [Alerts](#alerts)
- [SQL Analyzer](#sql-analyzer)
- [Anomaly Detection](#anomaly-detection)
- [Service Map](#service-map)
- [SLO/SLI](#slosli)
- [Profiling](#profiling)
- [WebFlux Support](#webflux-support)
- [Complete Example](#complete-example)

---

## Basic Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.enabled` | boolean | `true` | Enable or disable J-Obs entirely |
| `j-obs.path` | String | `/j-obs` | Base path for J-Obs endpoints |
| `j-obs.check-interval` | Duration | `5m` | Cache duration for dependency check results |

```yaml
j-obs:
  enabled: true
  path: /j-obs
  check-interval: 5m
```

---

## Security

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.security.enabled` | boolean | `false` | Enable security for J-Obs endpoints |

```yaml
j-obs:
  security:
    enabled: true
```

> **Note:** When security is enabled, integrate with Spring Security for authentication. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for details.

---

## Rate Limiting

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.rate-limiting.enabled` | boolean | `true` | Enable rate limiting for API endpoints |
| `j-obs.rate-limiting.max-requests` | int | `100` | Maximum requests per window |
| `j-obs.rate-limiting.window` | Duration | `1m` | Time window for rate limiting |

```yaml
j-obs:
  rate-limiting:
    enabled: true
    max-requests: 100
    window: 1m
```

---

## Dashboard

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.dashboard.refresh-interval` | Duration | `5s` | Refresh interval for real-time data |
| `j-obs.dashboard.theme` | String | `system` | Theme preference: `system`, `light`, or `dark` |

```yaml
j-obs:
  dashboard:
    refresh-interval: 5s
    theme: system  # Options: system, light, dark
```

---

## Traces

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.traces.enabled` | boolean | `true` | Enable or disable trace collection |
| `j-obs.traces.max-traces` | int | `10000` | Maximum number of traces to keep in memory |
| `j-obs.traces.retention` | Duration | `1h` | How long to retain traces |
| `j-obs.traces.sample-rate` | double | `1.0` | Sample rate for traces (1.0 = 100%) |

```yaml
j-obs:
  traces:
    enabled: true
    max-traces: 10000
    retention: 1h
    sample-rate: 1.0  # 0.5 = 50% sampling
```

### External Exporters

J-Obs can export traces to external observability platforms like Grafana Tempo, Jaeger, or Zipkin.

#### OTLP Exporter (Tempo, Generic Collectors)

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.traces.export.otlp.enabled` | boolean | `false` | Enable OTLP export |
| `j-obs.traces.export.otlp.endpoint` | String | `http://localhost:4317` | OTLP gRPC endpoint |
| `j-obs.traces.export.otlp.timeout` | Duration | `10s` | Export timeout |
| `j-obs.traces.export.otlp.headers` | Map | `{}` | Custom headers (e.g., authentication) |

```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317
        timeout: 10s
        headers:
          Authorization: "Bearer ${TEMPO_TOKEN}"
```

#### Zipkin Exporter

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.traces.export.zipkin.enabled` | boolean | `false` | Enable Zipkin export |
| `j-obs.traces.export.zipkin.endpoint` | String | `http://localhost:9411/api/v2/spans` | Zipkin API endpoint |
| `j-obs.traces.export.zipkin.timeout` | Duration | `10s` | Export timeout |

```yaml
j-obs:
  traces:
    export:
      zipkin:
        enabled: true
        endpoint: http://zipkin:9411/api/v2/spans
        timeout: 10s
```

#### Jaeger Exporter

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.traces.export.jaeger.enabled` | boolean | `false` | Enable Jaeger export |
| `j-obs.traces.export.jaeger.endpoint` | String | `http://localhost:14250` | Jaeger OTLP endpoint |
| `j-obs.traces.export.jaeger.timeout` | Duration | `10s` | Export timeout |

```yaml
j-obs:
  traces:
    export:
      jaeger:
        enabled: true
        endpoint: http://jaeger:4317  # Modern Jaeger uses OTLP on port 4317
        timeout: 10s
```

#### Multiple Exporters

You can enable multiple exporters simultaneously:

```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317
      zipkin:
        enabled: true
        endpoint: http://zipkin:9411/api/v2/spans
```

#### Required Dependencies

Add the appropriate exporter dependency to your `pom.xml`:

```xml
<!-- OTLP (for Tempo, Jaeger, generic OTLP collectors) -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

<!-- Zipkin -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

---

## Logs

### Main Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.logs.enabled` | boolean | `true` | Enable or disable log collection |
| `j-obs.logs.max-entries` | int | `10000` | Maximum number of log entries to keep in memory |
| `j-obs.logs.min-level` | String | `INFO` | Minimum log level to capture |

### WebSocket Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.logs.websocket.compression-enabled` | boolean | `true` | Enable per-message compression |
| `j-obs.logs.websocket.max-text-message-size` | int | `65536` | Maximum WebSocket text message size (bytes) |
| `j-obs.logs.websocket.send-buffer-size` | int | `16384` | Buffer size for outgoing messages (bytes) |

```yaml
j-obs:
  logs:
    enabled: true
    max-entries: 10000
    min-level: INFO  # Options: TRACE, DEBUG, INFO, WARN, ERROR
    websocket:
      compression-enabled: true
      max-text-message-size: 65536
      send-buffer-size: 16384
```

> **Note:** WebSocket is optional. If `spring-boot-starter-websocket` is not in classpath, J-Obs will work without real-time log streaming.

---

## Metrics

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.metrics.enabled` | boolean | `true` | Enable or disable metrics collection |
| `j-obs.metrics.refresh-interval` | long | `10000` | Refresh interval in milliseconds |
| `j-obs.metrics.max-history-points` | int | `360` | Maximum history data points per metric |

```yaml
j-obs:
  metrics:
    enabled: true
    refresh-interval: 10000  # 10 seconds
    max-history-points: 360  # 1 hour at 10s intervals
```

---

## Health

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.health.enabled` | boolean | `true` | Enable or disable health check integration |
| `j-obs.health.max-history-entries` | int | `100` | Maximum history entries per component |

```yaml
j-obs:
  health:
    enabled: true
    max-history-entries: 100
```

---

## Instrumentation

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.instrumentation.traced-annotation` | boolean | `true` | Enable `@Traced` annotation support |
| `j-obs.instrumentation.measured-annotation` | boolean | `true` | Enable `@Measured` annotation support |
| `j-obs.instrumentation.http-tracing` | boolean | `true` | Enable automatic HTTP request tracing |
| `j-obs.instrumentation.auto-instrument` | boolean | `false` | Auto-instrument all Spring components |

```yaml
j-obs:
  instrumentation:
    traced-annotation: true
    measured-annotation: true
    http-tracing: true
    auto-instrument: false  # WARNING: May add overhead
```

> **Warning:** `auto-instrument: true` will trace all public methods in `@Service`, `@Repository`, and `@Component` classes. This can add significant overhead.

---

## Alerts

### Main Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.enabled` | boolean | `true` | Enable alert notifications |
| `j-obs.alerts.evaluation-interval` | Duration | `15s` | Interval for evaluating alert conditions |
| `j-obs.alerts.max-events` | int | `10000` | Maximum alert events in memory |
| `j-obs.alerts.retention` | Duration | `7d` | Retention period for alert events |
| `j-obs.alerts.http-timeout` | Duration | `30s` | HTTP client timeout for notifications |

### Throttling Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.throttling.rate-limit` | int | `10` | Maximum alerts per rate period |
| `j-obs.alerts.throttling.rate-period` | Duration | `1m` | Rate limit period |
| `j-obs.alerts.throttling.cooldown` | Duration | `5m` | Cooldown between same alerts |
| `j-obs.alerts.throttling.grouping` | boolean | `true` | Enable grouping of similar alerts |
| `j-obs.alerts.throttling.group-wait` | Duration | `30s` | Time to wait for grouping |
| `j-obs.alerts.throttling.max-group-size` | int | `100` | Maximum alerts in a single group |
| `j-obs.alerts.throttling.group-by-labels` | List | `[service, instance]` | Labels for grouping |
| `j-obs.alerts.throttling.repeat-interval` | Duration | `4h` | Re-send interval for unresolved alerts |

### Provider: Telegram

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.providers.telegram.enabled` | boolean | `false` | Enable Telegram notifications |
| `j-obs.alerts.providers.telegram.bot-token` | String | - | Telegram Bot API token |
| `j-obs.alerts.providers.telegram.chat-ids` | List | `[]` | List of chat IDs to notify |

### Provider: Microsoft Teams

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.providers.teams.enabled` | boolean | `false` | Enable Teams notifications |
| `j-obs.alerts.providers.teams.webhook-url` | String | - | Teams Incoming Webhook URL |

### Provider: Slack

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.providers.slack.enabled` | boolean | `false` | Enable Slack notifications |
| `j-obs.alerts.providers.slack.webhook-url` | String | - | Slack Webhook URL |
| `j-obs.alerts.providers.slack.channel` | String | - | Optional channel override |

### Provider: Email

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.providers.email.enabled` | boolean | `false` | Enable email notifications |
| `j-obs.alerts.providers.email.host` | String | - | SMTP host |
| `j-obs.alerts.providers.email.port` | int | `587` | SMTP port |
| `j-obs.alerts.providers.email.username` | String | - | SMTP username |
| `j-obs.alerts.providers.email.password` | String | - | SMTP password |
| `j-obs.alerts.providers.email.from` | String | - | From email address |
| `j-obs.alerts.providers.email.to` | List | `[]` | List of recipient emails |
| `j-obs.alerts.providers.email.start-tls` | boolean | `true` | Enable STARTTLS |
| `j-obs.alerts.providers.email.ssl` | boolean | `false` | Enable SSL |

### Provider: Webhook

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.alerts.providers.webhook.enabled` | boolean | `false` | Enable generic webhook |
| `j-obs.alerts.providers.webhook.url` | String | - | Webhook URL |
| `j-obs.alerts.providers.webhook.method` | String | `POST` | HTTP method |
| `j-obs.alerts.providers.webhook.headers` | Map | `{}` | Custom headers |
| `j-obs.alerts.providers.webhook.template` | String | - | JSON template for payload |

```yaml
j-obs:
  alerts:
    enabled: true
    evaluation-interval: 15s
    max-events: 10000
    retention: 7d
    http-timeout: 30s

    throttling:
      rate-limit: 10
      rate-period: 1m
      cooldown: 5m
      grouping: true
      group-wait: 30s
      max-group-size: 100
      group-by-labels:
        - service
        - instance
      repeat-interval: 4h

    providers:
      telegram:
        enabled: true
        bot-token: ${TELEGRAM_BOT_TOKEN}
        chat-ids:
          - "-1001234567890"

      teams:
        enabled: false
        webhook-url: ${TEAMS_WEBHOOK_URL}

      slack:
        enabled: false
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"

      email:
        enabled: false
        host: smtp.gmail.com
        port: 587
        username: ${SMTP_USER}
        password: ${SMTP_PASSWORD}
        from: alerts@myapp.com
        to:
          - team@myapp.com
        start-tls: true
        ssl: false

      webhook:
        enabled: false
        url: https://api.example.com/webhooks/alerts
        method: POST
        headers:
          Authorization: "Bearer ${WEBHOOK_TOKEN}"
          Content-Type: "application/json"
        template: |
          {
            "alert": "{{name}}",
            "severity": "{{severity}}",
            "message": "{{message}}"
          }
```

---

## SQL Analyzer

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.sql-analyzer.enabled` | boolean | `true` | Enable SQL analysis |
| `j-obs.sql-analyzer.slow-query-threshold` | Duration | `1s` | Threshold for slow queries |
| `j-obs.sql-analyzer.very-slow-query-threshold` | Duration | `5s` | Threshold for critical slow queries |
| `j-obs.sql-analyzer.n-plus-one-min-queries` | int | `5` | Minimum similar queries for N+1 detection |
| `j-obs.sql-analyzer.n-plus-one-similarity` | double | `0.9` | Similarity threshold for N+1 (0.0-1.0) |
| `j-obs.sql-analyzer.large-result-set-threshold` | int | `1000` | Threshold for large result sets |
| `j-obs.sql-analyzer.detect-select-star` | boolean | `true` | Detect SELECT * queries |
| `j-obs.sql-analyzer.detect-missing-limit` | boolean | `true` | Detect SELECT without LIMIT |
| `j-obs.sql-analyzer.analysis-window` | Duration | `1h` | Time window for analysis |
| `j-obs.sql-analyzer.ignore-patterns` | List | `[]` | Regex patterns to ignore |

```yaml
j-obs:
  sql-analyzer:
    enabled: true
    slow-query-threshold: 1s
    very-slow-query-threshold: 5s
    n-plus-one-min-queries: 5
    n-plus-one-similarity: 0.9
    large-result-set-threshold: 1000
    detect-select-star: true
    detect-missing-limit: true
    analysis-window: 1h
    ignore-patterns:
      - "SELECT 1"  # Health checks
      - ".*flyway.*"  # Migrations
```

---

## Anomaly Detection

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.anomaly-detection.enabled` | boolean | `true` | Enable anomaly detection |
| `j-obs.anomaly-detection.detection-interval` | Duration | `1m` | Interval for detection runs |
| `j-obs.anomaly-detection.baseline-window` | Duration | `7d` | Window for baseline calculation |
| `j-obs.anomaly-detection.min-samples-for-baseline` | int | `100` | Minimum samples for baseline |
| `j-obs.anomaly-detection.latency-z-score-threshold` | double | `3.0` | Z-score threshold for latency |
| `j-obs.anomaly-detection.latency-min-increase-percent` | double | `100.0` | Minimum % increase for latency alert |
| `j-obs.anomaly-detection.error-rate-z-score-threshold` | double | `2.5` | Z-score threshold for error rate |
| `j-obs.anomaly-detection.error-rate-min-absolute` | double | `1.0` | Minimum absolute error rate |
| `j-obs.anomaly-detection.traffic-z-score-threshold` | double | `3.0` | Z-score threshold for traffic |
| `j-obs.anomaly-detection.alert-on-traffic-decrease` | boolean | `true` | Alert on traffic decrease |
| `j-obs.anomaly-detection.retention-period` | Duration | `7d` | Retention for resolved anomalies |

```yaml
j-obs:
  anomaly-detection:
    enabled: true
    detection-interval: 1m
    baseline-window: 7d
    min-samples-for-baseline: 100
    latency-z-score-threshold: 3.0
    latency-min-increase-percent: 100.0
    error-rate-z-score-threshold: 2.5
    error-rate-min-absolute: 1.0
    traffic-z-score-threshold: 3.0
    alert-on-traffic-decrease: true
    retention-period: 7d
```

---

## Service Map

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.service-map.default-window` | Duration | `1h` | Default time window for service map |
| `j-obs.service-map.cache-expiration` | Duration | `1m` | Cache expiration time |
| `j-obs.service-map.error-threshold` | double | `5.0` | Error rate % for unhealthy status |
| `j-obs.service-map.latency-threshold` | double | `1000` | Latency (ms) for degraded status |
| `j-obs.service-map.min-requests-for-health` | int | `10` | Minimum requests for health calc |

```yaml
j-obs:
  service-map:
    default-window: 1h
    cache-expiration: 1m
    error-threshold: 5.0
    latency-threshold: 1000
    min-requests-for-health: 10
```

---

## SLO/SLI

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.slo.enabled` | boolean | `true` | Enable SLO tracking |
| `j-obs.slo.evaluation-interval` | Duration | `1m` | SLO evaluation interval |
| `j-obs.slo.max-evaluation-history` | int | `100` | Max evaluation history per SLO |
| `j-obs.slo.default-window` | Duration | `30d` | Default SLO window |
| `j-obs.slo.error-budget-warning-threshold` | double | `25.0` | Error budget warning % |
| `j-obs.slo.short-window-burn-rate` | double | `14.4` | Short window burn rate threshold |
| `j-obs.slo.short-window-duration` | Duration | `1h` | Short window duration |
| `j-obs.slo.long-window-burn-rate` | double | `6.0` | Long window burn rate threshold |
| `j-obs.slo.long-window-duration` | Duration | `6h` | Long window duration |

```yaml
j-obs:
  slo:
    enabled: true
    evaluation-interval: 1m
    max-evaluation-history: 100
    default-window: 30d
    error-budget-warning-threshold: 25.0
    short-window-burn-rate: 14.4
    short-window-duration: 1h
    long-window-burn-rate: 6.0
    long-window-duration: 6h
```

---

## Profiling

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `j-obs.profiling.enabled` | boolean | `true` | Enable profiling features |
| `j-obs.profiling.default-duration` | Duration | `60s` | Default CPU profile duration |
| `j-obs.profiling.default-sampling-interval` | Duration | `10ms` | Default sampling interval |
| `j-obs.profiling.max-sessions` | int | `100` | Max profiling sessions in history |
| `j-obs.profiling.max-duration` | Duration | `10m` | Maximum allowed profile duration |

```yaml
j-obs:
  profiling:
    enabled: true
    default-duration: 60s
    default-sampling-interval: 10ms
    max-sessions: 100
    max-duration: 10m
```

---

## WebFlux Support

J-Obs automatically detects and adapts to Spring WebFlux applications. No additional configuration is required.

### Automatic Detection

J-Obs detects the application type at startup:
- **SERVLET** applications use traditional Spring MVC configuration with `HandlerInterceptor` for rate limiting and WebSocket for log streaming
- **REACTIVE** applications use WebFlux configuration with `WebFilter` for rate limiting and Server-Sent Events (SSE) for log streaming

### Log Streaming in WebFlux

In WebFlux applications, log streaming is available via Server-Sent Events instead of WebSocket:

**Endpoint:** `GET /j-obs/api/logs/stream`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `minLevel` | String | `INFO` | Minimum log level to stream |
| `logger` | String | (none) | Filter by logger name (contains match) |
| `message` | String | (none) | Filter by message content (case-insensitive) |

**JavaScript Example:**
```javascript
const eventSource = new EventSource('/j-obs/api/logs/stream?minLevel=INFO');

eventSource.addEventListener('log', (event) => {
    const log = JSON.parse(event.data);
    console.log(`[${log.data.level}] ${log.data.message}`);
});

eventSource.addEventListener('heartbeat', (event) => {
    console.log('Connection alive');
});

eventSource.onerror = (error) => {
    console.error('SSE error:', error);
    eventSource.close();
};
```

### Rate Limiting

Rate limiting works identically in both environments using the same configuration:

```yaml
j-obs:
  rate-limiting:
    enabled: true
    max-requests: 100
    window: 1m
```

In WebFlux applications, a `WebFilter` is used instead of `HandlerInterceptor`, but the behavior is identical.

### Dependencies

To enable WebFlux support, ensure `spring-boot-starter-webflux` is on your classpath:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

> **Note:** WebFlux and Web MVC are mutually exclusive. If both are present, Spring Boot defaults to Servlet mode.

---

## Complete Example

Here's a complete example with commonly used configurations:

```yaml
j-obs:
  enabled: true
  path: /j-obs
  check-interval: 5m

  security:
    enabled: false

  rate-limiting:
    enabled: true
    max-requests: 100
    window: 1m

  dashboard:
    refresh-interval: 5s
    theme: system

  traces:
    enabled: true
    max-traces: 10000
    retention: 1h
    sample-rate: 1.0

  logs:
    enabled: true
    max-entries: 10000
    min-level: INFO
    websocket:
      compression-enabled: true
      max-text-message-size: 65536
      send-buffer-size: 16384

  metrics:
    enabled: true
    refresh-interval: 10000
    max-history-points: 360

  health:
    enabled: true
    max-history-entries: 100

  instrumentation:
    traced-annotation: true
    measured-annotation: true
    http-tracing: true
    auto-instrument: false

  alerts:
    enabled: true
    evaluation-interval: 15s
    max-events: 10000
    retention: 7d
    http-timeout: 30s

    throttling:
      rate-limit: 10
      rate-period: 1m
      cooldown: 5m
      grouping: true
      group-wait: 30s
      max-group-size: 100
      repeat-interval: 4h

    providers:
      telegram:
        enabled: true
        bot-token: ${TELEGRAM_BOT_TOKEN}
        chat-ids:
          - "-1001234567890"

  sql-analyzer:
    enabled: true
    slow-query-threshold: 1s
    very-slow-query-threshold: 5s
    n-plus-one-min-queries: 5
    detect-select-star: true
    detect-missing-limit: true
    analysis-window: 1h

  anomaly-detection:
    enabled: true
    detection-interval: 1m
    baseline-window: 7d
    latency-z-score-threshold: 3.0
    error-rate-z-score-threshold: 2.5

  service-map:
    default-window: 1h
    cache-expiration: 1m
    error-threshold: 5.0
    latency-threshold: 1000

  slo:
    enabled: true
    evaluation-interval: 1m
    default-window: 30d
    error-budget-warning-threshold: 25.0

  profiling:
    enabled: true
    default-duration: 60s
    max-duration: 10m

# Required Spring Boot Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
```

---

## Environment Variables

For sensitive data, use environment variables:

```yaml
j-obs:
  alerts:
    providers:
      telegram:
        bot-token: ${TELEGRAM_BOT_TOKEN}
      email:
        username: ${SMTP_USER}
        password: ${SMTP_PASSWORD}
      webhook:
        headers:
          Authorization: "Bearer ${WEBHOOK_TOKEN}"
```

---

## Duration Format

Duration values support the following formats:
- `PT1H30M` - ISO-8601 format (1 hour 30 minutes)
- `1h` - 1 hour
- `30m` - 30 minutes
- `15s` - 15 seconds
- `500ms` - 500 milliseconds
- `7d` - 7 days

---

## See Also

- [README.md](README.md) - Getting started guide
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions
- [CHANGELOG.md](CHANGELOG.md) - Version history
