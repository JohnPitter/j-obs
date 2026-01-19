# J-Obs

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green?style=for-the-badge&logo=springboot)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)
![Build](https://img.shields.io/github/actions/workflow/status/JohnPitter/j-obs/ci-cd.yml?style=for-the-badge)

**Java Observability Library for Spring Boot**

*Complete out-of-the-box observability with a single dependency*

[Quick Start](#-quick-start) •
[Features](#-features) •
[Screenshots](#-screenshots) •
[Configuration](#%EF%B8%8F-configuration) •
[API Reference](#-api-reference)

</div>

---

## Overview

J-Obs is a Java library that adds complete observability to your Spring Boot application with a single dependency. It provides a beautiful web dashboard for real-time visualization of:

| Feature | Description |
|---------|-------------|
| **Traces** | Complete request journey with OpenTelemetry integration |
| **Logs** | Real-time streaming via WebSocket with filters |
| **Metrics** | Performance data with Micrometer/Prometheus |
| **Health** | Component and dependency health monitoring |
| **Alerts** | Multi-provider notifications (Telegram, Slack, Teams, Email) |
| **SLO/SLI** | Service Level Objectives with error budgets |
| **Profiling** | CPU, Memory, and Thread analysis |
| **Service Map** | Visual dependency graph |
| **SQL Analyzer** | N+1 detection and slow query analysis |
| **Anomaly Detection** | Automatic spike and pattern detection |

---

## Screenshots

### Dashboard Overview

The main dashboard shows health status, recent traces, errors, and key metrics at a glance.

![Dashboard](assets/dashboard.png)

**Key Elements:**
- **Health Status** - Overall application health with degraded/healthy indicators
- **Dependencies** - Status of external services (6/6 healthy in the screenshot)
- **Traces** - Number of recent traces captured (14)
- **Logs** - Total log entries (199)
- **Recent Traces** - List of recent operations with timing
- **Recent Errors** - Latest errors with stack traces
- **Health Components** - Individual component status (Database, Cache, Disk Space, Payment Service, Ping)
- **Metrics Overview** - Key metrics like counters and gauges

---

### Service Dependency Map

Visual representation of your application's dependencies and their health status.

![Service Map](assets/service-map.png)

**Features:**
- **Interactive Graph** - Click on nodes for details
- **Health Indicators** - Green (healthy), Yellow (degraded), Red (down)
- **Connection Details** - Shows latency and request rate between services
- **Auto-Discovery** - Automatically detects dependencies from traces

**Components shown:**
- Application (main service)
- Database
- Payment Gateway
- Inventory Service
- Email Notifications
- Push Notifications

---

### Profiling Tools

On-demand CPU, Memory, and Thread profiling to identify performance bottlenecks.

![Profiling](assets/profiling.png)

**CPU Profiling:**
- Configurable duration (default: 60 seconds)
- Sample-based profiling with minimal overhead
- Flame graph generation
- Hot method identification

**Memory Analysis:**
- Heap usage: Used / Max with percentage
- Non-heap memory tracking
- Garbage collector statistics
- Heap dump trigger

**Thread Dump:**
- Capture current thread state
- Identify blocked and waiting threads
- Deadlock detection
- Stack trace analysis

---

## Quick Start

### Option A: Install from Source (Local)

If the library is not yet published to Maven Central, you can install it locally:

```bash
# Clone the repository
git clone https://github.com/JohnPitter/j-obs.git
cd j-obs

# Build and install to local Maven repository
mvn clean install -DskipTests

# Or with tests
mvn clean install
```

After installation, the artifacts will be available in your local `~/.m2/repository` and you can use them in any project on your machine.

### Option B: Add Dependency from Maven Central

```xml
<dependency>
    <groupId>io.github.j-obs</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Option C: Use GitHub Packages

Add the GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/JohnPitter/j-obs</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
    <groupId>io.github.j-obs</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **Note:** GitHub Packages requires authentication. Add your GitHub credentials to `~/.m2/settings.xml`:
> ```xml
> <servers>
>     <server>
>         <id>github</id>
>         <username>YOUR_GITHUB_USERNAME</username>
>         <password>YOUR_GITHUB_TOKEN</password>
>     </server>
> </servers>
> ```

### Required Dependencies

After installing J-Obs, add these required dependencies to your project:

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- OpenTelemetry -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.32.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-trace</artifactId>
    <version>1.32.0</version>
</dependency>

<!-- Micrometer -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configure Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
```

### Access Dashboard

```
http://localhost:8080/j-obs
```

---

## Features

### 1. Distributed Tracing

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

**Features:**
- Automatic instrumentation of HTTP, Database, Cache, and Messaging
- Span attributes with method, URL, status, timing
- Trace ID propagation across services
- Waterfall visualization
- Flame graph support

**Automatic Instrumentation:**

| Layer | Technologies |
|-------|--------------|
| HTTP Inbound | Spring MVC, WebFlux |
| HTTP Outbound | RestTemplate, WebClient, Feign |
| Database | JDBC, JPA, R2DBC, MongoDB |
| Cache | Redis, Caffeine |
| Messaging | Kafka, RabbitMQ |

---

### 2. Real-Time Logs

Stream logs in real-time with powerful filtering capabilities.

```
23:21:37 INFO  OrderService    Creating order with 3 items
23:21:37 DEBUG OrderService    Validating inventory
23:21:37 INFO  OrderService    Order abc123 created successfully
23:21:38 WARN  PaymentService  High latency detected: 450ms
23:21:39 ERROR PaymentService  Payment failed: Connection timeout
```

**Features:**
- WebSocket streaming for instant updates
- Filter by level (DEBUG, INFO, WARN, ERROR)
- Filter by logger name
- Full-text search
- Trace ID correlation
- Download filtered logs

---

### 3. Metrics Dashboard

Visualize application performance metrics with charts and graphs.

**Built-in Metrics:**

| Category | Metrics |
|----------|---------|
| HTTP | Request rate, Error rate, Latency percentiles |
| JVM | Heap usage, GC pauses, Thread count |
| Database | Connection pool, Query time |
| Custom | Your application metrics |

**Percentile Support:**
- p50 (median)
- p95
- p99
- p99.9

---

### 4. Health Monitoring

Monitor the health of all your application components.

**Built-in Health Checks:**
- Database connectivity
- Disk space
- External services
- Custom health indicators

**Health Status:**
- `UP` - Component is healthy
- `DOWN` - Component is unhealthy
- `DEGRADED` - Component is partially healthy
- `UNKNOWN` - Health cannot be determined

**Custom Health Indicator:**

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

### 5. Alert System

Receive notifications when issues occur via multiple channels.

**Supported Providers:**

| Provider | Configuration |
|----------|---------------|
| **Telegram** | Bot token + Chat IDs |
| **Slack** | Webhook URL |
| **Microsoft Teams** | Webhook URL |
| **Email** | SMTP configuration |
| **Webhook** | Custom HTTP endpoint |

**Alert Types:**

| Type | Trigger |
|------|---------|
| Metric | `error_rate > 5%` or `p99 > 2s` |
| Log | `ERROR count > 10 in 1 minute` |
| Health | Component goes `DOWN` |
| Anomaly | Automatic spike detection |

**Configuration:**

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

### 6. SLO/SLI Tracking

Define and monitor Service Level Objectives.

**Concepts:**

| Term | Definition | Example |
|------|------------|---------|
| **SLI** | Service Level Indicator | Latency p99 |
| **SLO** | Service Level Objective | p99 < 200ms |
| **Error Budget** | Allowed failure margin | 0.1% of requests |
| **Burn Rate** | Budget consumption speed | 2x = twice as fast |

**Dashboard Features:**
- Current SLO compliance percentage
- Error budget remaining
- Burn rate alerts
- Historical trends

**Configuration:**

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

### 7. CPU/Memory Profiling

On-demand profiling to identify performance bottlenecks.

**CPU Profiling:**
```
Top Methods by CPU:
1. com.fasterxml.jackson.ObjectMapper.writeValue    18.5%
2. org.postgresql.jdbc.PgStatement.execute          15.2%
3. java.util.regex.Pattern.matcher                   8.7%
4. io.netty.handler.codec.HttpObjectEncoder          6.3%
5. com.example.service.OrderService.calculate        5.1%
```

**Memory Analysis:**
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

**Thread Dump:**
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

### 8. Service Dependency Map

Visualize your application's architecture and dependencies.

**Features:**
- Auto-discovery from traces
- Real-time health status
- Latency between services
- Request rate visualization
- Click for detailed metrics

**Health Indicators:**
- Green circle = Healthy
- Yellow circle = Degraded
- Red circle = Down

---

### 9. SQL Analyzer

Detect and fix SQL performance issues.

**Problem Detection:**

| Issue | Description | Severity |
|-------|-------------|----------|
| **N+1 Queries** | Multiple similar queries in one trace | Critical |
| **Slow Queries** | Queries exceeding threshold | Warning |
| **Missing Index** | Frequent queries without index | Warning |
| **SELECT *** | Queries returning all columns | Info |
| **No LIMIT** | Unbounded result sets | Warning |

**Example Detection:**

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

### 10. Anomaly Detection

Automatic detection of unusual patterns.

**Detection Types:**

| Type | Algorithm | Example |
|------|-----------|---------|
| Latency Spike | Z-Score | p99 jumped from 45ms to 2,340ms |
| Error Rate Spike | Z-Score | Error rate increased from 0.1% to 4.7% |
| Traffic Anomaly | Moving Average | Request rate 5x higher than normal |

**Auto-Correlation:**
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

---

## Configuration

### Full Configuration Reference

```yaml
j-obs:
  enabled: true
  path: /j-obs

  # Tracing Configuration
  traces:
    enabled: true
    max-traces: 10000
    retention: 1h
    sample-rate: 1.0

  # Logging Configuration
  logs:
    enabled: true
    max-entries: 10000
    min-level: INFO
    buffer-size: 10000

  # Metrics Configuration
  metrics:
    enabled: true
    refresh-interval: 5000
    cache-duration: 30s

  # Health Configuration
  health:
    enabled: true
    check-interval: 30s

  # Alert Configuration
  alerts:
    enabled: true
    evaluation-interval: 15s
    throttling:
      rate-limit: 10
      rate-period: 1m
      cooldown: 5m
      grouping: true
      group-wait: 30s
      max-group-size: 100
    providers:
      telegram:
        enabled: false
        bot-token: ""
        chat-ids: []
      slack:
        enabled: false
        webhook-url: ""
      teams:
        enabled: false
        webhook-url: ""
      email:
        enabled: false
        host: ""
        port: 587
        username: ""
        password: ""
      webhook:
        enabled: false
        url: ""
        headers: {}

  # SLO Configuration
  slos: []

  # Profiling Configuration
  profiling:
    enabled: true
    default-duration: 60s
    max-duration: 300s
    default-sampling-interval: 10ms

  # SQL Analyzer Configuration
  sql-analyzer:
    enabled: true
    slow-query-threshold: 1s
    n-plus-one-threshold: 5

  # Anomaly Detection Configuration
  anomaly-detection:
    enabled: true
    sensitivity: medium
    baseline-window: 7d

  # Service Map Configuration
  service-map:
    enabled: true
    refresh-interval: 30s

  # Security Configuration
  security:
    enabled: false
    username: admin
    password: ""
```

---

## API Reference

### Web Endpoints

| Endpoint | Description |
|----------|-------------|
| `/j-obs` | Main dashboard |
| `/j-obs/traces` | Trace visualization |
| `/j-obs/logs` | Real-time logs |
| `/j-obs/metrics` | Metrics dashboard |
| `/j-obs/health` | Health checks |
| `/j-obs/alerts` | Alert management |
| `/j-obs/tools` | Developer tools (Service Map, Profiling, SQL Analyzer, Anomaly Detection) |

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/j-obs/api/traces` | List traces |
| GET | `/j-obs/api/traces/{id}` | Get trace details |
| GET | `/j-obs/api/logs` | Query logs |
| GET | `/j-obs/api/metrics` | List metrics |
| GET | `/j-obs/api/metrics/{name}` | Get metric values |
| GET | `/j-obs/api/health` | Health status |
| GET | `/j-obs/api/alerts` | List alerts |
| POST | `/j-obs/api/alerts` | Create alert |
| GET | `/j-obs/api/slos` | List SLOs |
| GET | `/j-obs/api/slos/{name}` | Get SLO status |
| POST | `/j-obs/api/profiling/cpu/start` | Start CPU profiling |
| GET | `/j-obs/api/profiling/memory` | Get memory snapshot |
| POST | `/j-obs/api/profiling/threads` | Capture thread dump |
| GET | `/j-obs/api/service-map` | Get service map |
| GET | `/j-obs/api/sql/problems` | List SQL problems |
| GET | `/j-obs/api/anomalies` | List anomalies |

---

## Requirements

- **Java**: 17 or higher
- **Spring Boot**: 3.0 or higher
- **Dependencies**: OpenTelemetry, Micrometer, Actuator

---

## Project Structure

```
j-obs/
├── j-obs-core/                    # Domain model and core logic
│   └── src/main/java/
│       └── io/github/jobs/
│           ├── domain/            # Entities, Value Objects
│           ├── application/       # Use Cases, Ports
│           └── infrastructure/    # Adapters, Repositories
│
├── j-obs-spring-boot-starter/     # Spring Boot auto-configuration
│   └── src/main/java/
│       └── io/github/jobs/spring/
│           ├── autoconfigure/     # Auto-configuration classes
│           ├── web/               # Controllers, WebSocket
│           ├── alert/             # Alert engine and providers
│           └── actuator/          # Custom actuator endpoints
│
└── j-obs-sample/                  # Sample application
```

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Support

- **Issues**: [GitHub Issues](https://github.com/JohnPitter/j-obs/issues)
- **Discussions**: [GitHub Discussions](https://github.com/JohnPitter/j-obs/discussions)

---

<div align="center">

Made with ❤️ for the Java community

</div>
