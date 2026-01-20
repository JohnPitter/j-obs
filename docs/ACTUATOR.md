# J-Obs Actuator & API Reference

This document provides a complete reference for all J-Obs endpoints, health indicators, and API operations.

## Table of Contents

- [Overview](#overview)
- [Health Indicator](#health-indicator)
- [Dashboard Pages](#dashboard-pages)
- [REST API Endpoints](#rest-api-endpoints)
  - [General API](#general-api)
  - [Traces API](#traces-api)
  - [Logs API](#logs-api)
  - [Metrics API](#metrics-api)
  - [Health API](#health-api)
  - [Alerts API](#alerts-api)
  - [Alert Events API](#alert-events-api)
  - [Alert Providers API](#alert-providers-api)
  - [SLO API](#slo-api)
  - [Anomalies API](#anomalies-api)
  - [SQL Analyzer API](#sql-analyzer-api)
  - [Service Map API](#service-map-api)
  - [Profiling API](#profiling-api)
  - [Tools API](#tools-api)

---

## Overview

J-Obs exposes endpoints under a configurable base path (default: `/j-obs`).

```yaml
j-obs:
  path: /j-obs  # Change this to customize the base path
```

**Endpoint Types:**

| Type | Path Pattern | Description |
|------|--------------|-------------|
| Dashboard | `/j-obs/*` | HTML pages for the web UI |
| REST API | `/j-obs/api/*` | JSON APIs for programmatic access |
| WebSocket | `/j-obs/ws/*` | Real-time streaming (logs) |

---

## Health Indicator

J-Obs registers a Spring Boot Actuator health indicator named `jObs`.

### Accessing Health Status

```bash
# Full health details
curl http://localhost:8080/actuator/health

# J-Obs specific health
curl http://localhost:8080/actuator/health/jObs
```

### Health Response Structure

```json
{
  "status": "UP",
  "components": {
    "jObs": {
      "status": "UP",
      "details": {
        "traces": {
          "current": 150,
          "max": 10000,
          "usagePercent": 1.5
        },
        "logs": {
          "current": 5000,
          "max": 10000,
          "usagePercent": 50.0
        },
        "metrics": {
          "totalMetrics": 45
        },
        "alertEvents": {
          "current": 25,
          "max": 10000,
          "usagePercent": 0.25
        },
        "estimatedMemoryMb": 12
      }
    }
  }
}
```

### Health States

| Status | Condition | Description |
|--------|-----------|-------------|
| `UP` | Usage < 80% | J-Obs is healthy |
| `DEGRADED` | Usage 80-95% | Repository capacity is high |
| `DOWN` | Usage > 95% | Repository capacity critical |
| `DOWN` | Exception | Internal error occurred |

### Configuration

```yaml
management:
  endpoint:
    health:
      show-details: always
  health:
    jObs:
      enabled: true  # Enable/disable the health indicator
```

---

## Dashboard Pages

| Path | Description |
|------|-------------|
| `/j-obs` | Main dashboard with overview |
| `/j-obs/traces` | Distributed traces viewer |
| `/j-obs/logs` | Log viewer with real-time streaming |
| `/j-obs/metrics` | Metrics dashboard |
| `/j-obs/health` | Health status page |
| `/j-obs/alerts` | Alert rules management |
| `/j-obs/anomalies` | Anomaly detection dashboard |
| `/j-obs/service-map` | Service dependency map |
| `/j-obs/tools/profiling` | CPU/Memory profiling |
| `/j-obs/tools/sql` | SQL analyzer |
| `/j-obs/tools/slo` | SLO/SLI dashboard |

---

## REST API Endpoints

All API endpoints return JSON and accept `application/json` for request bodies.

### General API

**Base Path:** `/j-obs/api`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/requirements` | Check dependency requirements |
| `GET` | `/status` | Get J-Obs status overview |

**Example: Get Requirements**
```bash
curl http://localhost:8080/j-obs/api/requirements
```

```json
{
  "status": "COMPLETE",
  "missing": [],
  "found": [
    {"name": "spring-boot-actuator", "version": "3.2.0"},
    {"name": "opentelemetry-api", "version": "1.32.0"},
    {"name": "micrometer-core", "version": "1.12.0"}
  ]
}
```

---

### Traces API

**Base Path:** `/j-obs/api/traces`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List traces with pagination |
| `GET` | `/{traceId}` | Get trace by ID |
| `GET` | `/{traceId}/spans` | Get spans for a trace |
| `GET` | `/stats` | Get trace statistics |
| `DELETE` | `/` | Clear all traces |

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (0-based) |
| `size` | int | Page size (default: 20) |
| `service` | string | Filter by service name |
| `minDuration` | long | Minimum duration in ms |
| `maxDuration` | long | Maximum duration in ms |
| `status` | string | Filter by status (OK, ERROR) |

**Example: List Traces**
```bash
curl "http://localhost:8080/j-obs/api/traces?page=0&size=10&minDuration=100"
```

---

### Logs API

**Base Path:** `/j-obs/api/logs`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List logs with pagination |
| `GET` | `/stats` | Get log statistics |
| `GET` | `/stream` | SSE stream of logs |
| `DELETE` | `/` | Clear all logs |

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number |
| `size` | int | Page size |
| `level` | string | Filter by level (INFO, WARN, ERROR) |
| `logger` | string | Filter by logger name |
| `search` | string | Full-text search |
| `traceId` | string | Filter by trace ID |

**Example: Search Logs**
```bash
curl "http://localhost:8080/j-obs/api/logs?level=ERROR&search=NullPointer"
```

---

### Metrics API

**Base Path:** `/j-obs/api/metrics`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List all metrics |
| `GET` | `/{name}` | Get metric by name |
| `GET` | `/stats` | Get metrics statistics |
| `GET` | `/jvm` | Get JVM metrics |
| `GET` | `/http` | Get HTTP metrics |

**Example: Get JVM Metrics**
```bash
curl http://localhost:8080/j-obs/api/metrics/jvm
```

```json
{
  "heap": {
    "used": 256000000,
    "max": 512000000,
    "usagePercent": 50.0
  },
  "nonHeap": {
    "used": 64000000
  },
  "gc": {
    "count": 15,
    "time": 234
  },
  "threads": {
    "live": 25,
    "daemon": 20,
    "peak": 30
  }
}
```

---

### Health API

**Base Path:** `/j-obs/api/health`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Get health overview |
| `GET` | `/components` | List health components |
| `GET` | `/components/{name}` | Get component health |
| `GET` | `/history` | Get health history |

**Example: Get Health Components**
```bash
curl http://localhost:8080/j-obs/api/health/components
```

---

### Alerts API

**Base Path:** `/j-obs/api/alerts`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List all alert rules |
| `GET` | `/{id}` | Get alert by ID |
| `POST` | `/` | Create new alert rule |
| `PUT` | `/{id}` | Update alert rule |
| `DELETE` | `/{id}` | Delete alert rule |
| `POST` | `/{id}/enable` | Enable alert |
| `POST` | `/{id}/disable` | Disable alert |
| `POST` | `/{id}/test` | Test alert rule |
| `GET` | `/types` | Get available alert types |
| `GET` | `/severities` | Get available severities |
| `GET` | `/operators` | Get available operators |

**Example: Create Alert**
```bash
curl -X POST http://localhost:8080/j-obs/api/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "High Error Rate",
    "type": "METRIC",
    "severity": "CRITICAL",
    "condition": {
      "metric": "http_server_requests_seconds_count",
      "operator": "GREATER_THAN",
      "threshold": 100,
      "window": "PT5M"
    },
    "enabled": true
  }'
```

---

### Alert Events API

**Base Path:** `/j-obs/api/alert-events`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List alert events |
| `GET` | `/{id}` | Get event by ID |
| `POST` | `/{id}/acknowledge` | Acknowledge event |
| `POST` | `/{id}/resolve` | Resolve event |
| `GET` | `/stats` | Get event statistics |

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status (FIRING, ACKNOWLEDGED, RESOLVED) |
| `severity` | string | Filter by severity |
| `alertId` | string | Filter by alert rule ID |

---

### Alert Providers API

**Base Path:** `/j-obs/api/alert-providers`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List all providers |
| `GET` | `/{name}` | Get provider details |
| `POST` | `/{name}/test` | Test provider |

**Example: List Providers**
```bash
curl http://localhost:8080/j-obs/api/alert-providers
```

```json
[
  {
    "name": "telegram",
    "displayName": "Telegram",
    "enabled": true,
    "configured": true
  },
  {
    "name": "slack",
    "displayName": "Slack",
    "enabled": false,
    "configured": false
  }
]
```

**Example: Test Provider**
```bash
curl -X POST http://localhost:8080/j-obs/api/alert-providers/telegram/test
```

---

### SLO API

**Base Path:** `/j-obs/api/slos`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List all SLOs |
| `GET` | `/{id}` | Get SLO by ID |
| `POST` | `/` | Create SLO |
| `PUT` | `/{id}` | Update SLO |
| `DELETE` | `/{id}` | Delete SLO |
| `GET` | `/{id}/status` | Get SLO current status |
| `GET` | `/{id}/error-budget` | Get error budget |
| `GET` | `/{id}/burn-rate` | Get burn rate history |

---

### Anomalies API

**Base Path:** `/j-obs/api/anomalies`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List detected anomalies |
| `GET` | `/{id}` | Get anomaly details |
| `POST` | `/{id}/acknowledge` | Acknowledge anomaly |
| `POST` | `/{id}/ignore` | Ignore anomaly |
| `GET` | `/stats` | Get anomaly statistics |

---

### SQL Analyzer API

**Base Path:** `/j-obs/api/sql`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/problems` | List SQL problems |
| `GET` | `/slow-queries` | List slow queries |
| `GET` | `/n-plus-one` | List N+1 issues |
| `GET` | `/stats` | Get SQL statistics |

---

### Service Map API

**Base Path:** `/j-obs/api/service-map`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Get service map data |
| `GET` | `/nodes` | Get service nodes |
| `GET` | `/edges` | Get service connections |
| `GET` | `/node/{name}` | Get node details |

---

### Profiling API

**Base Path:** `/j-obs/api/profiling`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/cpu/start` | Start CPU profiling |
| `POST` | `/cpu/stop` | Stop CPU profiling |
| `GET` | `/cpu/{sessionId}` | Get CPU profile result |
| `GET` | `/cpu/{sessionId}/flamegraph` | Get flame graph |
| `GET` | `/memory` | Get memory snapshot |
| `POST` | `/heap-dump` | Trigger heap dump |
| `GET` | `/threads` | Get thread dump |
| `GET` | `/sessions` | List profiling sessions |

**Example: Start CPU Profile**
```bash
curl -X POST "http://localhost:8080/j-obs/api/profiling/cpu/start?duration=60s"
```

---

### Tools API

**Base Path:** `/j-obs/api/tools`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List available tools |
| `GET` | `/gc` | Trigger garbage collection |
| `GET` | `/clear-caches` | Clear internal caches |

---

## WebSocket Endpoints

### Log Streaming

**Endpoint:** `ws://localhost:8080/j-obs/ws/logs`

Connect to receive real-time log entries.

**Message Format:**
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "ERROR",
  "logger": "com.example.MyService",
  "message": "Something went wrong",
  "traceId": "abc123",
  "spanId": "def456"
}
```

**JavaScript Example:**
```javascript
const ws = new WebSocket('ws://localhost:8080/j-obs/ws/logs');

ws.onmessage = (event) => {
  const log = JSON.parse(event.data);
  console.log(`[${log.level}] ${log.message}`);
};
```

---

## Rate Limiting

All API endpoints are rate-limited by default:

| Setting | Default | Description |
|---------|---------|-------------|
| `max-requests` | 100 | Max requests per window |
| `window` | 1 minute | Time window |

When rate limited, you'll receive:
```json
{
  "error": "Rate limit exceeded",
  "retryAfter": 30
}
```

---

## Error Responses

All API errors follow this format:

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid alert configuration",
  "path": "/j-obs/api/alerts"
}
```

Common HTTP status codes:

| Code | Description |
|------|-------------|
| `200` | Success |
| `201` | Created |
| `400` | Bad Request |
| `404` | Not Found |
| `429` | Rate Limited |
| `500` | Internal Server Error |

---

## See Also

- [README.md](README.md) - Getting started guide
- [CONFIGURATION.md](CONFIGURATION.md) - Configuration reference
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues
