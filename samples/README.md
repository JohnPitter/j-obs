# J-Obs Samples

This directory contains focused sample applications demonstrating specific J-Obs features.

## Available Samples

| Sample | Port | Description |
|--------|------|-------------|
| [j-obs-minimal](./j-obs-minimal) | 8080 | Quickstart with minimal dependencies |
| [j-obs-webflux](./j-obs-webflux) | 8081 | Reactive/WebFlux application |
| [j-obs-security](./j-obs-security) | 8082 | Authentication & authorization |
| [j-obs-alerts](./j-obs-alerts) | 8083 | Alert providers (Telegram, Slack, etc.) |
| [j-obs-microservices](./j-obs-microservices) | 8084-8086 | Distributed tracing & service map |
| [j-obs-database](./j-obs-database) | 8087 | SQL Analyzer & JPA tracing |
| [j-obs-slo](./j-obs-slo) | 8088 | SLO/SLI tracking |

## Quick Start

### Run a specific sample:

```bash
cd samples/j-obs-minimal
mvn spring-boot:run
```

Then access the J-Obs dashboard at: http://localhost:8080/j-obs

### Build all samples:

```bash
mvn clean install -pl samples -am
```

## Sample Details

### j-obs-minimal (Quickstart)
**Best for:** Getting started quickly with J-Obs

Features demonstrated:
- Basic dashboard
- Automatic HTTP tracing
- Log capture
- Health checks
- JVM metrics

### j-obs-webflux (Reactive)
**Best for:** Reactive/WebFlux applications

Features demonstrated:
- Reactive endpoint tracing
- Mono/Flux stream handling
- Non-blocking observability

### j-obs-security (Authentication)
**Best for:** Understanding J-Obs security integration

Features demonstrated:
- Basic Auth
- Form-based login
- Role-based access control
- Security event tracing

Default credentials:
- admin/admin (ADMIN role)
- user/user (USER role)
- viewer/viewer (VIEWER role)

### j-obs-alerts (Notifications)
**Best for:** Setting up alert notifications

Features demonstrated:
- Telegram, Slack, Email providers
- Webhook integration
- Alert rules (metric, log, health-based)
- Alert throttling and grouping

### j-obs-microservices (Distributed Systems)
**Best for:** Multi-service architectures

Features demonstrated:
- Distributed tracing
- Service Map visualization
- Cross-service span correlation
- Trace context propagation

Architecture:
```
Client → API Gateway (8084) → Order Service (8085) → Inventory Service (8086)
```

### j-obs-database (SQL Analysis)
**Best for:** JPA/JDBC applications

Features demonstrated:
- N+1 query detection
- Slow query identification
- Missing index suggestions
- JPA/Hibernate integration

Includes intentional anti-patterns for learning.

### j-obs-slo (SLO Tracking)
**Best for:** Production reliability monitoring

Features demonstrated:
- SLO definition
- Error budget tracking
- Burn rate monitoring
- Traffic simulation

## Comparison with j-obs-sample

The root-level `j-obs-sample` module is a comprehensive demo showing many features together (orders, payments, inventory). These `samples/` projects are focused demonstrations of specific features, making them easier to understand and copy into your own projects.

## Creating Your Own Sample

Use `j-obs-minimal` as a template:

1. Copy the directory
2. Update `pom.xml` with your artifact ID
3. Update the package structure
4. Add your specific features

## Port Assignments

| Port | Sample |
|------|--------|
| 8080 | j-obs-minimal |
| 8081 | j-obs-webflux |
| 8082 | j-obs-security |
| 8083 | j-obs-alerts |
| 8084 | j-obs-microservices (API Gateway) |
| 8085 | j-obs-microservices (Order Service) |
| 8086 | j-obs-microservices (Inventory Service) |
| 8087 | j-obs-database |
| 8088 | j-obs-slo |
