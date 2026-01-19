# J-Obs - Java Observability Library

## Visão Geral

J-Obs é uma dependência Java para Spring Boot que fornece observabilidade completa out-of-the-box. Ao adicionar a dependência no `pom.xml`, um endpoint `/j-obs` é automaticamente exposto com uma interface web para visualização em tempo real de:

- **Logs** - Stream em tempo real de todos os logs da aplicação
- **Traces** - Jornada completa de cada requisição através dos serviços
- **Métricas** - Dados de performance e saúde da aplicação
- **Health Checks** - Status dos componentes e dependências

## Exemplo de Uso

```xml
<dependency>
    <groupId>io.github.j-obs</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Após adicionar a dependência:
- Aplicação: `localhost:8080`
- Dashboard de Observabilidade: `localhost:8080/j-obs`

## Stack Tecnológica

- **OpenTelemetry** - Instrumentação e coleta de traces, métricas e logs
- **Prometheus** - Exposição e agregação de métricas
- **Spring Boot Actuator** - Health checks e endpoints de gerenciamento
- **WebSocket** - Streaming em tempo real de logs e traces
- **HTMX + Tailwind CSS** - Interface web reativa e moderna

---

## Princípios de Desenvolvimento

### 1. Arquitetura Limpa

- Separação clara entre camadas: Domain, Application, Infrastructure, Presentation
- Inversão de dependência - domínio não conhece frameworks
- Use cases isolados e testáveis
- Entities sem dependências externas

### 2. Performance (Big O Notation)

- Análise de complexidade obrigatória para estruturas de dados e algoritmos
- Evitar O(n²) ou superior sem justificativa documentada
- Preferir operações O(1) ou O(log n) para lookups frequentes
- Buffer e batch processing para operações de I/O
- Lazy loading para dados pesados

### 3. Segurança (CVE Mitigation)

- Dependências sempre atualizadas (Dependabot/Renovate)
- Validação de input em todas as bordas
- Sanitização de output para prevenir XSS
- Prepared statements para queries
- Headers de segurança configurados
- Rate limiting nos endpoints

### 4. Resiliência e Cache

- Circuit breakers para chamadas externas
- Retry com backoff exponencial
- Timeouts configuráveis
- Cache em múltiplas camadas (L1: local, L2: distribuído)
- Graceful degradation quando serviços falham
- Bulkhead pattern para isolamento de falhas

### 5. Design Moderno

- Interface responsiva e acessível (WCAG 2.1)
- Dark/Light mode
- Componentes reutilizáveis
- Feedback visual imediato
- Loading states e skeleton screens
- Animações sutis e funcionais

### 6. Pirâmide de Testes

```
        /\
       /  \  E2E (poucos)
      /----\
     /      \  Integração (médio)
    /--------\
   /          \  Unitários (muitos)
  --------------
```

- **Unitários**: 70% - Lógica de domínio e use cases
- **Integração**: 20% - Controllers, repositories, clients
- **E2E**: 10% - Fluxos críticos do usuário
- Coverage mínimo: 80%
- Mutation testing para validar qualidade dos testes

### 7. Proteção de Dados

- Nunca logar dados sensíveis (PII, credentials, tokens)
- Mascaramento automático de campos sensíveis
- Encryption at rest e in transit
- Audit trail para operações críticas
- Configuração de retenção de dados

### 8. Observabilidade

- Logs estruturados (JSON) com correlation ID
- Trace ID propagado em todos os serviços
- Métricas RED (Rate, Errors, Duration)
- Métricas USE (Utilization, Saturation, Errors)
- Alertas configuráveis
- Dashboards por contexto (técnico, negócio)

### 9. Design System

- Tokens de design (cores, espaçamentos, tipografia)
- Componentes documentados
- Consistência visual em toda aplicação
- Acessibilidade built-in
- Temas customizáveis

### 10. Desenvolvimento por Fases

Toda feature deve ser planejada em:

```
Fase 1: Foundation
  └── SubFase 1.1: Domain modeling
  └── SubFase 1.2: Core interfaces
  └── SubFase 1.3: Unit tests

Fase 2: Implementation
  └── SubFase 2.1: Infrastructure
  └── SubFase 2.2: Integration tests
  └── SubFase 2.3: API endpoints

Fase 3: Presentation
  └── SubFase 3.1: UI components
  └── SubFase 3.2: E2E tests
  └── SubFase 3.3: Documentation
```

### 11. Changelog

Todas as alterações devem ser documentadas no `CHANGELOG.md` seguindo [Keep a Changelog](https://keepachangelog.com/):

```markdown
## [Unreleased]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
```

### 12. Build Funcional

- Build deve passar antes de qualquer commit
- Imports não utilizados removidos automaticamente
- Code formatting padronizado (spotless/google-java-format)
- Static analysis sem warnings (SpotBugs, PMD)
- Dependency check sem vulnerabilidades críticas

---

## Regras do Agente

### 1. Timeout de Comandos

- Comandos com mais de 60 segundos devem ser cancelados ou movidos para background
- Usar `run_in_background: true` para builds longos
- Feedback ao usuário sobre progresso

### 2. Fallback de Soluções

- Se uma abordagem falhar 2x, pesquisar alternativas na internet
- Documentar soluções tentadas e motivo da falha
- Preferir soluções com comunidade ativa e documentação

### 3. Economia de Tokens

- Ir direto à implementação
- Evitar resumos extensos do que foi feito
- Código > explicação
- Mostrar apenas diffs relevantes
- Não repetir contexto já estabelecido

---

## Estrutura do Projeto

```
j-obs/
├── j-obs-core/                    # Domínio e lógica central
│   ├── src/main/java/
│   │   └── io/github/jobs/
│   │       ├── domain/            # Entities, Value Objects
│   │       ├── application/       # Use Cases, Ports
│   │       └── infrastructure/    # Adapters, Repositories
│   └── src/test/java/
├── j-obs-spring-boot-starter/     # Auto-configuration
│   ├── src/main/java/
│   │   └── io/github/jobs/spring/
│   │       ├── autoconfigure/     # Spring Boot auto-config
│   │       ├── web/               # Controllers, WebSocket
│   │       └── actuator/          # Custom endpoints
│   └── src/main/resources/
│       ├── META-INF/
│       │   └── spring.factories
│       └── static/                # Frontend assets
├── j-obs-samples/                 # Exemplos de uso
├── CHANGELOG.md
├── README.md
└── pom.xml
```

---

## Verificação de Requisitos (Startup Check)

Ao acessar `/j-obs`, o sistema executa verificação automática de dependências no classpath. Se algum requisito estiver ausente, exibe uma tela de diagnóstico com instruções.

### Dependências Verificadas

| Dependência | Artifact | Obrigatória | Funcionalidade |
|-------------|----------|-------------|----------------|
| OpenTelemetry API | `io.opentelemetry:opentelemetry-api` | Sim | Core de tracing |
| OpenTelemetry SDK | `io.opentelemetry:opentelemetry-sdk` | Sim | Instrumentação |
| Micrometer Core | `io.micrometer:micrometer-core` | Sim | Métricas |
| Micrometer Prometheus | `io.micrometer:micrometer-registry-prometheus` | Sim | Export Prometheus |
| Spring Boot Actuator | `org.springframework.boot:spring-boot-starter-actuator` | Sim | Health/Endpoints |
| Logback | `ch.qos.logback:logback-classic` | Não | Logs (fallback p/ java.util.logging) |

### Comportamento da Tela de Requisitos

```
┌─────────────────────────────────────────────────────────────┐
│  J-Obs - Verificação de Requisitos                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✅ OpenTelemetry API ................ Detectado (1.32.0)   │
│  ✅ OpenTelemetry SDK ................ Detectado (1.32.0)   │
│  ❌ Micrometer Prometheus ............ Não encontrado       │
│  ✅ Spring Boot Actuator ............. Detectado (3.2.0)    │
│  ⚠️  Logback ......................... Não encontrado       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  ❌ 1 dependência obrigatória ausente                       │
│  ⚠️  1 dependência opcional ausente                         │
│                                                             │
│  Adicione ao seu pom.xml:                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ <dependency>                                           │ │
│  │   <groupId>io.micrometer</groupId>                     │ │
│  │   <artifactId>micrometer-registry-prometheus</artifactId>│
│  │   <version>1.12.0</version>                            │ │
│  │ </dependency>                                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                             │
│  [Copiar pom.xml]  [Verificar Novamente]  [Documentação]    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Implementação Técnica

```java
@Component
public class DependencyChecker {

    private static final List<Dependency> REQUIRED = List.of(
        new Dependency("io.opentelemetry.api.OpenTelemetry", "OpenTelemetry API", true),
        new Dependency("io.opentelemetry.sdk.OpenTelemetrySdk", "OpenTelemetry SDK", true),
        new Dependency("io.micrometer.core.instrument.MeterRegistry", "Micrometer Core", true),
        new Dependency("io.micrometer.prometheus.PrometheusMeterRegistry", "Micrometer Prometheus", true),
        new Dependency("org.springframework.boot.actuate.endpoint.annotation.Endpoint", "Spring Actuator", true),
        new Dependency("ch.qos.logback.classic.Logger", "Logback", false)
    );

    public DependencyCheckResult check() {
        return REQUIRED.stream()
            .map(this::checkDependency)
            .collect(DependencyCheckResult.collector());
    }

    private DependencyStatus checkDependency(Dependency dep) {
        try {
            Class.forName(dep.className());
            String version = detectVersion(dep);
            return DependencyStatus.found(dep, version);
        } catch (ClassNotFoundException e) {
            return DependencyStatus.notFound(dep);
        }
    }
}
```

### Estados da Verificação

| Estado | Ação |
|--------|------|
| **Todas obrigatórias presentes** | Redireciona para dashboard principal |
| **Obrigatória ausente** | Exibe tela de requisitos com instruções |
| **Apenas opcional ausente** | Exibe warning banner no dashboard |

### Verificação em Runtime

- Check executado apenas no primeiro acesso (resultado em cache)
- Botão "Verificar Novamente" força re-check
- Endpoint `/j-obs/api/requirements` retorna JSON com status
- Health indicator registrado no Actuator

### Guia de Instalação de Dependências

Quando uma dependência não for encontrada, a tela exibe instruções específicas para cada uma:

#### OpenTelemetry (Obrigatório)
```xml
<!-- OpenTelemetry BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-bom</artifactId>
            <version>1.32.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenTelemetry Dependencies -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-trace</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

#### Micrometer + Prometheus (Obrigatório)
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### Spring Boot Actuator (Obrigatório)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Configuração application.yml recomendada
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  prometheus:
    metrics:
      export:
        enabled: true

otel:
  exporter:
    otlp:
      endpoint: http://localhost:4317
  service:
    name: ${spring.application.name}
```

### Tela de Instalação Completa

```
┌─────────────────────────────────────────────────────────────────┐
│  J-Obs - Dependências Necessárias                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ❌ OpenTelemetry não encontrado                                 │
│                                                                  │
│  O OpenTelemetry é necessário para coleta de traces e spans.    │
│                                                                  │
│  📦 Adicione ao seu pom.xml:                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ <dependency>                                               │ │
│  │   <groupId>io.opentelemetry.instrumentation</groupId>      │ │
│  │   <artifactId>opentelemetry-spring-boot-starter</artifactId>│ │
│  │ </dependency>                                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│  [Copiar]                                                        │
│                                                                  │
│  📄 Adicione ao application.yml:                                │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ otel:                                                      │ │
│  │   service:                                                 │ │
│  │     name: ${spring.application.name}                       │ │
│  └────────────────────────────────────────────────────────────┘ │
│  [Copiar]                                                        │
│                                                                  │
│  📚 Documentação: https://opentelemetry.io/docs/java            │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  [Ver Todas Dependências]  [Copiar pom.xml Completo]  [Refresh] │
└─────────────────────────────────────────────────────────────────┘
```

### Endpoint de Dependências

```
GET /j-obs/api/requirements

Response:
{
  "status": "INCOMPLETE",
  "missing": ["opentelemetry-api", "micrometer-registry-prometheus"],
  "found": [
    { "name": "spring-boot-actuator", "version": "3.2.0" },
    { "name": "micrometer-core", "version": "1.12.0" }
  ],
  "instructions": {
    "opentelemetry-api": {
      "maven": "<dependency>...</dependency>",
      "gradle": "implementation 'io.opentelemetry:opentelemetry-api'",
      "docs": "https://opentelemetry.io/docs/java"
    }
  }
}
```

---

## Funcionalidades Principais

### Traces (Prioridade Alta)

O sistema de traces captura a jornada completa de uma requisição através de todas as camadas da aplicação.

#### Camadas Instrumentadas

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            TRACE COMPLETO                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ▼ GET /api/orders/123                                         [245ms]      │
│  │                                                                           │
│  ├─▶ Controller: OrderController.getOrder()                    [2ms]        │
│  │                                                                           │
│  ├─▶ Service: OrderService.findById()                          [180ms]      │
│  │   │                                                                       │
│  │   ├─▶ Cache: Redis GET order:123                            [3ms] MISS   │
│  │   │                                                                       │
│  │   ├─▶ Database: PostgreSQL                                  [45ms]       │
│  │   │   └─ SELECT * FROM orders WHERE id = ?                               │
│  │   │                                                                       │
│  │   ├─▶ HTTP: GET inventory-service/api/stock/123             [120ms]      │
│  │   │   └─ Response: 200 OK                                                │
│  │   │                                                                       │
│  │   └─▶ Cache: Redis SET order:123                            [2ms]        │
│  │                                                                           │
│  ├─▶ Service: NotificationService.sendEvent()                  [55ms]       │
│  │   │                                                                       │
│  │   └─▶ Kafka: PRODUCE order-events                           [50ms]       │
│  │       └─ Topic: order-events, Partition: 3                               │
│  │                                                                           │
│  └─▶ Response: 200 OK                                          [5ms]        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tipos de Spans Capturados

| Camada | Tecnologias | Atributos Capturados |
|--------|-------------|----------------------|
| **HTTP Inbound** | Spring MVC, WebFlux | method, url, status, headers, client IP |
| **HTTP Outbound** | RestTemplate, WebClient, Feign | url, method, status, duration |
| **Database** | JDBC, JPA, R2DBC, MongoDB | db.system, db.statement, db.operation, rows affected |
| **Cache** | Redis, Caffeine, Hazelcast | operation (GET/SET/DEL), key, hit/miss |
| **Messaging** | Kafka, RabbitMQ, SQS | topic, partition, offset, queue |
| **gRPC** | gRPC Client/Server | service, method, status |
| **Custom** | @Traced annotation | custom attributes |

#### Instrumentação Automática

O J-Obs auto-instrumenta as principais bibliotecas via OpenTelemetry:

```yaml
j-obs:
  traces:
    instrumentation:
      # HTTP
      spring-web: true
      spring-webflux: true
      http-client: true          # RestTemplate, WebClient
      feign: true

      # Database
      jdbc: true
      hibernate: true
      r2dbc: true
      mongo: true

      # Cache
      redis: true
      caffeine: true

      # Messaging
      kafka: true
      rabbitmq: true
      sqs: true

      # Outros
      grpc: true
      graphql: true
```

#### Instrumentação Manual

Para métodos customizados, use a annotation `@Traced`:

```java
@Service
public class PaymentService {

    @Traced(name = "process-payment", attributes = {
        @SpanAttribute(key = "payment.method", value = "#method"),
        @SpanAttribute(key = "payment.amount", value = "#amount")
    })
    public PaymentResult process(String method, BigDecimal amount) {
        // Lógica de pagamento
    }
}
```

Ou programaticamente:

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Tracer tracer;

    public Order processOrder(OrderRequest request) {
        Span span = tracer.spanBuilder("process-order")
            .setAttribute("order.items", request.getItems().size())
            .setAttribute("order.total", request.getTotal().doubleValue())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Lógica
            Order order = createOrder(request);

            span.setAttribute("order.id", order.getId());
            span.addEvent("order-created");

            return order;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

#### Propagação de Contexto

O trace ID é propagado automaticamente entre serviços:

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   API GW    │ ───▶ │  Order Svc  │ ───▶ │ Payment Svc │
│             │      │             │      │             │
│ trace-id: A │      │ trace-id: A │      │ trace-id: A │
│ span-id: 1  │      │ span-id: 2  │      │ span-id: 3  │
│ parent: -   │      │ parent: 1   │      │ parent: 2   │
└─────────────┘      └─────────────┘      └─────────────┘
```

Headers propagados:
- `traceparent` (W3C Trace Context)
- `tracestate`
- `baggage`

#### Visualização na UI

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > Traces > abc123def456                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Trace ID: abc123def456              Duration: 245ms                         │
│  Service: order-service              Spans: 8                                │
│  Start: 2024-01-15 10:30:45.123      Status: OK                             │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Timeline (Waterfall)                                          0ms    245ms │
│  ──────────────────────────────────────────────────────────────────────────│
│                                                                              │
│  ▼ GET /api/orders/123                    ████████████████████████████ 245ms│
│    ├─ OrderController.getOrder            █ 2ms                              │
│    ├─ OrderService.findById               ██████████████████ 180ms           │
│    │  ├─ Redis GET (MISS)                 █ 3ms                              │
│    │  ├─ PostgreSQL SELECT                ████ 45ms                          │
│    │  ├─ HTTP inventory-service           ████████████ 120ms                 │
│    │  └─ Redis SET                        █ 2ms                              │
│    └─ NotificationService.sendEvent       █████ 55ms                         │
│       └─ Kafka PRODUCE                    ████ 50ms                          │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Span Details: PostgreSQL SELECT                                [45ms]      │
│  ──────────────────────────────────────────────────────────────────────────│
│  db.system: postgresql                                                       │
│  db.name: orders_db                                                          │
│  db.statement: SELECT * FROM orders WHERE id = $1                           │
│  db.operation: SELECT                                                        │
│  net.peer.name: localhost                                                   │
│  net.peer.port: 5432                                                        │
│                                                                              │
│  [Ver Logs Correlacionados]  [Copiar Trace ID]  [Exportar JSON]             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Filtros e Busca

| Filtro | Exemplo | Descrição |
|--------|---------|-----------|
| `trace.id` | `abc123def456` | Busca por ID exato |
| `span.name` | `PostgreSQL*` | Nome do span (wildcard) |
| `span.kind` | `SERVER`, `CLIENT`, `PRODUCER` | Tipo de span |
| `service.name` | `order-service` | Serviço de origem |
| `http.status_code` | `>= 400` | Status HTTP |
| `db.system` | `postgresql` | Tipo de banco |
| `duration` | `> 1s` | Spans lentos |
| `error` | `true` | Apenas com erro |

#### Métricas Derivadas dos Traces

O J-Obs gera métricas automaticamente a partir dos traces:

```
# Latência por endpoint
http_server_duration_seconds{method="GET", uri="/api/orders/{id}", quantile="0.99"}

# Latência por dependência externa
http_client_duration_seconds{target="inventory-service", quantile="0.95"}

# Latência de queries
db_client_duration_seconds{db_system="postgresql", operation="SELECT", quantile="0.95"}

# Taxa de cache hit/miss
cache_operations_total{cache="redis", result="hit|miss"}
```

#### Export para Ferramentas Externas

```yaml
j-obs:
  traces:
    export:
      # Jaeger
      jaeger:
        enabled: true
        endpoint: http://jaeger:14250

      # Zipkin
      zipkin:
        enabled: false
        endpoint: http://zipkin:9411/api/v2/spans

      # OTLP (Grafana Tempo, etc)
      otlp:
        enabled: false
        endpoint: http://otel-collector:4317
```

### Logs (Prioridade Alta)

- Stream em tempo real via WebSocket
- Filtros por level, logger, mensagem
- Highlight de erros e warnings
- Busca full-text
- Correlação com trace ID
- Download de logs filtrados

### Métricas

- Gráficos de latência (p50, p95, p99)
- Request rate e error rate
- JVM metrics (heap, GC, threads)
- Custom metrics da aplicação
- Comparação temporal

### Health

- Status de cada componente
- Dependency health (DB, cache, external services)
- Histórico de incidents
- Alertas configuráveis

---

## Dashboard Overview

Página inicial com visão geral da saúde da aplicação em tempo real.

### Layout do Dashboard

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs Dashboard                                    [5s] [15s] [30s] [1m]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────┐│
│  │  HEALTH         │  │  REQUESTS/s     │  │  ERROR RATE     │  │  P99    ││
│  │  ✅ Healthy     │  │  📈 1,247       │  │  ⚠️ 0.3%        │  │  45ms   ││
│  │                 │  │  ↑ 12%          │  │  ↑ 0.1%         │  │  ↓ 5ms  ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────┘│
│                                                                              │
│  ┌─────────────────────────────────────┐  ┌─────────────────────────────────┐
│  │  Request Rate (últimos 5 min)       │  │  Response Time Distribution     │
│  │  ▁▂▃▄▅▆▇█▇▆▅▄▃▄▅▆▇█▇▆▅▄▃▂▁▂▃▄▅▆    │  │  p50: 12ms  p95: 38ms  p99: 45ms│
│  │                                     │  │  ████████░░░░░░░░░░░░░░░░░░░░░░ │
│  └─────────────────────────────────────┘  └─────────────────────────────────┘
│                                                                              │
│  ┌─────────────────────────────────────┐  ┌─────────────────────────────────┐
│  │  Top Endpoints (by latency)         │  │  Dependencies Health            │
│  │  ──────────────────────────────────│  │  ──────────────────────────────│
│  │  POST /api/orders      89ms  ████▌  │  │  ✅ PostgreSQL       3ms        │
│  │  GET  /api/users/{id}  45ms  ██▌    │  │  ✅ Redis            1ms        │
│  │  GET  /api/products    32ms  ██     │  │  ⚠️ payment-service  250ms      │
│  │  POST /api/payments    28ms  █▌     │  │  ✅ Kafka            5ms        │
│  └─────────────────────────────────────┘  └─────────────────────────────────┘
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Recent Errors                                              [Ver todos] ││
│  │  ─────────────────────────────────────────────────────────────────────  ││
│  │  🔴 10:45:32  NullPointerException em OrderService.process()            ││
│  │  🔴 10:44:18  Connection timeout: payment-service                        ││
│  │  🟡 10:43:55  Slow query detected: 2.3s SELECT * FROM orders...         ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Widgets Disponíveis

| Widget | Descrição | Refresh |
|--------|-----------|---------|
| **Health Status** | Status geral e dos componentes | 5s |
| **Request Rate** | Requisições por segundo | 1s |
| **Error Rate** | Percentual de erros (4xx, 5xx) | 5s |
| **Latency Percentiles** | p50, p95, p99 | 5s |
| **Top Endpoints** | Endpoints mais lentos/chamados | 10s |
| **Dependencies** | Saúde das dependências externas | 10s |
| **Recent Errors** | Últimos erros com stack trace | 5s |
| **JVM Metrics** | Heap, GC, Threads | 5s |
| **Active Traces** | Traces em andamento | 1s |

### Customização

```yaml
j-obs:
  dashboard:
    refresh-interval: 5s
    widgets:
      - type: health
        position: { row: 1, col: 1 }
      - type: request-rate
        position: { row: 1, col: 2 }
      - type: error-rate
        position: { row: 1, col: 3 }
        threshold:
          warning: 1%
          critical: 5%
      - type: latency
        position: { row: 1, col: 4 }
        percentiles: [p50, p95, p99]
```

---

## Service Map

Visualização gráfica das dependências e comunicação entre serviços.

### Mapa de Dependências

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > Service Map                              [Auto Layout] [Refresh]   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                              ┌─────────────┐                                 │
│                              │   Client    │                                 │
│                              │   (Browser) │                                 │
│                              └──────┬──────┘                                 │
│                                     │ HTTPS                                  │
│                                     ▼                                        │
│                              ┌─────────────┐                                 │
│                              │  API Gateway│                                 │
│                              │   nginx     │                                 │
│                              └──────┬──────┘                                 │
│                                     │                                        │
│                    ┌────────────────┼────────────────┐                      │
│                    │                │                │                       │
│                    ▼                ▼                ▼                       │
│             ┌───────────┐    ┌───────────┐    ┌───────────┐                 │
│             │  Order    │    │   User    │    │  Product  │                 │
│             │  Service  │    │  Service  │    │  Service  │                 │
│             │  ✅ 45ms  │    │  ✅ 12ms  │    │  ✅ 18ms  │                 │
│             └─────┬─────┘    └───────────┘    └───────────┘                 │
│                   │                                                          │
│          ┌────────┼────────┬─────────────┐                                  │
│          │        │        │             │                                   │
│          ▼        ▼        ▼             ▼                                   │
│    ┌──────────┐ ┌─────┐ ┌────────┐ ┌──────────┐                             │
│    │ Payment  │ │Redis│ │PostgreSQL│ │  Kafka   │                            │
│    │ Service  │ │ ✅  │ │  ✅ 8ms │ │  ✅ 3ms  │                            │
│    │ ⚠️ 250ms │ │ 2ms │ └────────┘ └──────────┘                             │
│    └──────────┘ └─────┘                                                      │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  Legend: ✅ Healthy  ⚠️ Degraded  🔴 Down    Line thickness = request volume│
└─────────────────────────────────────────────────────────────────────────────┘
```

### Informações por Conexão

Ao clicar em uma conexão entre serviços:

```
┌─────────────────────────────────────────┐
│  order-service → payment-service        │
├─────────────────────────────────────────┤
│  Protocol: HTTP/2                       │
│  Requests/s: 145                        │
│  Error Rate: 0.5%                       │
│  Avg Latency: 250ms                     │
│  P99 Latency: 890ms                     │
│                                         │
│  Recent Errors:                         │
│  - Connection timeout (3)               │
│  - 503 Service Unavailable (2)          │
│                                         │
│  [Ver Traces]  [Ver Métricas]           │
└─────────────────────────────────────────┘
```

### Detecção Automática

O Service Map é construído automaticamente a partir dos traces:

```java
// Extrai dependências dos spans
public class ServiceMapBuilder {

    public ServiceMap buildFromTraces(List<Trace> traces) {
        Map<ServicePair, ConnectionStats> connections = new HashMap<>();

        for (Trace trace : traces) {
            for (Span span : trace.getSpans()) {
                if (span.getKind() == SpanKind.CLIENT) {
                    ServicePair pair = new ServicePair(
                        span.getServiceName(),
                        span.getAttribute("peer.service")
                    );
                    connections.computeIfAbsent(pair, ConnectionStats::new)
                        .record(span);
                }
            }
        }

        return new ServiceMap(connections);
    }
}
```

---

## SQL Analyzer

Análise automática de queries SQL para identificar problemas de performance.

### Detecção de Problemas

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > SQL Analyzer                                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ⚠️ 3 problemas detectados                                                  │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🔴 N+1 Query Detected                                      [Crítico]   ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Endpoint: GET /api/orders                                              ││
│  │ Pattern: 1 query para orders + 47 queries para order_items             ││
│  │                                                                         ││
│  │ Query Principal:                                                        ││
│  │ SELECT * FROM orders WHERE user_id = ?                                 ││
│  │                                                                         ││
│  │ Query Repetida (47x):                                                   ││
│  │ SELECT * FROM order_items WHERE order_id = ?                           ││
│  │                                                                         ││
│  │ 💡 Sugestão: Use JOIN ou @EntityGraph para carregar items              ││
│  │                                                                         ││
│  │ SELECT o.*, oi.* FROM orders o                                         ││
│  │ LEFT JOIN order_items oi ON o.id = oi.order_id                         ││
│  │ WHERE o.user_id = ?                                                    ││
│  │                                                                         ││
│  │ [Ver Trace]  [Copiar Query]  [Marcar Resolvido]                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🟡 Slow Query                                              [Warning]   ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Duration: 2.3s (threshold: 1s)                                         ││
│  │ Query: SELECT * FROM products WHERE category LIKE '%electronics%'      ││
│  │                                                                         ││
│  │ 💡 Sugestão: Adicione índice ou use Full-Text Search                   ││
│  │ CREATE INDEX idx_products_category ON products(category);              ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ 🟡 Missing Index                                           [Warning]   ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Query: SELECT * FROM users WHERE email = ?                             ││
│  │ Executions: 1,247/min    Avg: 145ms                                    ││
│  │                                                                         ││
│  │ 💡 Sugestão: Coluna 'email' não possui índice                          ││
│  │ CREATE UNIQUE INDEX idx_users_email ON users(email);                   ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Tipos de Análise

| Problema | Detecção | Severidade |
|----------|----------|------------|
| **N+1 Queries** | Múltiplas queries similares no mesmo trace | Critical |
| **Slow Queries** | Queries acima do threshold configurado | Warning/Critical |
| **Missing Index** | Queries frequentes com alto tempo | Warning |
| **SELECT *** | Queries que retornam todas as colunas | Info |
| **Large Result Set** | Queries que retornam muitos registros | Warning |
| **No LIMIT** | SELECT sem LIMIT em tabelas grandes | Warning |
| **Cartesian Join** | JOINs sem condição | Critical |

### Configuração

```yaml
j-obs:
  sql-analyzer:
    enabled: true
    thresholds:
      slow-query: 1s
      very-slow-query: 5s
      n-plus-one:
        min-queries: 5
        similarity: 0.9
      large-result-set: 1000

    ignore-patterns:
      - "SELECT 1"  # Health checks
      - ".*flyway.*"  # Migrations

    suggestions:
      enabled: true
      include-ddl: true  # Sugerir CREATE INDEX
```

### API para Análise

```java
@RestController
@RequestMapping("/j-obs/api/sql")
public class SqlAnalyzerController {

    @GetMapping("/problems")
    public List<SqlProblem> getProblems(
        @RequestParam(defaultValue = "1h") Duration window,
        @RequestParam(defaultValue = "WARNING") Severity minSeverity
    ) {
        return sqlAnalyzer.analyze(window, minSeverity);
    }

    @GetMapping("/slow-queries")
    public List<SlowQuery> getSlowQueries(
        @RequestParam(defaultValue = "1s") Duration threshold
    ) {
        return sqlAnalyzer.findSlowQueries(threshold);
    }
}
```

---

## Anomaly Detection

Detecção automática de comportamentos anômalos usando algoritmos estatísticos.

### Tipos de Anomalias

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > Anomalias                                      Últimas 24 horas    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  🔴 2 anomalias críticas detectadas                                         │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Latency Spike                                              14:32:15    ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Endpoint: POST /api/orders                                             ││
│  │ Normal: 45ms (p99)     Atual: 2,340ms (+5,100%)                        ││
│  │                                                                         ││
│  │ Baseline  ▁▁▁▂▁▁▁▁▂▁▁▁▁▁▁▁▁▁▁▁▁▁█████▁▁▁▁▁▁▁  Anomalia                ││
│  │                                                                         ││
│  │ Possíveis causas:                                                       ││
│  │ • payment-service latência aumentou 3x                                  ││
│  │ • Queries PostgreSQL 2x mais lentas                                    ││
│  │                                                                         ││
│  │ [Investigar]  [Criar Alerta]  [Ignorar]                                ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Error Rate Spike                                           14:30:00    ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Normal: 0.1%     Atual: 4.7% (+4,600%)                                 ││
│  │                                                                         ││
│  │ Top Errors:                                                             ││
│  │ • ConnectionTimeoutException (78%)                                      ││
│  │ • CircuitBreakerOpenException (22%)                                    ││
│  │                                                                         ││
│  │ Serviço afetado: payment-service                                        ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │ Traffic Anomaly                                            10:15:00    ││
│  │ ─────────────────────────────────────────────────────────────────────  ││
│  │ Normal: ~500 req/s     Atual: 2,847 req/s (+469%)                      ││
│  │ Status: ✅ Sistema respondendo normalmente                             ││
│  │                                                                         ││
│  │ 💡 Possível causa: Campanha de marketing iniciada às 10:00            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Algoritmos Utilizados

| Algoritmo | Uso | Descrição |
|-----------|-----|-----------|
| **Z-Score** | Latência, Error Rate | Detecta valores fora do desvio padrão |
| **Moving Average** | Traffic | Compara com média móvel |
| **Seasonal Decomposition** | Padrões diários | Considera sazonalidade |
| **Isolation Forest** | Multi-dimensional | Detecta outliers em múltiplas métricas |

### Configuração

```yaml
j-obs:
  anomaly-detection:
    enabled: true

    # Baseline
    baseline:
      window: 7d           # Dados históricos para baseline
      min-samples: 1000    # Mínimo de amostras

    # Sensibilidade
    sensitivity:
      latency:
        z-score-threshold: 3.0      # 3 desvios padrão
        min-increase-percent: 100   # Mínimo 2x para alertar
      error-rate:
        z-score-threshold: 2.5
        min-absolute: 1%            # Ignorar se < 1%
      traffic:
        z-score-threshold: 3.0
        alert-on-decrease: true     # Alertar queda também

    # Ações automáticas
    actions:
      auto-create-alert: false
      notify-on-detection: true
      providers: [telegram]
```

### Correlação Automática

Quando uma anomalia é detectada, o J-Obs automaticamente busca causas:

```java
public class AnomalyCorrelator {

    public List<PossibleCause> findCauses(Anomaly anomaly) {
        List<PossibleCause> causes = new ArrayList<>();

        // 1. Verificar dependências
        for (Dependency dep : getDependencies(anomaly.getService())) {
            if (hasAnomalyInWindow(dep, anomaly.getTimestamp())) {
                causes.add(new PossibleCause(
                    "Dependency degradation: " + dep.getName(),
                    Confidence.HIGH
                ));
            }
        }

        // 2. Verificar deploys recentes
        Optional<Deploy> recentDeploy = findRecentDeploy(anomaly.getTimestamp());
        if (recentDeploy.isPresent()) {
            causes.add(new PossibleCause(
                "Recent deploy: " + recentDeploy.get().getVersion(),
                Confidence.MEDIUM
            ));
        }

        // 3. Verificar queries lentas
        List<SlowQuery> slowQueries = findSlowQueriesInWindow(anomaly);
        if (!slowQueries.isEmpty()) {
            causes.add(new PossibleCause(
                "Slow queries detected: " + slowQueries.size(),
                Confidence.MEDIUM
            ));
        }

        return causes;
    }
}
```

---

## SLO/SLI Tracking

Definição e acompanhamento de Service Level Objectives (SLOs) e Indicators (SLIs).

### Conceitos

| Termo | Definição | Exemplo |
|-------|-----------|---------|
| **SLI** | Métrica que indica nível de serviço | Latência p99, Error rate |
| **SLO** | Objetivo/meta para o SLI | p99 < 200ms, Error rate < 0.1% |
| **Error Budget** | Margem de erro permitida | 0.1% de requests podem falhar |
| **Burn Rate** | Velocidade de consumo do budget | 2x = consome budget 2x mais rápido |

### Dashboard de SLOs

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > SLOs                                               Período: 30d    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  API Availability                                                       ││
│  │  SLO: 99.9%    Current: 99.95%    Status: ✅ Healthy                   ││
│  │                                                                         ││
│  │  Error Budget: ████████████████████░░░░ 78% remaining (23.4h)          ││
│  │  Burn Rate: 0.7x (safe)                                                 ││
│  │                                                                         ││
│  │  30d ▁▁▁▁▁▁▁▁▁▁▁▁▂▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁▁                                   ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  API Latency (p99)                                                      ││
│  │  SLO: < 200ms    Current: 145ms    Status: ✅ Healthy                  ││
│  │                                                                         ││
│  │  Error Budget: ██████████████████████████ 95% remaining                ││
│  │  Burn Rate: 0.2x (safe)                                                 ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Payment Success Rate                                                   ││
│  │  SLO: 99.5%    Current: 99.2%    Status: ⚠️ At Risk                   ││
│  │                                                                         ││
│  │  Error Budget: ████░░░░░░░░░░░░░░░░░░░░ 15% remaining (4.5h)           ││
│  │  Burn Rate: 2.3x (elevated)                                             ││
│  │                                                                         ││
│  │  ⚠️ At current burn rate, budget exhausts in 4.5 hours                 ││
│  │                                                                         ││
│  │  [Ver Detalhes]  [Criar Alerta]  [Ajustar SLO]                         ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Definição de SLOs

```yaml
j-obs:
  slos:
    - name: api-availability
      description: "API deve estar disponível 99.9% do tempo"
      sli:
        type: availability
        metric: http_server_requests_total
        good: "status < 500"
        total: "status >= 0"
      objective: 99.9%
      window: 30d
      alerts:
        burn-rate-1h: 14.4    # Alerta se burn rate > 14.4x em 1h
        burn-rate-6h: 6       # Alerta se burn rate > 6x em 6h
        budget-remaining: 25%  # Alerta se budget < 25%

    - name: api-latency
      description: "99% das requests devem completar em menos de 200ms"
      sli:
        type: latency
        metric: http_server_requests_seconds
        threshold: 0.2  # 200ms
        percentile: 99
      objective: 99%
      window: 30d

    - name: payment-success
      description: "99.5% dos pagamentos devem ser processados com sucesso"
      sli:
        type: ratio
        good:
          metric: payment_transactions_total
          filter: "status='success'"
        total:
          metric: payment_transactions_total
      objective: 99.5%
      window: 7d
```

### Alertas Baseados em Burn Rate

```yaml
# Multi-window, multi-burn-rate alerts (Google SRE approach)
alerts:
  - name: slo-api-availability-page
    type: burn-rate
    slo: api-availability
    conditions:
      - burn_rate: 14.4
        window: 1h
        for: 5m
      - burn_rate: 6
        window: 6h
        for: 30m
    severity: critical
    providers: [pagerduty]

  - name: slo-api-availability-ticket
    type: burn-rate
    slo: api-availability
    conditions:
      - burn_rate: 3
        window: 1d
        for: 2h
      - burn_rate: 1
        window: 3d
        for: 6h
    severity: warning
    providers: [slack, email]
```

### API de SLOs

```java
@RestController
@RequestMapping("/j-obs/api/slos")
public class SloController {

    @GetMapping
    public List<SloStatus> getAllSlos() {
        return sloService.getAllStatus();
    }

    @GetMapping("/{name}")
    public SloDetail getSlo(@PathVariable String name) {
        return sloService.getDetail(name);
    }

    @GetMapping("/{name}/error-budget")
    public ErrorBudgetStatus getErrorBudget(@PathVariable String name) {
        return sloService.getErrorBudget(name);
    }

    @GetMapping("/{name}/burn-rate")
    public BurnRateHistory getBurnRate(
        @PathVariable String name,
        @RequestParam(defaultValue = "24h") Duration window
    ) {
        return sloService.getBurnRateHistory(name, window);
    }
}
```

---

## Profiling

CPU e Memory profiling sob demanda para identificar gargalos.

### Dashboard de Profiling

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  J-Obs > Profiling                                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Status: ⚪ Parado    [▶ Iniciar CPU Profile]  [▶ Iniciar Heap Dump]        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  CPU Profile (últimos 60s)                                              ││
│  │  ─────────────────────────────────────────────────────────────────────  ││
│  │                                                                         ││
│  │  Flame Graph:                                                           ││
│  │  ┌─────────────────────────────────────────────────────────────────┐   ││
│  │  │ main                                                            │   ││
│  │  ├─────────────────────────────────────────┬───────────────────────┤   ││
│  │  │ OrderService.process (45%)              │ PaymentService (30%)  │   ││
│  │  ├───────────────────┬─────────────────────┼───────────────────────┤   ││
│  │  │ DB Query (25%)    │ Serialization (20%) │ HTTP Call (30%)       │   ││
│  │  └───────────────────┴─────────────────────┴───────────────────────┘   ││
│  │                                                                         ││
│  │  Top Methods by CPU:                                                    ││
│  │  1. com.fasterxml.jackson.databind.ObjectMapper.writeValue  18.5%      ││
│  │  2. org.postgresql.jdbc.PgStatement.execute                 15.2%      ││
│  │  3. java.util.regex.Pattern.matcher                         8.7%       ││
│  │  4. io.netty.handler.codec.http.HttpObjectEncoder           6.3%       ││
│  │  5. com.example.service.OrderService.calculateTotal         5.1%       ││
│  │                                                                         ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  Memory Analysis                                                        ││
│  │  ─────────────────────────────────────────────────────────────────────  ││
│  │                                                                         ││
│  │  Heap Used: 512MB / 1024MB (50%)    GC Pauses: 12ms avg                ││
│  │                                                                         ││
│  │  ████████████████████░░░░░░░░░░░░░░░░░░░░                              ││
│  │                                                                         ││
│  │  Top Objects by Memory:                                                 ││
│  │  1. byte[]                          145MB (28%)                        ││
│  │  2. java.lang.String                 89MB (17%)                        ││
│  │  3. java.util.HashMap$Node           45MB (9%)                         ││
│  │  4. com.example.model.Order          38MB (7%)                         ││
│  │  5. java.util.ArrayList              22MB (4%)                         ││
│  │                                                                         ││
│  │  [Download Heap Dump]  [Analyze Allocations]                           ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Tipos de Profiling

| Tipo | Descrição | Impacto |
|------|-----------|---------|
| **CPU Sampling** | Amostragem de stack traces | Baixo (~2%) |
| **CPU Instrumentation** | Instrumentação de métodos | Alto (~20%) |
| **Heap Dump** | Snapshot da memória | Pausa a JVM |
| **Allocation Tracking** | Rastreia alocações | Médio (~10%) |
| **Thread Dump** | Estado de todas as threads | Nenhum |
| **Continuous Profiling** | Profiling em background | Muito baixo (~1%) |

### Configuração

```yaml
j-obs:
  profiling:
    enabled: true

    # CPU Profiling
    cpu:
      sampling-interval: 10ms
      max-duration: 60s
      include-system-threads: false

    # Memory Profiling
    memory:
      allocation-tracking: false  # Habilitar sob demanda
      heap-dump-on-oom: true
      heap-dump-path: /tmp/heapdump

    # Continuous Profiling (low overhead)
    continuous:
      enabled: true
      interval: 10s
      duration: 1s
      retention: 24h

    # Segurança
    security:
      require-auth: true
      allowed-roles: [ADMIN, DEVELOPER]
```

### API de Profiling

```java
@RestController
@RequestMapping("/j-obs/api/profiling")
public class ProfilingController {

    @PostMapping("/cpu/start")
    public ProfileSession startCpuProfile(
        @RequestParam(defaultValue = "60s") Duration duration,
        @RequestParam(defaultValue = "10ms") Duration interval
    ) {
        return profiler.startCpuProfile(duration, interval);
    }

    @GetMapping("/cpu/{sessionId}")
    public CpuProfileResult getCpuProfile(@PathVariable String sessionId) {
        return profiler.getResult(sessionId);
    }

    @GetMapping("/cpu/{sessionId}/flamegraph")
    public String getFlameGraph(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "svg") String format
    ) {
        return profiler.generateFlameGraph(sessionId, format);
    }

    @PostMapping("/heap-dump")
    public HeapDumpResult triggerHeapDump() {
        return profiler.heapDump();
    }

    @GetMapping("/threads")
    public List<ThreadInfo> getThreadDump() {
        return profiler.threadDump();
    }
}
```

### Integração com Async Profiler

O J-Obs usa [async-profiler](https://github.com/async-profiler/async-profiler) internamente:

```java
@Component
public class AsyncProfilerIntegration {

    private final AsyncProfiler profiler = AsyncProfiler.getInstance();

    public void startCpuProfile(Duration duration) {
        profiler.execute(String.format(
            "start,event=cpu,interval=10ms,file=/tmp/profile-%d.jfr",
            System.currentTimeMillis()
        ));

        scheduler.schedule(() -> {
            profiler.execute("stop");
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public FlameGraph generateFlameGraph(String profileFile) {
        // Converte JFR para flame graph
        return FlameGraphConverter.convert(profileFile);
    }
}
```

---

## Sistema de Alertas

O J-Obs possui um sistema de alertas extensível com suporte a múltiplos provedores. Os alertas são configuráveis via UI ou YAML.

### Provedores Suportados

| Provedor | Tipo | Configuração |
|----------|------|--------------|
| **Email** | SMTP | Host, porta, credenciais, destinatários |
| **Telegram** | Bot API | Bot token, chat IDs |
| **Slack** | Webhook | Webhook URL, channel |
| **Discord** | Webhook | Webhook URL |
| **Microsoft Teams** | Webhook | Webhook URL |
| **PagerDuty** | API | Integration key, severity mapping |
| **Webhook Genérico** | HTTP | URL, headers, template |

### Arquitetura de Alertas

```
┌─────────────────────────────────────────────────────────────────┐
│                      Alert Engine                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │ Alert Rules  │───▶│  Evaluator   │───▶│  Dispatcher  │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│         │                   │                    │               │
│         ▼                   ▼                    ▼               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │  Conditions  │    │  Throttling  │    │  Providers   │       │
│  │  - Metric    │    │  - Rate Limit│    │  - Email     │       │
│  │  - Log       │    │  - Cooldown  │    │  - Telegram  │       │
│  │  - Trace     │    │  - Grouping  │    │  - Slack     │       │
│  │  - Health    │    │              │    │  - Webhook   │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Tipos de Alerta

#### 1. Alertas de Métrica
```yaml
- name: high-latency
  type: metric
  condition:
    metric: http_server_requests_seconds
    percentile: p99
    operator: ">"
    threshold: 2.0
    duration: 5m
  severity: warning
  providers: [telegram, email]
```

#### 2. Alertas de Log
```yaml
- name: error-spike
  type: log
  condition:
    level: ERROR
    count: "> 50"
    window: 1m
    pattern: ".*OutOfMemoryError.*"  # opcional
  severity: critical
  providers: [pagerduty, slack]
```

#### 3. Alertas de Trace
```yaml
- name: slow-database
  type: trace
  condition:
    span_name: "SELECT *"
    attribute: db.system
    value: postgresql
    duration: "> 5s"
  severity: warning
  providers: [telegram]
```

#### 4. Alertas de Health
```yaml
- name: database-down
  type: health
  condition:
    component: database
    status: DOWN
    duration: 30s
  severity: critical
  providers: [pagerduty, email, telegram]
```

### Interface de Configuração

```
┌─────────────────────────────────────────────────────────────────┐
│  J-Obs > Alertas > Configurar Provedores                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Provedores Configurados                                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ ✅ Telegram    @JobsAlertBot     2 chats    [Testar] [Edit] ││
│  │ ✅ Email       smtp.gmail.com    3 emails   [Testar] [Edit] ││
│  │ ⚪ Slack       Não configurado              [Configurar]    ││
│  │ ⚪ Discord     Não configurado              [Configurar]    ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  [+ Adicionar Webhook Customizado]                               │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  Regras de Alerta                                     [+ Nova]   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 🔴 high-error-rate    Métrica   Critical   Telegram, Email  ││
│  │ 🟡 slow-requests      Métrica   Warning    Telegram         ││
│  │ 🔴 database-down      Health    Critical   PagerDuty        ││
│  │ 🟢 memory-usage       Métrica   Info       Slack            ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Implementação do Provider (SPI)

```java
public interface AlertProvider {

    String getName();

    boolean isConfigured();

    CompletableFuture<AlertResult> send(Alert alert);

    AlertProviderConfig getConfigSchema();

    void configure(Map<String, Object> config);

    default boolean supportsTest() { return true; }

    default CompletableFuture<TestResult> test() {
        return send(Alert.testAlert()).thenApply(TestResult::fromAlertResult);
    }
}

// Registro via ServiceLoader
// META-INF/services/io.github.jobs.alert.AlertProvider
```

### Exemplo: Telegram Provider

```java
@Component
public class TelegramAlertProvider implements AlertProvider {

    private String botToken;
    private List<String> chatIds;

    @Override
    public CompletableFuture<AlertResult> send(Alert alert) {
        var message = formatMessage(alert);

        return chatIds.stream()
            .map(chatId -> sendMessage(chatId, message))
            .reduce(CompletableFuture.completedFuture(AlertResult.success()),
                (a, b) -> a.thenCombine(b, AlertResult::merge));
    }

    private String formatMessage(Alert alert) {
        return """
            %s *%s*

            %s

            Severity: `%s`
            Time: `%s`
            Service: `%s`
            """.formatted(
                alert.severity().emoji(),
                alert.name(),
                alert.message(),
                alert.severity(),
                alert.timestamp(),
                alert.serviceName()
            );
    }
}
```

### Throttling e Agrupamento

| Configuração | Descrição | Default |
|--------------|-----------|---------|
| `rate_limit` | Máximo de alertas por período | 10/min |
| `cooldown` | Tempo mínimo entre alertas iguais | 5min |
| `grouping` | Agrupa alertas similares | true |
| `group_wait` | Tempo de espera para agrupar | 30s |
| `repeat_interval` | Re-envio se não resolvido | 4h |

### Histórico de Alertas

- Todos os alertas são persistidos localmente
- Visualização de timeline na UI
- Filtros por severidade, provider, status
- Export para CSV/JSON
- Retenção configurável (default: 7 dias)

---

## Configuração

```yaml
j-obs:
  enabled: true
  path: /j-obs
  security:
    enabled: true
    users:
      - username: admin
        password: ${J_OBS_PASSWORD}
  logs:
    buffer-size: 10000
    retention: 1h
  traces:
    sample-rate: 1.0
    max-spans: 1000
  metrics:
    export:
      prometheus:
        enabled: true

  # Configuração de Alertas
  alerts:
    enabled: true
    evaluation-interval: 15s
    throttling:
      rate-limit: 10/min
      cooldown: 5m
      grouping: true
      group-wait: 30s

    # Provedores
    providers:
      email:
        enabled: true
        host: ${SMTP_HOST}
        port: 587
        username: ${SMTP_USER}
        password: ${SMTP_PASSWORD}
        from: alerts@myapp.com
        to:
          - team@myapp.com
          - oncall@myapp.com

      telegram:
        enabled: true
        bot-token: ${TELEGRAM_BOT_TOKEN}
        chat-ids:
          - "-1001234567890"
          - "-1009876543210"

      slack:
        enabled: false
        webhook-url: ${SLACK_WEBHOOK_URL}
        channel: "#alerts"

      webhook:
        enabled: false
        url: https://my-webhook.com/alerts
        headers:
          Authorization: "Bearer ${WEBHOOK_TOKEN}"
        template: |
          {
            "alert": "{{name}}",
            "severity": "{{severity}}",
            "message": "{{message}}"
          }

    # Regras de Alerta
    rules:
      - name: high-error-rate
        type: metric
        condition:
          metric: http_server_requests_seconds_count
          tag: status=5xx
          operator: ">"
          threshold: 10
          window: 1m
        severity: critical
        providers: [telegram, email]

      - name: high-latency
        type: metric
        condition:
          metric: http_server_requests_seconds
          percentile: p99
          operator: ">"
          threshold: 2.0
          duration: 5m
        severity: warning
        providers: [telegram]

      - name: database-down
        type: health
        condition:
          component: db
          status: DOWN
          duration: 30s
        severity: critical
        providers: [telegram, email, slack]
```
