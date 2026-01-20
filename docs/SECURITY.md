# J-Obs Security

The J-Obs dashboard can be protected with authentication to prevent unauthorized access. Security is **disabled by default** for development convenience.

## Table of Contents

- [Enabling Security](#enabling-security)
- [Authentication Types](#authentication-types)
- [Basic Authentication](#basic-authentication)
- [API Key Authentication](#api-key-authentication)
- [Mixed Authentication](#mixed-authentication)
- [Exempt Paths](#exempt-paths)
- [Configuration Reference](#configuration-reference)
- [Login Endpoints](#login-endpoints)
- [Best Practices](#best-practices)

---

## Enabling Security

```yaml
j-obs:
  security:
    enabled: true
    type: basic  # or "api-key" or "both"
    users:
      - username: admin
        password: ${J_OBS_PASSWORD}
        role: ADMIN
      - username: viewer
        password: ${J_OBS_VIEWER_PASSWORD}
        role: USER
```

---

## Authentication Types

| Type | Use Case | Description |
|------|----------|-------------|
| `basic` | Browser access | Username/password via login form or HTTP Basic Auth |
| `api-key` | API access | API key in header or query parameter |
| `both` | Mixed access | Both methods accepted |

---

## Basic Authentication

For browser access, users see a login page at `/j-obs/login`. Sessions are maintained with configurable timeout.

```yaml
j-obs:
  security:
    enabled: true
    type: basic
    users:
      - username: admin
        password: ${J_OBS_PASSWORD}
    session-timeout: 8h
```

**API Access with Basic Auth:**

```bash
curl -u admin:password http://localhost:8080/j-obs/api/traces
```

---

## API Key Authentication

For programmatic access, use API keys:

```yaml
j-obs:
  security:
    enabled: true
    type: api-key
    api-keys:
      - ${J_OBS_API_KEY_1}
      - ${J_OBS_API_KEY_2}
    api-key-header: X-API-Key
```

**Usage:**

```bash
# Via header
curl -H "X-API-Key: your-api-key" http://localhost:8080/j-obs/api/traces

# Via query parameter
curl "http://localhost:8080/j-obs/api/traces?api_key=your-api-key"
```

---

## Mixed Authentication

For maximum flexibility, enable both authentication types:

```yaml
j-obs:
  security:
    enabled: true
    type: both
    users:
      - username: admin
        password: ${J_OBS_PASSWORD}
    api-keys:
      - ${J_OBS_API_KEY}
```

---

## Exempt Paths

Certain paths can be exempt from authentication (e.g., static resources):

```yaml
j-obs:
  security:
    enabled: true
    exempt-paths:
      - /static/**
      - /health
      - /ready
```

---

## Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `false` | Enable authentication |
| `type` | `basic` | Auth type: `basic`, `api-key`, or `both` |
| `users` | `[]` | List of users with username, password, role |
| `api-keys` | `[]` | List of valid API keys |
| `api-key-header` | `X-API-Key` | Header name for API key |
| `session-timeout` | `8h` | Session duration |
| `exempt-paths` | `[/static/**]` | Paths that bypass authentication |

---

## Login Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/j-obs/login` | GET | Login page |
| `/j-obs/login` | POST | Form login (username, password, redirect) |
| `/j-obs/logout` | GET/POST | Logout (redirects to login) |
| `/j-obs/api/logout` | POST | API logout (returns JSON) |

---

## Best Practices

1. **Use environment variables** for passwords and API keys
2. **Enable HTTPS** in production
3. **Set strong passwords** with at least 12 characters
4. **Rotate API keys** periodically
5. **Limit session timeout** based on security requirements

---

## Rate Limiting

J-Obs includes built-in rate limiting to protect against DoS attacks:

```yaml
j-obs:
  rate-limiting:
    enabled: true
    max-requests: 100
    window: 1m
```

---

## Input Sanitization

All user inputs are automatically sanitized to prevent:
- SQL Injection
- XSS attacks
- Log injection
- Path traversal

---

## SSRF Prevention

Webhook URLs are validated to prevent Server-Side Request Forgery:
- Blocks localhost and private IPs
- Validates URL scheme (HTTP/HTTPS only)
- Whitelist for known webhook domains (Telegram, Slack, Teams)
