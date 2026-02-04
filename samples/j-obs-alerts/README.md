# J-Obs Alerts Sample

This sample demonstrates the J-Obs alert system with multiple notification providers.

## Features Demonstrated

- **Alert Providers**: Telegram, Slack, Email, Webhook
- **Alert Rules**: Metric-based, Log-based, Health-based
- **Alert Throttling**: Rate limiting and grouping
- **Mock Webhook Sink**: Local testing without external services

## Running the Sample

```bash
cd samples/j-obs-alerts
mvn spring-boot:run
```

Access the dashboard at: http://localhost:8083/j-obs

## Triggering Alerts

### Error Spike Alert
```bash
# Trigger 10 errors (should fire error-spike alert)
curl http://localhost:8083/api/alerts/trigger-errors/10
```

### High Latency Alert
```bash
# Trigger a 3-second slow request
curl http://localhost:8083/api/alerts/trigger-slow/3000
```

### Health Alert
```bash
# Set service health to DOWN
curl -X POST http://localhost:8083/api/health-control/set-down

# Set service health back to UP
curl -X POST http://localhost:8083/api/health-control/set-up
```

### View Received Webhooks
```bash
curl -X POST http://localhost:8083/api/webhook-sink/alerts
```

## Configuring Real Providers

### Telegram

1. Create a bot via @BotFather on Telegram
2. Get your chat ID (use @userinfobot)
3. Set environment variables:
   ```bash
   export TELEGRAM_BOT_TOKEN=your-bot-token
   ```
4. Update `application.yml` to enable Telegram provider

### Slack

1. Create a Slack App and enable Incoming Webhooks
2. Copy the webhook URL
3. Set environment variable:
   ```bash
   export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/yyy/zzz
   ```
4. Update `application.yml` to enable Slack provider

### Email

1. Configure SMTP settings:
   ```bash
   export SMTP_HOST=smtp.gmail.com
   export SMTP_PORT=587
   export SMTP_USERNAME=your-email@gmail.com
   export SMTP_PASSWORD=your-app-password
   ```
2. Update `application.yml` to enable Email provider

## Alert Rules

| Rule Name | Type | Trigger Condition | Severity |
|-----------|------|-------------------|----------|
| error-spike | log | >5 ERROR logs in 1min | warning |
| critical-error | log | >10 ERROR logs in 1min | critical |
| high-latency | metric | p99 > 2s for 1min | warning |
| service-down | health | DOWN for 30s | critical |

## Architecture

```
Alert Engine
    │
    ├── Alert Rules (metric, log, health)
    │
    ├── Evaluator (checks conditions every 15s)
    │
    ├── Throttling (rate limit, cooldown, grouping)
    │
    └── Providers
        ├── Telegram
        ├── Slack
        ├── Email
        └── Webhook (mock sink in this sample)
```
