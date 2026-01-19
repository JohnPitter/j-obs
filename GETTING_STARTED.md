# J-Obs - Guia de Integração

Este guia mostra como integrar o J-Obs em uma aplicação Spring Boot existente para obter observabilidade completa (logs, traces, métricas e health checks) com uma interface web.

## Índice

1. [Requisitos](#requisitos)
2. [Instalação Rápida](#instalação-rápida)
3. [Configuração](#configuração)
4. [Acessando o Dashboard](#acessando-o-dashboard)
5. [Instrumentação Customizada](#instrumentação-customizada)
6. [Exemplos Práticos](#exemplos-práticos)
7. [Troubleshooting](#troubleshooting)

---

## Requisitos

- Java 17+
- Spring Boot 3.0+
- Maven ou Gradle

---

## Instalação Rápida

### Passo 1: Adicionar Dependência

**Maven (`pom.xml`):**

```xml
<dependency>
    <groupId>io.github.j-obs</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle (`build.gradle`):**

```groovy
implementation 'io.github.j-obs:j-obs-spring-boot-starter:1.0.0-SNAPSHOT'
```

### Passo 2: Adicionar Dependências Obrigatórias

O J-Obs requer algumas dependências para funcionar. Adicione as que ainda não existem no seu projeto:

**Maven:**

```xml
<!-- Spring Boot Actuator (obrigatório) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- OpenTelemetry (obrigatório para traces) -->
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

<!-- Micrometer (obrigatório para métricas) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Logback (opcional, mas recomendado para logs) -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
</dependency>
```

### Passo 3: Configurar Actuator

No `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      show-components: always
```

### Passo 4: Iniciar a Aplicação

```bash
mvn spring-boot:run
```

Acesse: **http://localhost:8080/j-obs**

---

## Configuração

### Configuração Completa (`application.yml`)

```yaml
j-obs:
  # Habilita/desabilita o J-Obs
  enabled: true

  # Path base para todos os endpoints J-Obs
  path: /j-obs

  # Intervalo de cache para verificação de dependências
  check-interval: 5m

  # Configurações do Dashboard
  dashboard:
    refresh-interval: 5s
    theme: system  # system, light, dark

  # Configurações de Traces
  traces:
    enabled: true
    max-traces: 10000        # Máximo de traces em memória
    retention: 1h            # Tempo de retenção
    sample-rate: 1.0         # Taxa de amostragem (1.0 = 100%)

  # Configurações de Logs
  logs:
    enabled: true
    max-entries: 10000       # Máximo de logs em memória
    min-level: INFO          # Nível mínimo (TRACE, DEBUG, INFO, WARN, ERROR)

  # Configurações de Métricas
  metrics:
    enabled: true
    refresh-interval: 5000   # Intervalo de coleta (ms)
    max-history-points: 360  # Pontos de histórico por métrica

  # Configurações de Health Checks
  health:
    enabled: true
    max-history-entries: 100 # Histórico de mudanças de status
```

### Configuração Mínima

Para começar rapidamente, apenas adicione:

```yaml
j-obs:
  enabled: true
```

---

## Acessando o Dashboard

Após iniciar a aplicação, os seguintes endpoints estarão disponíveis:

| URL | Descrição |
|-----|-----------|
| `/j-obs` | Dashboard principal com verificação de requisitos |
| `/j-obs/traces` | Visualização de traces (waterfall) |
| `/j-obs/logs` | Logs em tempo real (WebSocket) |
| `/j-obs/metrics` | Métricas com gráficos |
| `/j-obs/health` | Status dos health checks |

### APIs REST

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/j-obs/api/traces` | GET | Lista traces |
| `/j-obs/api/traces/{id}` | GET | Detalhes de um trace |
| `/j-obs/api/logs` | GET | Lista logs |
| `/j-obs/api/metrics` | GET | Lista métricas |
| `/j-obs/api/health` | GET | Status de saúde |
| `/j-obs/api/requirements` | GET | Status das dependências |

---

## Instrumentação Automática (Recomendado)

O J-Obs oferece instrumentação automática via annotations. **Não é necessário criar spans ou métricas manualmente!**

### @Observable - Observabilidade Completa

```java
import io.github.jobs.annotation.Observable;

@Service
@Observable  // Todos os métodos públicos são instrumentados!
public class OrderService {

    public Order create(OrderRequest request) {
        // Automaticamente cria:
        // - Span: "OrderService.create"
        // - Timer: "method.timed" com tags class=OrderService, method=create
        // - Counter: "method.calls" para contagem de invocações
        // - Counter: "method.exceptions" se ocorrer erro
        return new Order(...);
    }

    public Order process(String orderId) {
        // Também instrumentado automaticamente!
        return findAndProcess(orderId);
    }
}
```

### @Traced - Apenas Traces

```java
import io.github.jobs.annotation.Traced;

@Service
@Traced  // Apenas cria spans (sem métricas)
public class PaymentService {

    public void process(Payment payment) {
        // Cria span: "PaymentService.process"
    }

    @Traced(name = "validate-card", attributes = {"payment.type=card"})
    public void validateCard(String cardNumber) {
        // Span com nome e atributos customizados
    }
}
```

### @Measured - Apenas Métricas

```java
import io.github.jobs.annotation.Measured;

@Service
@Measured  // Apenas cria métricas (sem traces)
public class ReportService {

    public Report generate(ReportRequest request) {
        // Cria métricas de tempo e contagem
    }

    @Measured(name = "report.export", percentiles = {0.5, 0.95, 0.99})
    public void export(Report report, String format) {
        // Métricas com percentis customizados
    }
}
```

### Comparação das Annotations

| Annotation | Traces | Métricas | Uso |
|------------|--------|----------|-----|
| `@Observable` | ✅ | ✅ | Recomendado para a maioria dos casos |
| `@Traced` | ✅ | ❌ | Quando só precisa de traces |
| `@Measured` | ❌ | ✅ | Quando só precisa de métricas |

### Instrumentação HTTP Automática

Todas as requisições HTTP são instrumentadas automaticamente:

```
GET /api/orders/123
├── Span: "GET /api/orders/{id}"
│   ├── http.method: GET
│   ├── http.url: http://localhost:8080/api/orders/123
│   ├── http.status_code: 200
│   └── http.user_agent: ...
└── Metrics: http.server.requests (timer)
```

### Configuração

```yaml
j-obs:
  instrumentation:
    # @Traced annotation (default: true)
    traced-annotation: true

    # @Measured annotation (default: true)
    measured-annotation: true

    # HTTP request tracing (default: true)
    http-tracing: true

    # Auto-instrument ALL @Service/@Repository/@Component
    # Mesmo sem annotations (default: false)
    auto-instrument: false
```

---

## Instrumentação Manual (Avançado)

Para casos onde você precisa de controle total, ainda pode usar a API diretamente.

### Traces Manuais

```java
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

@Service
public class MeuService {

    private final Tracer tracer;

    public MeuService(Tracer tracer) {
        this.tracer = tracer;
    }

    public void operacaoComplexa() {
        Span span = tracer.spanBuilder("operacao-complexa")
            .setAttribute("custom.attribute", "value")
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Sua lógica
            span.addEvent("etapa-concluida");
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Métricas Manuais

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class MeuService {

    private final Counter operacoes;

    public MeuService(MeterRegistry registry) {
        this.operacoes = Counter.builder("minhas.operacoes")
            .tag("tipo", "custom")
            .register(registry);
    }

    public void executar() {
        operacoes.increment();
    }
}
```

### Adicionando Health Checks Customizados

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("meuBancoDeDados")
public class MeuBancoHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Verificar conexão
            boolean conectado = verificarConexao();
            long tempoResposta = medirTempoResposta();

            if (conectado && tempoResposta < 1000) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("host", "localhost:5432")
                    .withDetail("responseTime", tempoResposta + "ms")
                    .withDetail("connectionPool", "active=5, idle=10")
                    .build();
            } else if (conectado) {
                return Health.status("DEGRADED")
                    .withDetail("warning", "Alta latência detectada")
                    .withDetail("responseTime", tempoResposta + "ms")
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Não foi possível conectar")
                    .build();
            }

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private boolean verificarConexao() {
        // Implementar verificação
        return true;
    }

    private long medirTempoResposta() {
        // Implementar medição
        return 50;
    }
}
```

### Usando Logs Estruturados

O J-Obs captura automaticamente logs do Logback. Para melhor correlação:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Service
public class MeuService {

    private static final Logger log = LoggerFactory.getLogger(MeuService.class);

    public void processar(String pedidoId, String clienteId) {
        // Adicionar contexto ao MDC (aparece nos logs)
        MDC.put("pedidoId", pedidoId);
        MDC.put("clienteId", clienteId);

        try {
            log.info("Iniciando processamento do pedido");

            // Diferentes níveis de log
            log.debug("Detalhes internos: valor={}", calcularValor());
            log.info("Pedido processado com sucesso");
            log.warn("Cache miss detectado");
            log.error("Erro ao processar", new RuntimeException("Exemplo"));

        } finally {
            // Limpar MDC
            MDC.remove("pedidoId");
            MDC.remove("clienteId");
        }
    }
}
```

---

## Exemplos Práticos

### Exemplo 1: API REST com Observabilidade Completa

```java
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoService pedidoService;
    private final Tracer tracer;
    private final Counter requestsTotal;

    public PedidoController(PedidoService pedidoService,
                           Tracer tracer,
                           MeterRegistry registry) {
        this.pedidoService = pedidoService;
        this.tracer = tracer;
        this.requestsTotal = Counter.builder("api.requests.total")
            .tag("endpoint", "/api/pedidos")
            .register(registry);
    }

    @PostMapping
    public ResponseEntity<Pedido> criar(@RequestBody CriarPedidoRequest request) {
        requestsTotal.increment();

        Span span = tracer.spanBuilder("POST /api/pedidos")
            .setAttribute("cliente.id", request.clienteId())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            MDC.put("clienteId", request.clienteId());

            log.info("Recebida requisição para criar pedido");

            Pedido pedido = pedidoService.criar(request);

            span.setAttribute("pedido.id", pedido.getId());
            log.info("Pedido {} criado com sucesso", pedido.getId());

            return ResponseEntity.ok(pedido);

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            log.error("Erro ao criar pedido: {}", e.getMessage(), e);
            throw e;

        } finally {
            MDC.clear();
            span.end();
        }
    }
}
```

### Exemplo 2: Service com Múltiplas Dependências

```java
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final Tracer tracer;
    private final Timer dbTimer;
    private final Timer apiTimer;

    public PedidoService(Tracer tracer, MeterRegistry registry) {
        this.tracer = tracer;
        this.dbTimer = Timer.builder("db.query.time").register(registry);
        this.apiTimer = Timer.builder("external.api.time").register(registry);
    }

    public Pedido criar(CriarPedidoRequest request) {
        Span span = tracer.spanBuilder("PedidoService.criar").startSpan();

        try (Scope scope = span.makeCurrent()) {

            // 1. Validar estoque
            validarEstoque(request.itens());

            // 2. Calcular frete
            BigDecimal frete = calcularFrete(request.endereco());

            // 3. Processar pagamento
            processarPagamento(request.pagamento());

            // 4. Salvar pedido
            Pedido pedido = salvarPedido(request, frete);

            // 5. Notificar
            enviarNotificacao(pedido);

            return pedido;

        } finally {
            span.end();
        }
    }

    private void validarEstoque(List<Item> itens) {
        Span span = tracer.spanBuilder("validarEstoque")
            .setAttribute("itens.count", itens.size())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.debug("Validando estoque para {} itens", itens.size());
            // Lógica de validação
        } finally {
            span.end();
        }
    }

    private BigDecimal calcularFrete(Endereco endereco) {
        return apiTimer.record(() -> {
            Span span = tracer.spanBuilder("calcularFrete")
                .setAttribute("http.method", "POST")
                .setAttribute("http.url", "https://frete-api.com/calcular")
                .startSpan();

            try (Scope scope = span.makeCurrent()) {
                log.debug("Calculando frete para CEP {}", endereco.cep());
                // Chamar API de frete
                return new BigDecimal("25.00");
            } finally {
                span.end();
            }
        });
    }

    private void processarPagamento(Pagamento pagamento) {
        Span span = tracer.spanBuilder("processarPagamento")
            .setAttribute("pagamento.tipo", pagamento.tipo())
            .setAttribute("pagamento.valor", pagamento.valor().doubleValue())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Processando pagamento de R$ {}", pagamento.valor());
            // Lógica de pagamento
        } finally {
            span.end();
        }
    }

    private Pedido salvarPedido(CriarPedidoRequest request, BigDecimal frete) {
        return dbTimer.record(() -> {
            Span span = tracer.spanBuilder("salvarPedido")
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation", "INSERT")
                .startSpan();

            try (Scope scope = span.makeCurrent()) {
                log.debug("Salvando pedido no banco de dados");
                // Salvar no banco
                return new Pedido(/* ... */);
            } finally {
                span.end();
            }
        });
    }

    private void enviarNotificacao(Pedido pedido) {
        Span span = tracer.spanBuilder("enviarNotificacao")
            .setAttribute("notification.type", "email")
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.debug("Enviando notificação para cliente");
            // Enviar email/push
        } finally {
            span.end();
        }
    }
}
```

---

## Troubleshooting

### Problema: Dashboard mostra "Dependência não encontrada"

**Solução:** Verifique se todas as dependências obrigatórias estão no classpath:

```xml
<!-- Verificar se estas dependências existem -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Problema: Traces não aparecem

**Solução 1:** Verifique se o Tracer está sendo injetado corretamente:

```java
@Autowired
private Tracer tracer;  // Deve estar disponível
```

**Solução 2:** Verifique a configuração:

```yaml
j-obs:
  traces:
    enabled: true
    sample-rate: 1.0  # 100% das requisições
```

### Problema: Logs não aparecem em tempo real

**Solução 1:** Verifique se o WebSocket está funcionando:
- Abra o console do navegador (F12)
- Vá para a aba Network > WS
- Deve haver uma conexão para `/j-obs/ws/logs`

**Solução 2:** Verifique o nível de log:

```yaml
j-obs:
  logs:
    min-level: DEBUG  # Capturar todos os níveis
```

### Problema: Métricas customizadas não aparecem

**Solução:** Aguarde o próximo ciclo de refresh ou force:

```bash
curl -X POST http://localhost:8080/j-obs/api/metrics/refresh
```

### Problema: Health check customizado não aparece

**Solução:** Verifique se a classe está anotada com `@Component`:

```java
@Component("nomeDoIndicador")  // Nome aparece no dashboard
public class MeuHealthIndicator implements HealthIndicator {
    // ...
}
```

---

## Segurança

### Modelo de Segurança do J-Obs

O J-Obs é projetado para ser usado em ambientes de desenvolvimento e staging. A API REST é **stateless** e não utiliza sessões ou cookies para autenticação.

#### CSRF Protection

**A API J-Obs não requer proteção CSRF** pelos seguintes motivos:

1. **Stateless**: A API não mantém estado de sessão no servidor
2. **Sem cookies de autenticação**: Não há cookies de sessão que poderiam ser explorados
3. **Operações de leitura**: A maioria das operações são de leitura (GET)
4. **Ambiente controlado**: Destinado a ambientes de desenvolvimento/staging

Se você precisar expor o J-Obs em produção, recomendamos:
- Usar autenticação via proxy reverso (nginx, Traefik)
- Restringir acesso por IP
- Usar VPN ou rede privada

#### Rate Limiting

O J-Obs inclui rate limiting por padrão para proteção contra DoS:

```yaml
j-obs:
  rate-limiting:
    enabled: true
    max-requests: 100    # Máximo de requests
    window: 1m           # Janela de tempo
```

#### Sanitização de Input

Todos os parâmetros de busca são sanitizados para prevenir:
- Injection attacks
- ReDoS (Regular Expression Denial of Service)
- Resource exhaustion

#### Validação de URLs (SSRF Prevention)

URLs de webhook são validadas para bloquear:
- `localhost` e `127.0.0.1`
- IPs privados (10.x.x.x, 172.16-31.x.x, 192.168.x.x)
- IPs link-local (169.254.x.x)
- IPv6 loopback (`::1`)

#### Configuração Segura Recomendada

Para ambientes mais seguros, configure:

```yaml
j-obs:
  enabled: true

  # Rate limiting
  rate-limiting:
    enabled: true
    max-requests: 50
    window: 1m

  # Alertas - Use HTTPS para webhooks
  alerts:
    http-timeout: 30s
    providers:
      webhook:
        url: https://secure-webhook.internal/alerts  # HTTPS obrigatório
```

#### Proxying Seguro

Se precisar expor o J-Obs externamente, use um proxy reverso com autenticação:

**Nginx:**
```nginx
location /j-obs {
    auth_basic "J-Obs Admin";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://localhost:8080;
}
```

**Traefik (com BasicAuth):**
```yaml
http:
  middlewares:
    j-obs-auth:
      basicAuth:
        users:
          - "admin:$apr1$..."
  routers:
    j-obs:
      rule: "PathPrefix(`/j-obs`)"
      middlewares:
        - j-obs-auth
```

---

## Próximos Passos

1. **Explore o Dashboard**: Navegue pelas diferentes seções
2. **Gere Carga**: Use ferramentas como `ab` ou `wrk` para gerar tráfego
3. **Adicione Instrumentação**: Adicione traces e métricas aos seus serviços
4. **Configure Alertas**: (Em breve) Configure alertas baseados em métricas

## Suporte

- GitHub Issues: https://github.com/j-obs/j-obs/issues
- Documentação: https://j-obs.github.io

---

**J-Obs** - Observabilidade simples para Spring Boot
