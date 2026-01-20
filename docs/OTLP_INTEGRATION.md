# J-Obs OTLP Integration Guide

This guide explains how to use J-Obs with external OpenTelemetry (OTLP) agents and collectors.

## Table of Contents

- [Overview](#overview)
- [Architecture Options](#architecture-options)
- [Using with OTLP Java Agent](#using-with-otlp-java-agent)
- [Using with External Collectors](#using-with-external-collectors)
- [Read-Only Mode (Dashboard Only)](#read-only-mode-dashboard-only)
- [Integration with Popular Tools](#integration-with-popular-tools)
- [Avoiding Duplicate Data](#avoiding-duplicate-data)
- [Configuration Reference](#configuration-reference)

---

## Overview

J-Obs can work in several modes with OpenTelemetry:

| Mode | Description | Use Case |
|------|-------------|----------|
| **Standalone** | J-Obs collects and displays traces internally | Development, small apps |
| **Export** | J-Obs collects and exports to external backends | Production with Jaeger/Tempo |
| **Read-Only** | External agent collects, J-Obs displays | When using OTLP Java Agent |
| **Hybrid** | J-Obs + Agent, avoiding duplicates | Complex scenarios |

---

## Architecture Options

### Option 1: J-Obs Standalone (Default)

```
┌─────────────────────────────────────────┐
│           Your Application              │
├─────────────────────────────────────────┤
│  J-Obs (traces, logs, metrics)          │
│  └── Dashboard at /j-obs                │
└─────────────────────────────────────────┘
```

**Configuration:**
```yaml
j-obs:
  enabled: true
  traces:
    enabled: true
```

### Option 2: J-Obs with Export to External Backend

```
┌─────────────────────────────────────────┐
│           Your Application              │
├─────────────────────────────────────────┤
│  J-Obs (traces, logs, metrics)          │
│  ├── Dashboard at /j-obs                │
│  └── Export to Jaeger/Tempo/Zipkin      │
└────────────────┬────────────────────────┘
                 │ OTLP/Zipkin
                 ▼
┌─────────────────────────────────────────┐
│  Jaeger / Grafana Tempo / Zipkin        │
└─────────────────────────────────────────┘
```

**Configuration:**
```yaml
j-obs:
  enabled: true
  traces:
    enabled: true
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317
      # Or Zipkin
      zipkin:
        enabled: true
        endpoint: http://zipkin:9411/api/v2/spans
      # Or Jaeger (via OTLP)
      jaeger:
        enabled: true
        endpoint: http://jaeger:4317
```

### Option 3: OTLP Java Agent + J-Obs Dashboard

```
┌─────────────────────────────────────────┐
│           Your Application              │
│  + OpenTelemetry Java Agent (-javaagent)│
├─────────────────────────────────────────┤
│  J-Obs (logs, metrics, dashboard)       │
│  └── Dashboard at /j-obs (read-only)    │
└────────────────┬────────────────────────┘
                 │ OTLP
                 ▼
┌─────────────────────────────────────────┐
│  OpenTelemetry Collector                │
│  └── Jaeger / Tempo / etc              │
└─────────────────────────────────────────┘
```

**Configuration:**
```yaml
j-obs:
  enabled: true
  traces:
    enabled: false  # Agent handles traces
  logs:
    enabled: true
  metrics:
    enabled: true
```

---

## Using with OTLP Java Agent

The [OpenTelemetry Java Agent](https://opentelemetry.io/docs/languages/java/automatic/) provides automatic instrumentation via `-javaagent`.

### Step 1: Download the Agent

```bash
curl -L -o opentelemetry-javaagent.jar \
  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

### Step 2: Configure the Agent

Create `otel.properties`:
```properties
otel.service.name=my-app
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
otel.logs.exporter=otlp
otel.exporter.otlp.endpoint=http://collector:4317
```

### Step 3: Run with Agent

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.javaagent.configuration-file=otel.properties \
     -jar my-app.jar
```

### Step 4: Configure J-Obs in Read-Only Mode

```yaml
j-obs:
  enabled: true
  traces:
    enabled: false  # Disable J-Obs trace collection (agent handles it)
  logs:
    enabled: true   # J-Obs still collects logs
  metrics:
    enabled: true   # J-Obs still collects metrics
```

---

## Using with External Collectors

### OpenTelemetry Collector

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

processors:
  batch:

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
```

**J-Obs Configuration:**
```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://otel-collector:4317
        headers:
          Authorization: "Bearer ${OTEL_TOKEN}"
        timeout: 30s
```

### Grafana Tempo

```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317
```

### Jaeger

```yaml
j-obs:
  traces:
    export:
      jaeger:
        enabled: true
        endpoint: http://jaeger:4317
```

### Zipkin

```yaml
j-obs:
  traces:
    export:
      zipkin:
        enabled: true
        endpoint: http://zipkin:9411/api/v2/spans
```

---

## Read-Only Mode (Dashboard Only)

When using an external OTLP agent for trace collection, configure J-Obs to only display data without collecting traces:

```yaml
j-obs:
  enabled: true

  # Disable trace collection (agent handles it)
  traces:
    enabled: false

  # Keep logs and metrics
  logs:
    enabled: true
    max-entries: 10000

  metrics:
    enabled: true
    refresh-interval: 5000

  # Keep health monitoring
  health:
    enabled: true

  # Keep alerting (based on logs/metrics)
  alerts:
    enabled: true
```

**Benefits:**
- No duplicate traces
- Lower memory usage
- Agent handles complex instrumentation
- J-Obs provides unified dashboard

---

## Integration with Popular Tools

### Jaeger

```yaml
j-obs:
  traces:
    export:
      jaeger:
        enabled: true
        endpoint: http://jaeger:4317

# Or via OTLP collector
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://jaeger:4317
```

**Docker Compose:**
```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP
```

### Grafana Tempo

```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317
```

**Docker Compose:**
```yaml
services:
  tempo:
    image: grafana/tempo:latest
    command: [ "-config.file=/etc/tempo.yaml" ]
    volumes:
      - ./tempo.yaml:/etc/tempo.yaml
    ports:
      - "4317:4317"  # OTLP gRPC
```

### Zipkin

```yaml
j-obs:
  traces:
    export:
      zipkin:
        enabled: true
        endpoint: http://zipkin:9411/api/v2/spans
```

**Docker Compose:**
```yaml
services:
  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
```

### Grafana + Loki + Tempo Stack

For a complete observability stack:

```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://tempo:4317

  # Logs still handled by J-Obs
  logs:
    enabled: true

  # Metrics via Prometheus
  metrics:
    enabled: true
```

---

## Avoiding Duplicate Data

When using both J-Obs and an external agent, you may get duplicate data. Here's how to avoid it:

### Scenario 1: Agent for Traces, J-Obs for Logs/Metrics

```yaml
j-obs:
  traces:
    enabled: false  # Agent handles traces
  logs:
    enabled: true
  metrics:
    enabled: true
```

### Scenario 2: J-Obs for Everything, Export to Backend

```yaml
j-obs:
  traces:
    enabled: true
    export:
      otlp:
        enabled: true
        endpoint: http://collector:4317
```

### Scenario 3: Sampling to Reduce Volume

```yaml
j-obs:
  traces:
    enabled: true
    sample-rate: 0.1  # Only 10% of traces
    export:
      otlp:
        enabled: true
        endpoint: http://collector:4317
```

---

## Configuration Reference

### Trace Export Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `j-obs.traces.export.otlp.enabled` | `false` | Enable OTLP export |
| `j-obs.traces.export.otlp.endpoint` | - | OTLP endpoint URL |
| `j-obs.traces.export.otlp.headers` | `{}` | Custom headers (auth, etc) |
| `j-obs.traces.export.otlp.timeout` | `30s` | Request timeout |
| `j-obs.traces.export.zipkin.enabled` | `false` | Enable Zipkin export |
| `j-obs.traces.export.zipkin.endpoint` | - | Zipkin endpoint URL |
| `j-obs.traces.export.jaeger.enabled` | `false` | Enable Jaeger export |
| `j-obs.traces.export.jaeger.endpoint` | - | Jaeger OTLP endpoint |

### Example: Full Export Configuration

```yaml
j-obs:
  traces:
    enabled: true
    max-traces: 10000
    retention: 1h
    sample-rate: 1.0

    export:
      # OTLP (Grafana Tempo, generic collectors)
      otlp:
        enabled: true
        endpoint: http://tempo:4317
        headers:
          Authorization: "Bearer ${TEMPO_TOKEN}"
        timeout: 30s

      # Zipkin
      zipkin:
        enabled: false
        endpoint: http://zipkin:9411/api/v2/spans

      # Jaeger (via OTLP)
      jaeger:
        enabled: false
        endpoint: http://jaeger:4317
```

---

## Troubleshooting

### Traces not appearing in external backend

1. Check endpoint connectivity:
   ```bash
   curl -v http://collector:4317
   ```

2. Enable debug logging:
   ```yaml
   logging:
     level:
       io.github.jobs: DEBUG
       io.opentelemetry: DEBUG
   ```

3. Verify exporter is registered (check startup logs)

### Duplicate traces

- Ensure only one trace source is enabled
- Use `j-obs.traces.enabled: false` when using external agent

### High memory usage

- Reduce `j-obs.traces.max-traces`
- Enable sampling with `j-obs.traces.sample-rate: 0.1`
- Use shorter retention with `j-obs.traces.retention: 30m`
