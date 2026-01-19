# J-Obs - Melhorias Pendentes

Este arquivo lista as melhorias identificadas que ainda precisam ser implementadas.

## Legenda de Prioridade
- 🔴 **HIGH** - Alta prioridade
- 🟡 **MEDIUM** - Média prioridade
- 🟢 **LOW** - Baixa prioridade
- 🔵 **FEATURE** - Funcionalidade nova
- ✅ **DONE** - Concluído

---

## Segurança

### ✅ DONE - Rate Limiting nos Controllers
**Arquivo:** `RateLimiter.java`, `RateLimitInterceptor.java`
**Implementado:** Rate limiter com sliding window algorithm e interceptor Spring MVC.
**Configuração:** `j-obs.rate-limiting.max-requests`, `j-obs.rate-limiting.window`

### ✅ DONE - Sanitização de Input
**Arquivo:** `InputSanitizer.java`, Controllers
**Implementado:** Sanitização de todos os parâmetros de busca (logger, message, traceId, etc).
**Validações:** Tamanho máximo, caracteres permitidos, regex escaping.

### ✅ DONE - Timeout Configurável em HTTP Clients
**Arquivo:** `JObsProperties.Alerts`, Providers
**Implementado:** Timeout configurável via `j-obs.alerts.http-timeout`.
**Default:** 30 segundos.

### ✅ DONE - Validação de URLs nos Providers
**Arquivo:** `UrlValidator.java`, Webhook Providers
**Implementado:** Validação contra SSRF (bloqueio de IPs privados, localhost, etc).
**Suporte:** Whitelist de domínios conhecidos (Telegram, Slack, Teams).

### ✅ DONE - CSRF Protection Documentation
**Arquivo:** `GETTING_STARTED.md`
**Implementado:** Seção "Segurança" explicando:
- Modelo stateless da API (não requer CSRF)
- Rate limiting configurável
- Sanitização de input
- Validação de URLs (SSRF prevention)
- Configuração segura recomendada
- Proxying com autenticação (nginx, Traefik)

---

## Performance

### ✅ DONE - Cache de Métricas
**Arquivo:** `CachedMetricRepository.java`
**Implementado:** Cache decorator para MetricRepository com TTL configurável.
**Cache de:** stats(), getMetricNames(), getCategories(), getTagKeys(), query().

### ✅ DONE - Paginação em LogRepository.query()
**Arquivo:** `InMemoryLogRepository.java`
**Implementado:** Paginação eficiente diretamente no buffer circular.
**Otimizações:**
- Iteração em ordem reversa sem criar lista intermediária
- Aplicação de filtro, offset e limit durante iteração
- Método `count(LogQuery)` também otimizado

### ✅ DONE - Lazy Loading de Trace Spans
**Arquivo:** `TraceApiController.java`
**Implementado:** Carregamento lazy de spans com paginação.
**Funcionalidades:**
- Parâmetro `includeSpans` (default: true) no endpoint `GET /traces/{traceId}`
- Parâmetro `maxSpans` para limitar número de spans retornados
- Endpoint `/traces/{traceId}/spans` com paginação (limit, offset)
- Resposta inclui `hasMore` e `hasMoreSpans` para indicar mais dados
- 4 novos testes de integração

### ✅ DONE - Compressão WebSocket
**Arquivos:** `JObsWebSocketConfiguration.java`, `JObsProperties.java`
**Implementado:** Configuração de WebSocket com suporte a compressão.
**Funcionalidades:**
- `JObsWebSocketConfiguration` para configurar container WebSocket
- Propriedades `j-obs.logs.websocket.*` para configuração
- `compression-enabled` (default: true) - habilita permessage-deflate
- `max-text-message-size` (default: 65536) - tamanho máximo de mensagem
- `send-buffer-size` (default: 16384) - tamanho do buffer
- Permessage-deflate habilitado por padrão em Tomcat 8.5+ e Jetty 9.4+

### 🟢 LOW - Object Pooling para LogEntry
**Arquivo:** `JObsLogAppender.java`
**Problema:** Cria novo LogEntry para cada log, pressão no GC.
**Solução:** Usar object pool para reduzir alocações.

---

## Infraestrutura

### ✅ DONE - Graceful Shutdown
**Arquivo:** `JObsTraceAutoConfiguration.java`
**Implementado:** Bean com `destroyMethod = "shutdown"` para TraceRepository.

### ✅ DONE - Health Indicator para J-Obs
**Arquivo:** `JObsHealthIndicator.java`, `JObsActuatorAutoConfiguration.java`
**Implementado:** Health indicator que reporta:
- Status dos repositórios (traces, logs, metrics, alertEvents)
- Uso de capacidade (com alertas para >80% e >95%)
- Estimativa de uso de memória

### 🟢 LOW - Métricas do J-Obs
**Descrição:** Expor métricas internas do J-Obs (logs processados, traces armazenados, etc).
**Arquivos a modificar:**
- `JObsAutoConfiguration.java`

---

## Qualidade de Código

### ✅ DONE - Testes Unitários para Código Novo
**Arquivos:** Vários arquivos de teste
**Implementado:** Testes para:
- `RateLimiter` - sliding window, cleanup, rate limiting
- `InputSanitizer` - todas as validações (logger, message, traceId, etc)
- `UrlValidator` - SSRF prevention (private IPs, localhost, IPv6)
- `CachedMetricRepository` - cache e invalidação
- `JObsHealthIndicator` - status reporting, degraded, down
- `AlertThrottler` - tryAcquire, cooldown, rate limiting
- `AlertGroup`, `AlertGroupKey`, `AlertGrouper` - grouping logic
**Total:** 525 testes passando (364 core + 161 starter)

### 🟡 MEDIUM - Documentação JavaDoc
**Arquivos:** Providers, Controllers
**Problema:** Classes públicas sem JavaDoc adequado.
**Solução:** Adicionar JavaDoc em todas as classes e métodos públicos.

### 🟢 LOW - Logging Consistente
**Arquivos:** Vários providers
**Problema:** Níveis de log inconsistentes entre providers.
**Solução:** Padronizar: DEBUG para operações normais, INFO para eventos importantes, WARN para problemas recuperáveis, ERROR para falhas.

### 🟢 LOW - Constantes Mágicas
**Arquivos:** `InMemoryLogRepository.java`, `InMemoryTraceRepository.java`
**Problema:** Valores como 10000, 1000 hardcoded.
**Solução:** Extrair para constantes nomeadas ou configuração.

---

## Funcionalidades (conforme CLAUDE.md)

### ✅ DONE - SQL Analyzer
**Descrição:** Análise automática de queries SQL para identificar N+1, slow queries, missing indexes.
**Arquivos:** `SqlAnalyzer.java`, `SqlProblem.java`, `SqlProblemType.java`, `SqlQuery.java`, `DefaultSqlAnalyzer.java`, `SqlAnalyzerApiController.java`
**Funcionalidades:**
- Detecção de N+1 queries
- Identificação de slow queries
- Detecção de SELECT * anti-pattern
- Análise de missing LIMIT clauses
- API REST completa

### ✅ DONE - Anomaly Detection
**Descrição:** Detecção automática de anomalias usando Z-Score, Moving Average.
**Arquivos:** `Anomaly.java`, `AnomalyType.java`, `AnomalySeverity.java`, `PossibleCause.java`, `DefaultAnomalyDetector.java`
**Funcionalidades:**
- Z-score based latency spike detection
- Moving average for traffic anomalies
- Error rate spike detection
- Automatic cause correlation

### ✅ DONE - SLO/SLI Tracking
**Descrição:** Definição e acompanhamento de Service Level Objectives.
**Arquivos:** `Slo.java`, `Sli.java`, `SliType.java`, `SloStatus.java`, `ErrorBudget.java`, `BurnRate.java`, `SloEvaluation.java`, `DefaultSloService.java`, `SloApiController.java`
**Funcionalidades:**
- SLI types: AVAILABILITY, LATENCY, ERROR_RATE, THROUGHPUT
- Error budget calculation with remaining percentage
- Burn rate with severity levels (safe, elevated, high, critical)
- Multi-window burn rate alerts
- Periodic evaluation with Micrometer integration
- 71 testes

### ✅ DONE - Profiling
**Descrição:** CPU e Memory profiling sob demanda.
**Arquivos:** `ProfileType.java`, `ProfileStatus.java`, `ProfileSession.java`, `ProfileResult.java`, `CpuSample.java`, `FlameGraphNode.java`, `MemoryInfo.java`, `ThreadDump.java`, `ProfilingService.java`, `DefaultProfilingService.java`, `ProfilingApiController.java`
**Funcionalidades:**
- CPU profiling com duração e intervalo configuráveis
- Memory snapshot com detalhes de heap, pools e GC
- Thread dump com análise de estados e deadlocks
- Flame graph para visualização de CPU hotspots
- 55 testes

### ✅ DONE - Service Map
**Descrição:** Visualização gráfica das dependências entre serviços.
**Arquivos:** `ServiceNode.java`, `ServiceConnection.java`, `ServiceMap.java`, `DefaultServiceMapBuilder.java`, `ServiceMapApiController.java`
**Funcionalidades:**
- SVG-based interactive visualization
- Node health calculation based on error rates and latency
- Connection statistics (RPS, error rate, latency percentiles)
- Auto-discovery from traces

### ✅ DONE - Alert Grouping
**Arquivos:** `AlertGroup.java`, `AlertGroupKey.java`, `AlertGroupStatus.java`, `AlertGrouper.java`
**Implementado:** Agrupamento de alertas similares por nome, severidade e labels configuráveis.
**Funcionalidades:**
- Agrupamento por alertName + severity + labels configuráveis
- Tempo de espera configurável antes de enviar grupo (`group-wait`)
- Tamanho máximo de grupo (`max-group-size`)
- Flush automático quando grupo atinge tamanho máximo
- Flush automático após tempo de espera
- Mensagem de resumo para grupos com múltiplos alertas

### 🔵 FEATURE - Alert Acknowledge/Resolve
**Descrição:** Permitir acknowledge e resolve de alertas via UI.
**Status:** API existe, UI incompleta
**Arquivos a modificar:**
- `alerts.html` (template)

### 🔵 FEATURE - Dark/Light Mode Toggle
**Descrição:** Toggle manual de tema além do automático.
**Status:** Automático funciona, toggle manual não existe
**Arquivos a modificar:**
- Templates HTML
- `JObsProperties.Dashboard`

---

## Resumo de Progresso

### Concluídos (18 itens)
1. ✅ Rate Limiting nos Controllers
2. ✅ Sanitização de Input
3. ✅ Timeout Configurável em HTTP Clients
4. ✅ Validação de URLs nos Providers
5. ✅ Cache de Métricas
6. ✅ Graceful Shutdown
7. ✅ Health Indicator para J-Obs
8. ✅ Paginação eficiente no LogRepository
9. ✅ Testes unitários para código novo (525 testes)
10. ✅ Documentação CSRF e Segurança
11. ✅ Lazy Loading de Trace Spans
12. ✅ Compressão WebSocket
13. ✅ SQL Analyzer
14. ✅ Anomaly Detection
15. ✅ SLO/SLI Tracking
16. ✅ Service Map
17. ✅ Profiling
18. ✅ Alert Grouping

### Pendentes

| Categoria | Itens | Complexidade |
|-----------|-------|--------------|
| Performance | 1 | Baixa |
| Qualidade | 3 | Baixa-Média |
| Features | 2 | Média |
| Infraestrutura | 1 | Baixa |

**Total:** 7 itens pendentes (reduzido de 25)

---

## Próximos Passos Recomendados

1. **Prioridade 1:** Documentação JavaDoc
2. **Prioridade 2:** Alert Acknowledge/Resolve (UI)
3. **Prioridade 3:** Dark/Light Mode Toggle
