# J-Obs Features

Complete documentation of all J-Obs features.

## Table of Contents

- [Distributed Tracing](#distributed-tracing)
- [Real-Time Logs](#real-time-logs)
- [Metrics Dashboard](#metrics-dashboard)
- [Health Monitoring](#health-monitoring)
- [Alert System](#alert-system)
- [SLO/SLI Tracking](#slosli-tracking)
- [CPU/Memory Profiling](#cpumemory-profiling)
- [Service Dependency Map](#service-dependency-map)
- [SQL Analyzer](#sql-analyzer)
- [Anomaly Detection](#anomaly-detection)
- [Automatic Instrumentation](#automatic-instrumentation)

---

## Distributed Tracing

Capture the complete journey of every request through your application.

```
▼ GET /api/orders/123                              [245ms]
  ├─ OrderController.getOrder                      [2ms]
  ├─ OrderService.findById                         [180ms]
  │  ├─ Redis GET (MISS)                           [3ms]
  │  ├─ PostgreSQL SELECT                          [45ms]
  │  └─ HTTP inventory-service                     [120ms]
  └─ Response: 200 OK                              [5ms]
```

### Features

- Automatic instrumentation of HTTP, Database, Cache, and Messaging
- Span attributes with method, URL, status, timing
- Trace ID propagation across services
- Waterfall visualization
- Flame graph support

### Automatic Instrumentation

| Layer | Technologies |
|-------|--------------|
| HTTP Inbound | Spring MVC, WebFlux |
| HTTP Outbound | RestTemplate, WebClient, Feign |
| Database | JDBC, JPA, R2DBC, MongoDB |
| Cache | Redis, Caffeine |
| Messaging | Kafka, RabbitMQ |

---

## Real-Time Logs

Stream logs in real-time with powerful filtering capabilities.

```
23:21:37 INFO  OrderService    Creating order with 3 items
23:21:37 DEBUG OrderService    Validating inventory
23:21:37 INFO  OrderService    Order abc123 created successfully
23:21:38 WARN  PaymentService  High latency detected: 450ms
23:21:39 ERROR PaymentService  Payment failed: Connection timeout
```

### Features

- WebSocket streaming for instant updates
- Filter by level (DEBUG, INFO, WARN, ERROR)
- Filter by logger name
- Full-text search
- Trace ID correlation
- Download filtered logs

---

## Metrics Dashboard

Visualize application performance metrics with charts and graphs.

### Built-in Metrics

| Category | Metrics |
|----------|---------|
| HTTP | Request rate, Error rate, Latency percentiles |
| JVM | Heap usage, GC pauses, Thread count |
| Database | Connection pool, Query time |
| Custom | Your application metrics |

### Percentile Support

- p50 (median)
- p95
- p99
- p99.9

---

## Health Monitoring

Monitor the health of all your application components.

### Built-in Health Checks

- Database connectivity
- Disk space
- External services
- Custom health indicators

### Health Status

| Status | Description |
|--------|-------------|
| `UP` | Component is healthy |
| `DOWN` | Component is unhealthy |
| `DEGRADED` | Component is partially healthy |
| `UNKNOWN` | Health cannot be determined |

### Custom Health Indicator

```java
@Component("paymentGateway")
public class PaymentHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        if (isPaymentServiceAvailable()) {
            return Health.up()
                .withDetail("provider", "Stripe")
                .withDetail("latency", "45ms")
                .build();
        }
        return Health.down()
            .withDetail("error", "Connection refused")
            .build();
    }
}
```

---

## Alert System

Receive notifications when issues occur via multiple channels.

### Supported Providers

| Provider | Configuration |
|----------|---------------|
| **Telegram** | Bot token + Chat IDs |
| **Slack** | Webhook URL |
| **Microsoft Teams** | Webhook URL |
| **Email** | SMTP configuration |
| **Webhook** | Custom HTTP endpoint |

### Alert Types

| Type | Trigger |
|------|---------|
| Metric | `error_rate > 5%` or `p99 > 2s` |
| Log | `ERROR count > 10 in 1 minute` |
| Health | Component goes `DOWN` |
| Anomaly | Automatic spike detection |

### Configuration

```yaml
j-obs:
  alerts:
    enabled: true
    evaluation-interval: 15s

    providers:
      telegram:
        enabled: true
        bot-token: ${TELEGRAM_BOT_TOKEN}
        chat-ids:
          - "-1001234567890"

      slack:
        enabled: true
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"

      email:
        enabled: true
        host: smtp.gmail.com
        port: 587
        username: ${SMTP_USER}
        password: ${SMTP_PASSWORD}
        from: alerts@myapp.com
        to:
          - team@myapp.com

    throttling:
      rate-limit: 10
      rate-period: 1m
      cooldown: 5m
      grouping: true
      group-wait: 30s
```

---

## SLO/SLI Tracking

Define and monitor Service Level Objectives.

### Concepts

| Term | Definition | Example |
|------|------------|---------|
| **SLI** | Service Level Indicator | Latency p99 |
| **SLO** | Service Level Objective | p99 < 200ms |
| **Error Budget** | Allowed failure margin | 0.1% of requests |
| **Burn Rate** | Budget consumption speed | 2x = twice as fast |

### Dashboard Features

- Current SLO compliance percentage
- Error budget remaining
- Burn rate alerts
- Historical trends

### Configuration

```yaml
j-obs:
  slos:
    - name: api-availability
      description: "API must be available 99.9% of the time"
      sli:
        type: availability
        good: "status < 500"
      objective: 99.9%
      window: 30d

    - name: api-latency
      description: "99% of requests must complete in under 200ms"
      sli:
        type: latency
        threshold: 200ms
        percentile: 99
      objective: 99%
      window: 30d
```

---

## CPU/Memory Profiling

On-demand profiling to identify performance bottlenecks.

### CPU Profiling

```
Top Methods by CPU:
1. com.fasterxml.jackson.ObjectMapper.writeValue    18.5%
2. org.postgresql.jdbc.PgStatement.execute          15.2%
3. java.util.regex.Pattern.matcher                   8.7%
4. io.netty.handler.codec.HttpObjectEncoder          6.3%
5. com.example.service.OrderService.calculate        5.1%
```

### Memory Analysis

```
Heap Used: 512MB / 1024MB (50%)
GC Pauses: 12ms avg

Top Objects by Memory:
1. byte[]                    145MB (28%)
2. java.lang.String           89MB (17%)
3. java.util.HashMap$Node     45MB (9%)
4. com.example.model.Order    38MB (7%)
5. java.util.ArrayList        22MB (4%)
```

### Thread Dump

```
Thread Count: 47
  RUNNABLE:        12
  WAITING:         25
  TIMED_WAITING:    8
  BLOCKED:          2

Blocked Threads:
- pool-1-thread-3: waiting for lock on OrderService
- pool-1-thread-7: waiting for lock on OrderService
```

---

## Service Dependency Map

Visualize your application's architecture and dependencies.

### Features

- Auto-discovery from traces
- Real-time health status
- Latency between services
- Request rate visualization
- Click for detailed metrics

### Health Indicators

| Color | Status |
|-------|--------|
| Green | Healthy |
| Yellow | Degraded |
| Red | Down |

---

## SQL Analyzer

Detect and fix SQL performance issues.

### Problem Detection

| Issue | Description | Severity |
|-------|-------------|----------|
| **N+1 Queries** | Multiple similar queries in one trace | Critical |
| **Slow Queries** | Queries exceeding threshold | Warning |
| **Missing Index** | Frequent queries without index | Warning |
| **SELECT *** | Queries returning all columns | Info |
| **No LIMIT** | Unbounded result sets | Warning |

### Example Detection

```
🔴 N+1 Query Detected                              [Critical]
─────────────────────────────────────────────────────────────
Endpoint: GET /api/orders
Pattern: 1 query for orders + 47 queries for order_items

Query Principal:
SELECT * FROM orders WHERE user_id = ?

Query Repetida (47x):
SELECT * FROM order_items WHERE order_id = ?

💡 Suggestion: Use JOIN or @EntityGraph to load items

SELECT o.*, oi.* FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.user_id = ?
```

---

## Anomaly Detection

Automatic detection of unusual patterns.

### Detection Types

| Type | Algorithm | Example |
|------|-----------|---------|
| Latency Spike | Z-Score | p99 jumped from 45ms to 2,340ms |
| Error Rate Spike | Z-Score | Error rate increased from 0.1% to 4.7% |
| Traffic Anomaly | Moving Average | Request rate 5x higher than normal |

### Auto-Correlation

When an anomaly is detected, J-Obs automatically searches for:
- Dependency degradation
- Recent deployments
- Slow queries
- Resource exhaustion

---

## Automatic Instrumentation

### @Observable Annotation

Add complete observability to any class with a single annotation:

```java
@Service
@Observable  // All methods are automatically traced and measured
public class OrderService {

    public Order create(OrderRequest request) {
        // Automatically creates:
        // - Span: "OrderService.create"
        // - Metrics: method.timed, method.calls
        return new Order(...);
    }
}
```

### Available Annotations

| Annotation | Traces | Metrics | Description |
|------------|--------|---------|-------------|
| `@Observable` | ✅ | ✅ | Complete instrumentation (recommended) |
| `@Traced` | ✅ | ❌ | Only distributed tracing |
| `@Measured` | ❌ | ✅ | Only metrics collection |

### Custom Configuration

```java
@Service
public class PaymentService {

    @Traced(name = "process-payment", attributes = {"provider=stripe"})
    public void process(Payment payment) {
        // Custom span name and attributes
    }

    @Measured(name = "payment.validation.time", percentiles = {0.5, 0.95, 0.99})
    public void validate(Payment payment) {
        // Custom metric name and percentiles
    }
}
```
