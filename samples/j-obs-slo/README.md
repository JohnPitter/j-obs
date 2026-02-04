# J-Obs SLO Sample

This sample demonstrates Service Level Objective (SLO) tracking and error budget management.

## Features Demonstrated

- **SLO Definitions**: Availability, latency, and success rate SLOs
- **Error Budget**: Visual tracking of remaining error budget
- **Burn Rate**: Monitoring how fast error budget is being consumed
- **Traffic Simulation**: Automated traffic generation for metrics

## Running the Sample

```bash
cd samples/j-obs-slo
mvn spring-boot:run
```

Access the dashboard at: http://localhost:8088/j-obs

## SLOs Defined

| SLO Name | Objective | Window | Description |
|----------|-----------|--------|-------------|
| api-availability | 99.9% | 30d | API should be available |
| api-latency | 99% < 200ms | 30d | p99 latency under 200ms |
| api-latency-p95 | 95% < 100ms | 7d | p95 latency under 100ms |
| success-rate | 99.5% | 7d | Request success rate |

## Traffic Simulation

The sample includes an automatic traffic simulator that generates 5-10 requests per second.
This creates realistic metrics for SLO tracking.

### Control Traffic Simulation

```bash
# Disable traffic simulation
curl -X POST http://localhost:8088/api/traffic/disable

# Enable traffic simulation
curl -X POST http://localhost:8088/api/traffic/enable

# Check status
curl http://localhost:8088/api/traffic/status
```

## Simulating SLO Violations

### Increase Error Rate (burns error budget faster)

```bash
# Set 10% error rate (will violate availability SLO)
curl -X POST http://localhost:8088/api/config/error-rate/10

# Set 50% error rate (severe violation)
curl -X POST http://localhost:8088/api/config/error-rate/50
```

### Increase Latency (burns latency SLO budget)

```bash
# Set 150ms base latency (may violate p95 SLO)
curl -X POST http://localhost:8088/api/config/latency/150

# Set 250ms base latency (will violate p99 SLO)
curl -X POST http://localhost:8088/api/config/latency/250
```

### Reset to Normal

```bash
curl -X POST http://localhost:8088/api/config/reset
```

## Understanding SLO Concepts

### SLI (Service Level Indicator)
A quantitative measure of service behavior. Examples:
- Request latency (p99)
- Error rate
- Availability percentage

### SLO (Service Level Objective)
A target value for an SLI. Examples:
- p99 latency < 200ms
- Availability > 99.9%
- Error rate < 0.1%

### Error Budget
The amount of unreliability your SLO allows:
- 99.9% availability = 0.1% error budget
- Over 30 days = ~43 minutes of allowed downtime

### Burn Rate
How fast you're consuming your error budget:
- Burn rate 1.0x = consuming budget at exactly the sustainable rate
- Burn rate 2.0x = consuming budget twice as fast
- Burn rate 14.4x = will exhaust 30-day budget in ~50 hours

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| GET /api/orders | Main endpoint tracked by SLOs |
| GET /api/health-check | Fast endpoint (always healthy) |
| GET /api/slow-operation | Intentionally slow endpoint |
| POST /api/config/error-rate/{percent} | Set error rate |
| POST /api/config/latency/{ms} | Set base latency |
| POST /api/config/reset | Reset to defaults |
| GET /api/config | Get current config |

## Dashboard Views

1. **SLO Overview**: See all SLOs with current status
2. **Error Budget**: Visual representation of remaining budget
3. **Burn Rate Graph**: Track burn rate over time
4. **SLO History**: Historical compliance data
