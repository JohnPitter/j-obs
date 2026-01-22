# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.13] - 2026-01-21

### Fixed
- **CRITICAL: JavaScript Error in Dashboard Templates** - Fixed missing semicolon after `tailwind.config` object
  - Affected pages: traces-list, logs, metrics, health, alerts, layout, login, requirements, index, tools-page-wrapper
  - Error: `Uncaught TypeError: {(intermediate value)(intermediate value)} is not a function`
  - Cause: JavaScript interpreted `} (function() {` as trying to call an object as a function
  - All 10 HTML templates now have correct semicolon placement

## [1.0.12] - 2026-01-20

### Fixed
- **Dependency Management Strategy** - Removed explicit Micrometer versions from parent POM
  - Micrometer versions are now fully managed by Spring Boot BOM
  - Prevents `NoSuchMethodError` in Prometheus classes when using different Spring Boot versions
  - Fixes `ExporterProperties.getPrometheusTimestampsInMs()` error with Spring Boot 3.4.x

### Added
- **OnServerContainerCondition Tests** - Comprehensive test coverage for WebSocket conditional loading
  - 7 test cases covering all scenarios (MockServletContext, real container, null context, etc.)
  - Validates automatic detection of test environments

### Changed
- **Documentation Improvements**
  - README now recommends `j-obs-bom` over `j-obs-parent` for dependency management
  - Added detailed compatibility matrix with Spring Boot and Micrometer versions
  - New "Dependency Version Strategy" section explaining best practices
  - TROUBLESHOOTING.md includes Prometheus version conflict workaround with 3 solutions
  - Quick reference table updated with Prometheus error resolution

### Important
- **Users should use `j-obs-bom`** (not `j-obs-parent`) when importing J-Obs
  - `j-obs-bom` only manages J-Obs module versions
  - Does NOT override Spring Boot's dependency management
  - Prevents version conflicts with Micrometer and Prometheus

## [1.0.11] - 2026-01-20

### Fixed
- **Complete J-Obs Disable with `j-obs.enabled=false`** - Setting this property now properly disables ALL J-Obs auto-configurations
  - Previously, some configurations only checked their own sub-properties (e.g., `j-obs.logs.enabled`)
  - Now ALL auto-configurations check `j-obs.enabled` first
  - This fixes MockServletContext test failures when trying to disable J-Obs
  - Affected configurations: `JObsLogAutoConfiguration`, `JObsWebSocketConfiguration`, `JObsTraceAutoConfiguration`, `JObsMetricAutoConfiguration`, `JObsHealthAutoConfiguration`, `JObsAlertAutoConfiguration`, `JObsAlertNotificationAutoConfiguration`, `JObsSqlAnalyzerAutoConfiguration`, `JObsAnomalyDetectionAutoConfiguration`, `JObsProfilingAutoConfiguration`, `JObsToolsAutoConfiguration`, `JObsTraceExportAutoConfiguration`, `JObsInternalMetricsAutoConfiguration`, `JObsWebFluxLogAutoConfiguration`

- **J-Obs BOM No Longer Pins Third-Party Versions** - The BOM now only manages J-Obs module versions
  - Removed Spring Boot BOM import from j-obs-bom
  - Removed explicit Micrometer version pinning
  - Users' Spring Boot version will now manage Micrometer and other dependency versions
  - This prevents version conflicts when using J-Obs with Spring Boot 3.3.x and 3.4.x
  - Previously, j-obs-bom pinned Micrometer 1.12.0 which conflicted with Spring Boot 3.4.x (Micrometer 1.14.x)

### Changed
- Updated documentation in TROUBLESHOOTING.md with:
  - Information about fully disabling J-Obs with `j-obs.enabled=false`
  - Clarification that j-obs-bom only manages J-Obs versions, not third-party dependencies
  - Added Micrometer version conflict troubleshooting section

## [1.0.10] - 2026-01-20

### Added
- **ConditionalOnServerContainer Annotation** - Custom condition for WebSocket configuration
  - `OnServerContainerCondition` checks for actual `ServerContainer` availability in `ServletContext`
  - Prevents WebSocket auto-configuration from loading in test environments with `MockServletContext`
- **Alternative Class Names Support** - Dependency checking now supports libraries with changed package names
  - `Dependency.alternativeClassNames()` allows specifying fallback class names
  - Supports all compatible versions of dependencies regardless of package changes

### Fixed
- **MockServletContext Test Failures** - Fixed `ServerContainer not available` errors in Spring Boot tests
  - WebSocket configuration now only loads when a real `ServerContainer` is present
  - Tests using `@SpringBootTest` without `WebEnvironment.RANDOM_PORT` now work correctly
  - No manual workaround required (previously needed `j-obs.logs.websocket.enabled=false`)
- **Micrometer 1.13+ Compatibility** - Fixed dependency check failing with newer Micrometer versions
  - Micrometer 1.13 changed package from `io.micrometer.prometheus` to `io.micrometer.prometheusmetrics`
  - Now checks both old and new class names to support all Micrometer versions
  - Detected version is displayed correctly on the requirements page

## [1.0.9] - 2026-01-20

### Added
- **Automatic Observability Conflict Resolution** - J-Obs now automatically handles conflicts with Spring Boot Actuator
  - `JObsObservabilityAutoConfiguration` for automatic handling of observability conflicts
  - Auto-configures `@Scheduled` tasks with `ObservationRegistry.NOOP` to prevent TracingContext errors
  - New configuration: `j-obs.observability.scheduled-tasks-noop` (default: true)

### Fixed
- **GlobalOpenTelemetry Conflict** - Fixed `IllegalStateException: GlobalOpenTelemetry.set has already been called` when using with Spring Boot Actuator
  - J-Obs now detects and reuses existing GlobalOpenTelemetry instances
  - No longer conflicts with Spring Boot's tracing auto-configuration
- **TracingContext in @Scheduled Methods** - Fixed `IllegalArgumentException: Context does not have an entry for key TracingContext`
  - Automatically configures scheduled tasks to bypass Micrometer's Observation API
  - Users no longer need to create manual `SchedulingConfigurer` beans

### Changed
- **Deprecated Jaeger Exporter Removed** - Removed `opentelemetry-exporter-jaeger` dependency
  - Jaeger natively supports OTLP protocol
  - Use OTLP exporter instead: `j-obs.traces.export.otlp.endpoint=http://jaeger:4317`

### Infrastructure
- Added missing plugin version (0.7.0) to `central-publishing-maven-plugin` in benchmarks module

## [1.0.8] - 2026-01-20

### Added
- **Performance Benchmarks** - New `j-obs-benchmarks` module with JMH benchmarks
  - `LogEntryBenchmark` - Measures LogEntry creation throughput (~2.7M ops/sec)
  - `LogRepositoryBenchmark` - Benchmarks for InMemoryLogRepository operations
  - `TraceRepositoryBenchmark` - Benchmarks for InMemoryTraceRepository operations
  - Concurrent operation benchmarks (4 threads)
  - Parameterized tests for different buffer sizes (1K, 5K, 10K)

## [1.0.6] - 2026-01-20

### Fixed
- **WebSocket Configuration** - Fixed `NoClassDefFoundError` when WebSocket dependency is not present
  - Added `ServletServerContainerFactoryBean` to `@ConditionalOnClass` in `JObsWebSocketConfiguration`
  - Configuration now properly skipped when spring-boot-starter-websocket is not in classpath

### Added
- **JavaDoc Documentation** - Comprehensive JavaDoc for public APIs
  - Domain classes: `LogEntry`, `LogLevel`, `Span`, `AlertProvider`
  - Application interfaces: `LogRepository`, `TraceRepository`
  - Auto-configurations: `JObsAutoConfiguration`, `JObsProperties`, `JObsLogAutoConfiguration`, `JObsTraceAutoConfiguration`, `JObsAlertNotificationAutoConfiguration`, `JObsInternalMetricsAutoConfiguration`
  - Alert providers: All five providers with configuration examples and usage documentation

### Changed
- **Code Quality Improvements**
  - Extracted magic constants to named static finals in repository classes:
    - `DEFAULT_MAX_EVENTS` in `InMemoryAlertEventRepository`
    - `DEFAULT_MAX_HISTORY_PER_SLO` in `InMemorySloRepository`
    - `DEFAULT_MAX_SESSIONS` in `InMemoryProfilingRepository`
  - Standardized logging levels across all components (DEBUG for routine operations, INFO for important events, WARN for recoverable issues, ERROR for failures)

## [1.0.5] - 2026-01-19

### Added
- **Dashboard Authentication** - Comprehensive security system for protecting the J-Obs dashboard
  - `JObsAuthenticationFilter` for Servlet-based applications (Spring MVC)
  - `ReactiveAuthenticationFilter` for WebFlux-based applications
  - `JObsSecurityAutoConfiguration` for automatic security setup
  - Login page with dark/light mode support at `/j-obs/login`
  - Three authentication types:
    - `basic` - Username/password via login form or HTTP Basic Auth
    - `api-key` - API key in header (`X-API-Key`) or query parameter (`api_key`)
    - `both` - Both methods accepted
  - Session-based authentication with configurable timeout
  - Constant-time API key comparison for security (prevents timing attacks)
  - Exempt paths configuration for static resources
  - Comprehensive test coverage (21 tests)
- New configuration options for security:
  - `j-obs.security.enabled` - Enable/disable authentication (default: false)
  - `j-obs.security.type` - Authentication type: `basic`, `api-key`, or `both`
  - `j-obs.security.users` - List of users with username, password, and role
  - `j-obs.security.api-keys` - List of valid API keys
  - `j-obs.security.api-key-header` - Header name for API key (default: X-API-Key)
  - `j-obs.security.session-timeout` - Session duration (default: 8h)
  - `j-obs.security.exempt-paths` - Paths that bypass authentication
- New endpoints for authentication:
  - `GET /j-obs/login` - Login page
  - `POST /j-obs/login` - Form login (username, password, redirect)
  - `GET /j-obs/logout` - Browser logout (redirects to login)
  - `POST /j-obs/api/logout` - API logout (returns JSON)

## [1.0.4] - 2026-01-19

### Changed
- **GroupId Migration** - Changed Maven groupId from `io.github.j-obs` to `io.github.johnpitter`
  - Required for Maven Central namespace verification
  - Artifact IDs remain unchanged (`j-obs-core`, `j-obs-spring-boot-starter`)
  - New dependency declaration:
    ```xml
    <groupId>io.github.johnpitter</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    ```

## [1.0.3] - 2026-01-19

### Fixed
- **Maven Central Publishing** - Fixed configuration that was preventing actual publication
  - Removed deprecated `tokenAuth` parameter from `central-publishing-maven-plugin`
  - Changed sample module configuration from `skipPublishing` to `excludeArtifacts`
  - Added explicit skip configurations for GPG, Javadoc, and Source plugins in sample module
  - Sample module artifacts are now properly excluded without blocking entire publication

## [1.0.2] - 2026-01-19

### Added
- **Maven Central Support** - Library can now be published to Maven Central
  - Configured `central-publishing-maven-plugin` 0.7.0
  - Added GPG signing in release profile
  - Updated CI/CD workflow with Maven Central publishing stage
  - Added comprehensive `PUBLISHING.md` guide
- **Improved Developer Metadata** - Enhanced POM with complete developer and issue management info

### Changed
- **Code Style** - Replaced inline fully qualified class names with proper imports
  - Fixed across 13 files in both `j-obs-core` and `j-obs-spring-boot-starter`
  - Improves code readability and follows Java conventions

### Fixed
- **Sample Module Exclusion** - `j-obs-sample` is now properly excluded from deployment
  - Added `maven-deploy-plugin` skip configuration
  - Added `central-publishing-maven-plugin` skip configuration

## [1.0.1] - 2026-01-19

### Fixed
- **WebSocket Optional Dependency** - WebSocket support is now properly optional
  - Extracted WebSocket configuration to inner class with `@ConditionalOnClass`
  - Added conditional check for both `WebSocketConfigurer` and `ServerContainer`
  - Fixed failure in test environments using `MockServletContext`
  - Made `spring-boot-starter-websocket` dependency optional in POM
- **Test Environment Compatibility** - J-Obs now works correctly in Spring Boot test contexts
  - Previously failed with "WebSocketConfigurer not found" error
  - Now gracefully degrades when WebSocket is not available
  - Core functionality (logs, traces, metrics) works without WebSocket

### Added
- **JObsLogAutoConfigurationTest** - New test class verifying auto-configuration behavior
  - Tests for proper bean creation
  - Tests for working without WebSocket support
  - Tests for property configuration

## [1.0.0] - 2026-01-18

### Added
- **Sample Application (`j-obs-sample`)** - Complete demonstration project
  - `ActivityGenerator` - Scheduled job that generates sample activity for observability demonstration
    - Creates orders, processes payments, checks inventory, sends notifications
    - Generates traffic bursts for anomaly detection testing
    - Produces various log levels with MDC context
    - Simulates slow operations and error spikes
  - `OrderService`, `InventoryService`, `PaymentService`, `NotificationService` - Full service layer with `@Observable` annotation
  - Custom Health Indicators:
    - `InventoryServiceHealthIndicator` - Business-specific health check with low stock detection
    - `DiskSpaceHealthIndicator` - Simulated disk space monitoring with warning thresholds
    - `KafkaHealthIndicator` - Message broker health simulation
    - `CacheHealthIndicator` - Redis cache connectivity simulation
    - `PaymentServiceHealthIndicator` - External payment gateway health with latency tracking
    - `DatabaseHealthIndicator` - Database connectivity with connection pool stats
  - Domain models: `Order`, `OrderItem`, `OrderStatus`
  - Comprehensive `application.yml` with all J-Obs features configured
  - Demonstrates SQL patterns for SQL Analyzer testing
- **Dashboard Request Rate Chart** - Real-time visualization of request rate over last 5 minutes
  - Line chart using Chart.js
  - Auto-refresh every 5 seconds
  - Fallback message when no data available
- **Top Endpoints Widget** - Shows top 5 endpoints sorted by latency
- Initial project structure with Maven multi-module setup
- `j-obs-core` module with domain entities and dependency checking logic
  - `Dependency` value object for dependency metadata
  - `DependencyStatus` for check results
  - `DependencyCheckResult` aggregate with filtering capabilities
  - `KnownDependencies` registry for all required/optional dependencies
  - `DependencyChecker` interface (port) and `ClasspathDependencyChecker` implementation
- `j-obs-spring-boot-starter` module with Spring Boot auto-configuration
  - `JObsAutoConfiguration` for automatic bean registration
  - `JObsProperties` for externalized configuration
  - `JObsController` serving HTML dashboard with dependency status
  - `JObsApiController` providing REST API endpoints
- Dependency verification for:
  - OpenTelemetry API (required)
  - OpenTelemetry SDK (required)
  - Micrometer Core (required)
  - Micrometer Prometheus (required)
  - Spring Boot Actuator (required)
  - Logback (optional)
- Dashboard UI with:
  - Dark/Light theme based on system preference
  - Requirements check page with installation instructions
  - Basic dashboard overview (placeholder for future metrics)
  - HTMX integration for dynamic updates
  - Tailwind CSS styling
- REST API endpoints:
  - `GET /j-obs/api/requirements` - Check dependency status
  - `POST /j-obs/api/requirements/refresh` - Force re-check
  - `GET /j-obs/api/health` - Health status
- Configuration options via `application.yml`:
  - `j-obs.enabled` - Enable/disable J-Obs
  - `j-obs.path` - Custom base path
  - `j-obs.check-interval` - Cache duration for checks
  - `j-obs.dashboard.theme` - Theme preference
- **Trace visualization with OpenTelemetry integration**
  - Domain model for traces: `Span`, `Trace`, `SpanKind`, `SpanStatus`, `SpanEvent`, `TraceQuery`
  - `TraceRepository` interface and `InMemoryTraceRepository` implementation with TTL and eviction
  - `JObsSpanExporter` for capturing spans from OpenTelemetry SDK
  - `JObsTraceAutoConfiguration` for automatic trace configuration
  - `TraceApiController` REST API for querying and filtering traces
  - `TraceController` HTML pages for trace visualization
  - Waterfall timeline UI for visualizing span hierarchy and timing
  - Filtering by service name, status, duration, and span name
  - HTTP and database attribute extraction from spans
- New REST API endpoints for traces:
  - `GET /j-obs/api/traces` - List traces with filtering
  - `GET /j-obs/api/traces/{traceId}` - Get trace details
  - `GET /j-obs/api/traces/{traceId}/spans` - Get spans for waterfall view
  - `GET /j-obs/api/traces/stats` - Get trace statistics
  - `GET /j-obs/api/traces/services` - List available services
  - `DELETE /j-obs/api/traces` - Clear all traces
- New configuration options for traces:
  - `j-obs.traces.enabled` - Enable/disable trace collection
  - `j-obs.traces.max-traces` - Maximum traces to keep in memory
  - `j-obs.traces.retention` - How long to retain traces
  - `j-obs.traces.sample-rate` - Sampling rate for traces
- Unit tests for trace domain layer (28 tests)
- Integration tests for TraceApiController (11 tests)
- **Real-time log streaming with WebSocket**
  - Domain model for logs: `LogLevel`, `LogEntry`, `LogQuery`
  - `LogRepository` interface with subscription support for reactive streaming
  - `InMemoryLogRepository` with circular buffer (efficient memory management)
  - `JObsLogAppender` for Logback integration (captures application logs)
  - `JObsLogAutoConfiguration` with WebSocket configuration
  - `LogWebSocketHandler` for real-time log streaming
  - `LogApiController` REST API for querying and filtering logs
  - `LogController` HTML page with live log viewer
  - MDC support for trace correlation
  - Exception/stacktrace formatting
  - Level-based filtering (TRACE, DEBUG, INFO, WARN, ERROR)
- New REST API endpoints for logs:
  - `GET /j-obs/api/logs` - List logs with filtering and pagination
  - `GET /j-obs/api/logs/stats` - Get log statistics by level
  - `GET /j-obs/api/logs/levels` - Get available log levels
  - `GET /j-obs/api/logs/loggers` - List unique logger names
  - `DELETE /j-obs/api/logs` - Clear all logs
- WebSocket endpoint for real-time streaming:
  - `WS /j-obs/ws/logs` - WebSocket for live log feed
  - Supports filter, pause, resume commands
  - Session-based filtering
- New configuration options for logs:
  - `j-obs.logs.enabled` - Enable/disable log collection
  - `j-obs.logs.max-entries` - Maximum log entries to keep
  - `j-obs.logs.min-level` - Minimum log level to capture
- Log viewer UI features:
  - Real-time streaming with WebSocket
  - Filter by level, logger, message text
  - Pause/Resume functionality
  - Clear logs button
  - Trace ID correlation links
  - Exception/stacktrace display
  - Connection status indicator
- Unit tests for log domain layer (12 tests)
- **Metrics visualization with Micrometer integration**
  - Domain model for metrics: `Metric`, `MetricType`, `MetricValue`, `MetricQuery`, `MetricSnapshot`
  - `MetricRepository` interface with time-series history support
  - `MicrometerMetricRepository` backed by Micrometer MeterRegistry
  - `JObsMetricAutoConfiguration` with automatic metric refresh
  - `MetricApiController` REST API for querying metrics
  - `MetricController` HTML page with interactive charts
  - Support for all Micrometer meter types: Counter, Gauge, Timer, DistributionSummary, LongTaskTimer
  - Percentile data extraction for Timers and Distribution Summaries
  - Time-series data collection with configurable history retention
- New REST API endpoints for metrics:
  - `GET /j-obs/api/metrics` - List metrics with filtering
  - `GET /j-obs/api/metrics/{id}` - Get metric details with history
  - `GET /j-obs/api/metrics/stats` - Get metric statistics by type
  - `GET /j-obs/api/metrics/names` - List all metric names
  - `GET /j-obs/api/metrics/categories` - List metric categories
  - `GET /j-obs/api/metrics/tags` - List tag keys
  - `GET /j-obs/api/metrics/tags/{key}/values` - List values for a tag
  - `GET /j-obs/api/metrics/snapshot` - Get time-series data
  - `POST /j-obs/api/metrics/refresh` - Force metric refresh
- New configuration options for metrics:
  - `j-obs.metrics.enabled` - Enable/disable metric collection
  - `j-obs.metrics.refresh-interval` - How often to collect metrics (ms)
  - `j-obs.metrics.max-history-points` - Maximum history data points per metric
- Metrics viewer UI features:
  - Card-based metric display with current values
  - Filter by name, type, and category
  - Chart.js integration for time-series visualization
  - Percentile and statistics display for Timers
  - Tag-based filtering
  - Auto-refresh every 10 seconds
- **Health checks visualization with Spring Boot Actuator integration**
  - Domain model for health: `HealthStatus`, `HealthComponent`, `HealthCheckResult`
  - `HealthRepository` interface with history tracking support
  - `ActuatorHealthRepository` backed by Spring Boot Actuator HealthEndpoint
  - `JObsHealthAutoConfiguration` for automatic health configuration
  - `HealthApiController` REST API for querying health status
  - `HealthController` HTML page with component cards
  - Support for all Actuator health statuses: UP, DOWN, OUT_OF_SERVICE, UNKNOWN
  - Component details extraction with error messages
  - History tracking for health state changes
- New REST API endpoints for health:
  - `GET /j-obs/api/health` - Get overall health status
  - `GET /j-obs/api/health/components` - List all health components
  - `GET /j-obs/api/health/components/{name}` - Get specific component health
  - `GET /j-obs/api/health/components/{name}/history` - Get component history
  - `POST /j-obs/api/health/refresh` - Force health refresh
  - `GET /j-obs/api/health/summary` - Get health summary statistics
- New configuration options for health:
  - `j-obs.health.enabled` - Enable/disable health check integration
  - `j-obs.health.max-history-entries` - Maximum history entries per component
- Health viewer UI features:
  - Overall health status banner with healthy/unhealthy count
  - Component cards with status, details, and errors
  - CSS styling based on health status (UP=green, DOWN=red, etc.)
  - Emoji indicators for each health status
  - Auto-refresh every 30 seconds
  - Manual refresh button
  - Dark/Light theme support
- **Sample application for testing and demonstration**
  - Complete Spring Boot application demonstrating all J-Obs features
  - OrderController and OrderService with full instrumentation
  - Custom health indicators (database, cache, payment-service)
  - ActivityGenerator for continuous log/trace generation
  - Sample configuration in application.yml
- **Documentation**
  - README.md with quick start guide
  - GETTING_STARTED.md with complete integration tutorial
  - Code examples for traces, metrics, logs, and health checks
- **Automatic instrumentation via annotations**
  - `@Observable` annotation for full observability (traces + metrics)
  - `@Traced` annotation for automatic span creation
  - `@Measured` annotation for automatic metrics (timer, counter)
  - `TracingAspect` for intercepting annotated methods
  - `MetricsAspect` for recording method execution metrics
  - `AutoInstrumentationAspect` for auto-instrumenting all Spring components (opt-in)
  - `HttpTracingFilter` for automatic HTTP request tracing
  - Configuration options: `j-obs.instrumentation.*`
- Unit tests for security and infrastructure code:
  - `RateLimiterTest` - sliding window, cleanup, rate limiting (14 tests)
  - `InputSanitizerTest` - logger, message, traceId sanitization (36 tests)
  - `UrlValidatorTest` - SSRF prevention for webhooks (19 tests)
  - `CachedMetricRepositoryTest` - cache and invalidation (13 tests)
  - `JObsHealthIndicatorTest` - status reporting, degraded, down (12 tests)
  - `AlertThrottlerTest` - tryAcquire, cooldown, global rate limiting (11 tests)
  - `TraceApiControllerTest` - lazy loading and pagination tests (4 tests)
- Total: 148 tests passing
- **SQL Analyzer for query performance analysis**
  - Domain model for SQL analysis: `SqlProblem`, `SqlProblemType`, `SqlSeverity`, `SlowQuery`
  - `SqlAnalyzer` interface with problem detection and slow query analysis
  - `DefaultSqlAnalyzer` implementation with multiple detection algorithms
  - `JObsSqlAnalyzerAutoConfiguration` for automatic configuration
  - Problem detection for:
    - N+1 queries (multiple similar queries in same trace)
    - Slow queries (above configurable threshold)
    - SELECT * anti-pattern
    - Missing LIMIT clauses
    - Large result sets
  - `SqlAnalyzerApiController` REST API for SQL problems
  - `SqlAnalyzerController` HTML page with problem cards
  - Configurable thresholds and ignore patterns
- New REST API endpoints for SQL analysis:
  - `GET /j-obs/api/sql/problems` - List detected SQL problems
  - `GET /j-obs/api/sql/slow-queries` - List slow queries
  - `GET /j-obs/api/sql/stats` - Get SQL analysis statistics
  - `POST /j-obs/api/sql/analyze` - Force re-analysis
- New configuration options for SQL analysis:
  - `j-obs.sql-analyzer.enabled` - Enable/disable SQL analysis
  - `j-obs.sql-analyzer.slow-query-threshold` - Slow query threshold
  - `j-obs.sql-analyzer.n-plus-one-threshold` - N+1 detection threshold
  - `j-obs.sql-analyzer.ignore-patterns` - Patterns to ignore
- **Anomaly Detection for automatic problem detection**
  - Domain model: `Anomaly`, `AnomalyType`, `AnomalySeverity`, `PossibleCause`
  - `AnomalyDetector` interface with detection and statistics
  - `DefaultAnomalyDetector` implementation with statistical analysis
  - `JObsAnomalyDetectionAutoConfiguration` for automatic setup
  - Detection algorithms:
    - Z-score based latency spike detection
    - Moving average for traffic anomalies
    - Error rate spike detection
  - Configurable sensitivity and thresholds
  - Automatic cause correlation with dependencies
- New REST API endpoints for anomalies:
  - `GET /j-obs/api/anomalies` - List detected anomalies
  - `GET /j-obs/api/anomalies/{id}` - Get anomaly details
  - `GET /j-obs/api/anomalies/stats` - Get detection statistics
  - `POST /j-obs/api/anomalies/detect` - Trigger detection
  - `POST /j-obs/api/anomalies/{id}/acknowledge` - Acknowledge anomaly
- New configuration options for anomaly detection:
  - `j-obs.anomaly-detection.enabled` - Enable/disable detection
  - `j-obs.anomaly-detection.detection-interval` - Detection interval
  - `j-obs.anomaly-detection.baseline-window` - Baseline calculation window
  - `j-obs.anomaly-detection.sensitivity.*` - Sensitivity settings
- **Alert Notification System with multiple providers**
  - Domain model: `Alert`, `AlertEvent`, `AlertEventStatus`, `AlertSeverity`, `AlertType`, `AlertCondition`
  - `AlertProvider` SPI for extensible notification providers
  - `AlertEventRepository` for alert history tracking
  - `AlertEngine` for periodic alert evaluation
  - `AlertDispatcher` for async notification dispatch
  - `AlertThrottler` for rate limiting and cooldown
  - Built-in providers:
    - Telegram (Bot API)
    - Microsoft Teams (Webhook)
    - Slack (Webhook)
    - Email (SMTP)
    - Generic Webhook (customizable)
  - `AlertNotificationService` for centralized alert management
  - `JObsAlertNotificationAutoConfiguration` for automatic setup
- New REST API endpoints for alerts:
  - `GET /j-obs/api/alerts` - List configured alerts
  - `GET /j-obs/api/alert-events` - List alert events
  - `GET /j-obs/api/alert-events/{id}` - Get event details
  - `POST /j-obs/api/alert-events/{id}/acknowledge` - Acknowledge alert
  - `POST /j-obs/api/alert-events/{id}/resolve` - Resolve alert
  - `GET /j-obs/api/alert-providers` - List providers
  - `POST /j-obs/api/alert-providers/{name}/test` - Test provider
- New configuration options for alerts:
  - `j-obs.alerts.enabled` - Enable/disable alert system
  - `j-obs.alerts.evaluation-interval` - Alert evaluation interval
  - `j-obs.alerts.throttling.*` - Throttling settings
  - `j-obs.alerts.providers.telegram.*` - Telegram configuration
  - `j-obs.alerts.providers.teams.*` - Teams configuration
  - `j-obs.alerts.providers.slack.*` - Slack configuration
  - `j-obs.alerts.providers.email.*` - Email configuration
  - `j-obs.alerts.providers.webhook.*` - Webhook configuration
- **Service Map for dependency visualization**
  - Domain model: `ServiceNode`, `ServiceConnection`, `ServiceMap`
  - `ServiceNode` with `ServiceType`, `ServiceHealth`, `ServiceStats`
  - `ServiceConnection` with `ConnectionType`, `ConnectionStats`
  - `ServiceMapBuilder` interface for building service maps
  - `DefaultServiceMapBuilder` extracting dependencies from traces
  - `JObsServiceMapAutoConfiguration` for automatic setup
  - SVG-based interactive visualization
  - Node health calculation based on error rates and latency
  - Connection statistics (RPS, error rate, latency percentiles)
- New REST API endpoints for service map:
  - `GET /j-obs/api/service-map` - Get service map
  - `POST /j-obs/api/service-map/refresh` - Force refresh
  - `GET /j-obs/api/service-map/stats` - Get map statistics
  - `GET /j-obs/api/service-map/nodes` - List all nodes
  - `GET /j-obs/api/service-map/nodes/{id}` - Get node details
  - `GET /j-obs/api/service-map/nodes/issues` - List nodes with issues
  - `GET /j-obs/api/service-map/connections` - List all connections
- New configuration options for service map:
  - `j-obs.service-map.default-window` - Time window for analysis
  - `j-obs.service-map.cache-expiration` - Cache TTL
  - `j-obs.service-map.error-threshold` - Error rate threshold
  - `j-obs.service-map.latency-threshold` - Latency threshold
  - `j-obs.service-map.min-requests-for-health` - Min requests for health calc
- **Lazy loading for trace spans**:
  - `GET /j-obs/api/traces/{traceId}` now supports `includeSpans` parameter (default: true)
  - `GET /j-obs/api/traces/{traceId}` now supports `maxSpans` parameter to limit spans (default: 100)
  - `GET /j-obs/api/traces/{traceId}/spans` now supports pagination with `limit` and `offset`
  - `SpansResponse` DTO with `hasMore` indicator for pagination
  - `TraceDetailDto` extended with `spansIncluded` and `hasMoreSpans` fields
- **WebSocket compression support**:
  - `JObsWebSocketConfiguration` for container-level WebSocket settings
  - Configuration via `j-obs.logs.websocket.*` properties
  - `compression-enabled` (default: true) for permessage-deflate
  - `max-text-message-size` (default: 65536) for large log entries
  - `send-buffer-size` (default: 16384) for optimal throughput
  - Works with Tomcat 8.5+, Jetty 9.4+ (permessage-deflate enabled by default)
- **SLO/SLI Tracking for service level management**
  - Domain model: `Slo`, `Sli`, `SliType`, `SloStatus`, `ErrorBudget`, `BurnRate`, `SloEvaluation`
  - `SliType` enum: AVAILABILITY, LATENCY, ERROR_RATE, THROUGHPUT
  - `SloStatus` enum: HEALTHY, AT_RISK, BREACHED, NO_DATA
  - `ErrorBudget` calculation with remaining percentage and exhaustion tracking
  - `BurnRate` calculation with severity levels (safe, elevated, high, critical)
  - `SloRepository` interface for SLO persistence with evaluation history
  - `SloService` interface for SLO management and evaluation
  - `InMemorySloRepository` implementation with configurable history size
  - `DefaultSloService` with Micrometer MeterRegistry integration
  - `SloScheduler` for periodic SLO evaluation
  - `JObsSloAutoConfiguration` for automatic setup
  - Factory methods for common SLO patterns (availability, latency)
  - Builder pattern for custom SLO configuration
  - Multi-window burn rate alerts support
- New REST API endpoints for SLOs:
  - `GET /j-obs/api/slos` - List all SLOs with current status
  - `GET /j-obs/api/slos/{name}` - Get SLO details
  - `POST /j-obs/api/slos` - Create new SLO
  - `DELETE /j-obs/api/slos/{name}` - Delete SLO
  - `POST /j-obs/api/slos/evaluate` - Evaluate all SLOs
  - `POST /j-obs/api/slos/{name}/evaluate` - Evaluate specific SLO
  - `GET /j-obs/api/slos/{name}/history` - Get evaluation history
  - `GET /j-obs/api/slos/summary` - Get summary statistics
- New configuration options for SLOs:
  - `j-obs.slo.enabled` - Enable/disable SLO tracking
  - `j-obs.slo.evaluation-interval` - Evaluation interval
  - `j-obs.slo.max-evaluation-history` - History size per SLO
  - `j-obs.slo.default-window` - Default SLO window
  - `j-obs.slo.error-budget-warning-threshold` - Warning threshold
  - `j-obs.slo.short-window-burn-rate` - Short window alert config
  - `j-obs.slo.long-window-burn-rate` - Long window alert config
- 71 new tests for SLO/SLI functionality
- **Profiling for CPU, memory, and thread analysis**
  - Domain model: `ProfileType`, `ProfileStatus`, `ProfileSession`, `ProfileResult`
  - `CpuSample` with stack trace analysis and flame graph support
  - `FlameGraphNode` for building flame graphs from CPU samples
  - `MemoryInfo` with heap, non-heap, memory pools, and GC statistics
  - `ThreadDump` with thread state analysis and deadlock detection
  - `ProfilingService` interface for profiling operations
  - `DefaultProfilingService` using JVM management APIs
  - `InMemoryProfilingRepository` for session history
  - `JObsProfilingAutoConfiguration` for automatic setup
  - CPU profiling with configurable duration and sampling interval
  - Memory snapshot capture with detailed pool breakdown
  - Thread dump with stack traces and lock information
- New REST API endpoints for profiling:
  - `GET /j-obs/api/profiling/stats` - Get profiling statistics
  - `GET /j-obs/api/profiling/sessions` - List profiling sessions
  - `GET /j-obs/api/profiling/sessions/{id}` - Get session details
  - `POST /j-obs/api/profiling/cpu/start` - Start CPU profiling
  - `POST /j-obs/api/profiling/cpu/{id}/stop` - Stop CPU profiling
  - `GET /j-obs/api/profiling/cpu/running` - Get running CPU profile
  - `GET /j-obs/api/profiling/sessions/{id}/cpu` - Get CPU profile results
  - `POST /j-obs/api/profiling/memory` - Capture memory snapshot
  - `GET /j-obs/api/profiling/memory` - Get latest memory snapshot
  - `POST /j-obs/api/profiling/threads` - Capture thread dump
  - `GET /j-obs/api/profiling/threads` - Get latest thread dump
  - `POST /j-obs/api/profiling/sessions/{id}/cancel` - Cancel session
  - `DELETE /j-obs/api/profiling/sessions` - Clear completed sessions
- New configuration options for profiling:
  - `j-obs.profiling.enabled` - Enable/disable profiling
  - `j-obs.profiling.default-duration` - Default CPU profile duration
  - `j-obs.profiling.default-sampling-interval` - Default sampling interval
  - `j-obs.profiling.max-sessions` - Maximum sessions to keep
  - `j-obs.profiling.max-duration` - Maximum allowed duration
- 55 new tests for profiling functionality
- **Alert Grouping for reducing notification noise**
  - Domain model: `AlertGroup`, `AlertGroupKey`, `AlertGroupStatus`
  - `AlertGrouper` service for managing pending groups
  - Groups alerts by: alert name + severity + configurable labels
  - Configurable group wait time before sending
  - Configurable maximum group size
  - Automatic flush when group reaches max size
  - Automatic flush after group wait time expires
  - Summary message generation for multi-alert groups
  - Integration with `AlertScheduler` for automatic grouping
- New configuration options for alert grouping:
  - `j-obs.alerts.throttling.max-group-size` - Maximum alerts per group
  - `j-obs.alerts.throttling.group-by-labels` - Labels used for grouping
- 48 new tests for alert grouping functionality
- **Security documentation** in `GETTING_STARTED.md`:
  - CSRF protection explanation (stateless API design)
  - Rate limiting configuration guide
  - Input sanitization overview
  - SSRF prevention for webhooks
  - Secure proxy configuration (nginx, Traefik)

### Changed
- `UrlValidator` now correctly handles IPv6 addresses by stripping brackets from URL host
- `JObsMetricAutoConfiguration` now wraps MetricRepository with `CachedMetricRepository` for improved performance
- `JObsTraceAutoConfiguration` now uses `destroyMethod = "shutdown"` for proper cleanup
- `InMemoryLogRepository.query()` now uses efficient direct buffer iteration instead of creating intermediate collections
- `InMemoryLogRepository.count(LogQuery)` optimized to iterate directly without creating intermediate collections
- `InMemoryTraceRepository` eviction now uses insertion order for deterministic behavior when timestamps are equal
- `InputSanitizer.sanitizeTraceId()` now accepts alphanumeric characters (not just hex) to support various trace ID formats
- `GET /j-obs/api/traces/{traceId}/spans` now returns `SpansResponse` object with pagination metadata instead of plain list (backward compatible via `$.spans` field)

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- **Chart.js Reactivity Issue** - Fixed "Maximum call stack size exceeded" error when Chart.js instance was made reactive by Alpine.js
  - Moved chart reference outside Alpine's reactive scope
  - Chart now renders correctly without reactivity conflicts
- **Request Rate Chart Error Handling** - Added proper error handling and fallback UI when request rate data is unavailable

### Security
- **Rate Limiting**: Added `RateLimiter` and `RateLimitInterceptor` to protect API endpoints from DoS attacks
  - Configurable via `j-obs.rate-limiting.enabled`, `j-obs.rate-limiting.max-requests`, `j-obs.rate-limiting.window`
  - Default: 100 requests per minute per client
  - Supports X-Forwarded-For for proxied requests
- **Input Sanitization**: Added `InputSanitizer` utility class to prevent injection attacks
  - Sanitizes all user input parameters (logger, message, traceId, thread, etc.)
  - Validates trace ID format (hex characters and dashes only)
  - Escapes regex special characters for safe pattern matching
  - Limits input lengths to prevent resource exhaustion
- **URL Validation**: Added `UrlValidator` to prevent SSRF attacks in webhook providers
  - Blocks localhost, private IPs (10.x, 172.16-31.x, 192.168.x), and link-local addresses
  - Validates URL scheme (HTTP/HTTPS only)
  - Whitelist of known webhook domains (Telegram, Slack, Teams)
  - DNS resolution check for hostname-based URLs
- **Configurable HTTP Timeout**: Added `j-obs.alerts.http-timeout` for controlling alert provider timeouts
  - Default: 30 seconds
  - Applied to all alert providers (Telegram, Teams, Slack, Webhook)

### Infrastructure
- **Health Indicator**: Added `JObsHealthIndicator` for Spring Boot Actuator integration
  - Reports repository usage (traces, logs, metrics, alerts)
  - Capacity alerts at 80% (DEGRADED) and 95% (DOWN)
  - Estimated memory usage calculation
  - Auto-configuration via `JObsActuatorAutoConfiguration`
- **Metrics Caching**: Added `CachedMetricRepository` decorator for improved performance
  - Caches stats(), getMetricNames(), getCategories(), getTagKeys(), query()
  - Configurable TTL based on metrics refresh interval
  - Automatic cache invalidation on refresh()
- **Graceful Shutdown**: Ensured proper cleanup of TraceRepository background tasks

