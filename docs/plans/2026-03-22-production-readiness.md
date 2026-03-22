# Production Readiness Implementation Plan

**Goal:** Make J-Obs production-ready with persistence, security defaults, sampling, and performance hardening.

**Architecture:** Optional JDBC persistence (auto-configures when DataSource available), security-by-default, configurable trace sampling, AOP metadata caching, and OTLP export support.

**Streams (parallel):**
- A: JDBC Persistence (new package `persistence/`, auto-config, Flyway migrations)
- B: Security Hardening (defaults, WebSocket auth, config validation, error boundaries)
- C: Performance (trace sampling, AOP method cache, retention policies)
- D: RBAC + Export (role-based access, OTLP export auto-config)
