# J-Obs

**Java Observability Library for Spring Boot**

J-Obs é uma biblioteca que adiciona observabilidade completa à sua aplicação Spring Boot com uma única dependência. Inclui dashboard web para visualização em tempo real de:

- **Traces** - Jornada completa de cada requisição (OpenTelemetry)
- **Logs** - Stream em tempo real via WebSocket
- **Métricas** - Dados de performance com gráficos (Micrometer)
- **Health Checks** - Status dos componentes (Actuator)

## Quick Start

### 1. Adicionar Dependência

```xml
<dependency>
    <groupId>io.github.j-obs</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Adicionar Dependências Obrigatórias

```xml
<!-- Actuator -->
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

### 3. Configurar Actuator

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

### 4. Acessar o Dashboard

```
http://localhost:8080/j-obs
```

## Screenshots

### Dashboard Principal
```
┌─────────────────────────────────────────────────────────────┐
│  J-Obs Dashboard                                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐        │
│  │ HEALTH  │  │ TRACES  │  │  LOGS   │  │ METRICS │        │
│  │   ✅    │  │   147   │  │  1,234  │  │   52    │        │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Trace Waterfall
```
▼ GET /api/orders/123                         [245ms]
  ├─ OrderController.getOrder                 [2ms]
  ├─ OrderService.findById                    [180ms]
  │  ├─ Redis GET (MISS)                      [3ms]
  │  ├─ PostgreSQL SELECT                     [45ms]
  │  └─ HTTP inventory-service                [120ms]
  └─ Response: 200 OK                         [5ms]
```

### Logs em Tempo Real
```
23:21:37 INFO  OrderService - Creating order with 3 items
23:21:37 DEBUG OrderService - Validating inventory
23:21:37 INFO  OrderService - Order abc123 created
23:21:38 WARN  PaymentService - High latency detected
```

## Configuração

```yaml
j-obs:
  enabled: true
  path: /j-obs

  traces:
    enabled: true
    max-traces: 10000
    retention: 1h

  logs:
    enabled: true
    max-entries: 10000
    min-level: INFO

  metrics:
    enabled: true
    refresh-interval: 5000

  health:
    enabled: true
```

## Endpoints Disponíveis

| URL | Descrição |
|-----|-----------|
| `/j-obs` | Dashboard principal |
| `/j-obs/traces` | Visualização de traces |
| `/j-obs/logs` | Logs em tempo real |
| `/j-obs/metrics` | Métricas com gráficos |
| `/j-obs/health` | Health checks |

## Instrumentação Automática

### Zero Configuration - Basta adicionar @Observable

```java
@Service
@Observable  // Todos os métodos são automaticamente traced e medidos!
public class OrderService {

    public Order create(OrderRequest request) {
        // Automaticamente cria:
        // - Span: "OrderService.create"
        // - Métricas: method.timed, method.calls
        return new Order(...);
    }

    public Order process(String orderId) {
        // Também instrumentado automaticamente
        return findById(orderId);
    }
}
```

### Annotations Disponíveis

| Annotation | Descrição |
|------------|-----------|
| `@Observable` | Traces + Métricas (recomendado) |
| `@Traced` | Apenas traces |
| `@Measured` | Apenas métricas |

### Customização

```java
@Service
public class PaymentService {

    @Traced(name = "process-payment", attributes = {"provider=stripe"})
    public void process(Payment payment) {
        // Span com nome e atributos customizados
    }

    @Measured(name = "payment.time", percentiles = {0.5, 0.95, 0.99})
    public void validate(Payment payment) {
        // Métricas com configuração específica
    }
}
```

### HTTP Automático

Todas as requisições HTTP são automaticamente instrumentadas:
- Spans com método, URL, status
- Métricas de latência por endpoint
- Trace ID no MDC para correlação de logs

### Health Checks

```java
@Component("meuBanco")
public class MeuHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("database", "PostgreSQL")
            .withDetail("responseTime", "5ms")
            .build();
    }
}
```

## Documentação

Para documentação completa, veja [GETTING_STARTED.md](GETTING_STARTED.md).

## Requisitos

- Java 17+
- Spring Boot 3.0+

## Módulos

| Módulo | Descrição |
|--------|-----------|
| `j-obs-core` | Domain model e lógica central |
| `j-obs-spring-boot-starter` | Auto-configuração Spring Boot |
| `j-obs-sample` | Aplicação de exemplo |

## Licença

Apache License 2.0

## Contribuindo

1. Fork o projeto
2. Crie sua branch (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Add nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request
