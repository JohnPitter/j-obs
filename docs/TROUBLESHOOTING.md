# J-Obs Troubleshooting Guide

This guide helps you resolve common issues when integrating J-Obs into your Spring Boot application.

## Table of Contents

- [Dependency Issues](#dependency-issues)
- [WebSocket Issues](#websocket-issues)
- [Test Environment Issues](#test-environment-issues)
- [Integration Conflicts](#integration-conflicts)
- [Performance Issues](#performance-issues)
- [Dashboard Issues](#dashboard-issues)

---

## Dependency Issues

### "POM is invalid, transitive dependencies will not be available"

**Symptoms:**
```
[WARNING] The POM for io.github.johnpitter:j-obs-spring-boot-starter:jar:x.x.x is invalid
```

**Cause:** Using an old version published before Maven Central validation.

**Solution:** Use version 1.0.4 or later:
```xml
<dependency>
    <groupId>io.github.johnpitter</groupId>
    <artifactId>j-obs-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

### "Could not find artifact io.github.j-obs"

**Cause:** The groupId changed from `io.github.j-obs` to `io.github.johnpitter` in v1.0.4.

**Solution:** Update your dependency:
```xml
<!-- Old (won't work) -->
<groupId>io.github.j-obs</groupId>

<!-- New (correct) -->
<groupId>io.github.johnpitter</groupId>
```

### Dependency version conflicts with Spring Boot

**Symptoms:**
- `NoSuchMethodError` or `ClassNotFoundException`
- OpenTelemetry or Micrometer version conflicts

**Solution:** J-Obs is compatible with Spring Boot 3.2.x, 3.3.x, and 3.4.x. The j-obs-bom only manages J-Obs module versions and does NOT pin third-party dependency versions, so it will not conflict with your Spring Boot version.

1. Check the [Compatibility Matrix](#compatibility-matrix)
2. Use the BOM for J-Obs version management:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.johnpitter</groupId>
            <artifactId>j-obs-bom</artifactId>
            <version>1.0.11</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Important:** Let your Spring Boot parent manage Micrometer and other dependency versions. The j-obs-bom only provides J-Obs module versions.

### Micrometer version conflicts

**Symptoms:**
```
NoClassDefFoundError: io/micrometer/prometheus/PrometheusMeterRegistry
```
or
```
NoClassDefFoundError: io/micrometer/prometheusmetrics/PrometheusMeterRegistry
```

**Cause:** Micrometer 1.13+ changed the package name from `io.micrometer.prometheus` to `io.micrometer.prometheusmetrics`.

**Solution:** As of v1.0.10+, J-Obs automatically detects both old and new Micrometer Prometheus packages. Ensure you're using J-Obs 1.0.10+ and let Spring Boot manage Micrometer versions.

### Prometheus version conflicts

**Symptoms:**
```
java.lang.NoSuchMethodError: 'boolean io.prometheus.metrics.config.ExporterProperties.getPrometheusTimestampsInMs()'
```

or similar `NoSuchMethodError` in Prometheus classes.

**Cause:** The `micrometer-registry-prometheus` dependency brings transitive Prometheus dependencies. When there are version misalignments between `prometheus-metrics-exposition-textformats` and `prometheus-metrics-config`, methods may be missing.

This typically occurs when:
- Using a Spring Boot version different from what J-Obs was built with
- Manually pinning Prometheus dependency versions
- Importing `j-obs-parent` instead of `j-obs-bom`

**Solution:**

**Option 1 (Recommended):** Use `j-obs-bom` instead of `j-obs-parent`:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Only manages J-Obs modules, doesn't touch Prometheus/Micrometer -->
        <dependency>
            <groupId>io.github.johnpitter</groupId>
            <artifactId>j-obs-bom</artifactId>
            <version>1.0.11</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Option 2:** Force consistent Prometheus versions in your project:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Force all Prometheus artifacts to the same version -->
        <dependency>
            <groupId>io.prometheus</groupId>
            <artifactId>prometheus-metrics-config</artifactId>
            <version>1.3.5</version>
        </dependency>
        <dependency>
            <groupId>io.prometheus</groupId>
            <artifactId>prometheus-metrics-exposition-textformats</artifactId>
            <version>1.3.5</version>
        </dependency>
        <dependency>
            <groupId>io.prometheus</groupId>
            <artifactId>prometheus-metrics-exposition-formats</artifactId>
            <version>1.3.5</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Option 3:** Use Spring Boot's Prometheus BOM (Spring Boot 3.4+):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.prometheus</groupId>
            <artifactId>prometheus-metrics-bom</artifactId>
            <version>1.3.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Diagnosis:** Run `mvn dependency:tree -Dincludes=io.prometheus` to check for version misalignments.

---

## WebSocket Issues

### "WebSocketConfigurer class not found"

**Symptoms:**
```
java.io.FileNotFoundException: class path resource
[org/springframework/web/socket/config/annotation/WebSocketConfigurer.class]
cannot be opened because it does not exist
```

**Cause:** WebSocket dependency is not in classpath (fixed in v1.0.1+).

**Solutions:**

**Option 1:** Add WebSocket dependency (if you want real-time logs):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**Option 2:** Use v1.0.1+ where WebSocket is optional. J-Obs will work without real-time log streaming.

### "ServerContainer not available"

**Symptoms:**
```
A]ServerContainer'</beanornclassNotaroundException: Unable to find aiftype
'javax.websocket.server.ServerContainer'
```

**Cause:** Running in environment without WebSocket support (e.g., some test contexts).

**Solution:** Since v1.0.1, J-Obs gracefully degrades. Ensure you're using v1.0.1+. WebSocket features will be disabled automatically.

### WebSocket connection fails in production

**Symptoms:**
- Dashboard shows "Disconnected" for real-time logs
- WebSocket upgrade fails with 404

**Solutions:**

1. **Check reverse proxy configuration** (nginx, Traefik, etc.):
```nginx
location /j-obs/ws {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

2. **Check if WebSocket is enabled:**
```yaml
j-obs:
  logs:
    websocket:
      enabled: true
```

---

## Test Environment Issues

### Tests fail with "MockServletContext" or "ServerContainer not available"

**Symptoms:**
```
Caused by: java.lang.IllegalStateException:
javax.websocket.server.ServerContainer not available
```

or

```
Caused by: java.lang.IllegalStateException:
A 'ServerContainer' that could not be satisfied
```

**Cause:** `MockServletContext` doesn't have the `ServerContainer` attribute set. This occurs in Spring Boot tests that use `@SpringBootTest` without a real servlet container.

**Solution:** As of v1.0.10+, J-Obs automatically detects if a real `ServerContainer` is available in the `ServletContext`. If running in a test environment with `MockServletContext`, WebSocket configuration is automatically skipped.

The fix uses a custom `@ConditionalOnServerContainer` annotation that checks for the presence of the `jakarta.websocket.server.ServerContainer` attribute in the `ServletContext` before loading WebSocket-related beans.

**Workaround for older versions (< 1.0.10):**
```java
@SpringBootTest
@TestPropertySource(properties = {
    "j-obs.logs.websocket.enabled=false"
})
class MyTest {
    // ...
}
```

**Alternative: Use integration tests with real server:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyIntegrationTest {
    // This starts a real servlet container with WebSocket support
}
```

### Tests are slow with J-Obs enabled

**Cause:** J-Obs initializes repositories and collectors on startup.

**Solutions:**

**Option 1 (Recommended):** Disable J-Obs with property:
```yaml
# application-test.yml
j-obs:
  enabled: false
```

```java
@SpringBootTest
@ActiveProfiles("test")
class MyTest {
    // ...
}
```

Or via `@TestPropertySource`:
```java
@SpringBootTest
@TestPropertySource(properties = "j-obs.enabled=false")
class MyTest {
    // ...
}
```

As of v1.0.11+, setting `j-obs.enabled=false` completely disables ALL J-Obs auto-configurations including WebSocket, logs, traces, metrics, alerts, and all other features.

**Option 2:** Exclude specific J-Obs configurations:
```java
@SpringBootTest
@EnableAutoConfiguration(exclude = {
    JObsAutoConfiguration.class
})
class MyTest {
    // ...
}
```

---

## Integration Conflicts

### Conflict with existing Micrometer configuration

**Symptoms:**
- Duplicate metrics
- `BeanDefinitionOverrideException`

**Solution:** J-Obs uses the existing `MeterRegistry` if present. Ensure you don't have conflicting beans:

```java
// Don't do this if J-Obs is present
@Bean
public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
}
```

**To customize Micrometer:**
```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
```

### Conflict with existing Logback configuration

**Symptoms:**
- Logs not appearing in J-Obs dashboard
- Duplicate logs

**Cause:** Custom `logback-spring.xml` might not include J-Obs appender.

**Solution:** Add J-Obs appender to your logback config:

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Your existing appenders -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <!-- ... -->
    </appender>

    <!-- J-Obs appender (auto-registered, just ensure root logger includes it) -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <!-- J-Obs automatically attaches to root logger -->
    </root>
</configuration>
```

### Conflict with Spring Boot Actuator OpenTelemetry

**Symptoms:**
```
java.lang.IllegalStateException: GlobalOpenTelemetry.set has already been called.
GlobalOpenTelemetry.set must be called only once before any calls to GlobalOpenTelemetry.get.
```

**Cause:** Spring Boot Actuator has its own OpenTelemetry auto-configuration that initializes `GlobalOpenTelemetry` before J-Obs.

**Solution:** As of v1.0.9+, J-Obs automatically detects and reuses existing `GlobalOpenTelemetry` instances. If you're on an older version, disable Spring Boot's tracing auto-configuration:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration
      - org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration
```

### TracingContext errors in @Scheduled methods

**Symptoms:**
```
java.lang.IllegalArgumentException: Context does not have an entry for key
[class io.micrometer.tracing.handler.TracingObservationHandler$TracingContext]
```

**Cause:** Spring Boot Actuator's Observation API instruments `@Scheduled` methods. When tracing is disabled or conflicts with J-Obs, the `TracingContext` is not available.

**Solution:** As of v1.0.9+, J-Obs automatically configures scheduled tasks to use `ObservationRegistry.NOOP`, preventing this error. No manual configuration is needed.

If you need to disable this automatic fix (e.g., you want Spring Boot Actuator to observe scheduled tasks):

```yaml
j-obs:
  observability:
    scheduled-tasks-noop: false
```

**For older versions (< 1.0.9):** Create a configuration manually:

```java
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class ObservabilityConfig implements SchedulingConfigurer {
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setObservationRegistry(ObservationRegistry.NOOP);
    }
}
```

### Conflict with OTLP Agent

**Symptoms:**
- Duplicate traces
- High memory usage

**Cause:** Both J-Obs and OTLP agent are collecting traces.

**Solutions:**

**Option 1:** Disable J-Obs trace collection (use J-Obs as dashboard only):
```yaml
j-obs:
  traces:
    collection:
      enabled: false
```

**Option 2:** Disable OTLP agent and use J-Obs for everything.

**Option 3:** Configure J-Obs to export to your OTLP collector:
```yaml
j-obs:
  traces:
    export:
      otlp:
        enabled: true
        endpoint: http://otel-collector:4317
```

---

## Performance Issues

### High memory usage

**Symptoms:**
- OOM errors
- Increasing heap usage over time

**Causes & Solutions:**

1. **Too many logs stored:**
```yaml
j-obs:
  logs:
    buffer-size: 5000  # Reduce from default 10000
    retention: 30m     # Reduce from default 1h
```

2. **Too many traces stored:**
```yaml
j-obs:
  traces:
    max-traces: 500    # Reduce from default 1000
    retention: 30m
```

3. **Large spans in traces:**
```yaml
j-obs:
  traces:
    max-spans-per-trace: 100  # Limit spans
```

### Dashboard is slow

**Causes & Solutions:**

1. **Too much data loading:**
   - Use pagination in log viewer
   - Filter by time range
   - Use search to narrow results

2. **WebSocket compression disabled:**
```yaml
j-obs:
  logs:
    websocket:
      compression-enabled: true  # Enable if not already
```

---

## Dashboard Issues

### Dashboard returns 404

**Cause:** J-Obs might not be auto-configured.

**Solutions:**

1. **Check if J-Obs is enabled:**
```yaml
j-obs:
  enabled: true  # default is true
```

2. **Check component scan:**
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourapp", "io.github.jobs"})
public class Application {
    // ...
}
```

3. **Check the path:**
   - Default: `http://localhost:8080/j-obs`
   - Custom:
   ```yaml
   j-obs:
     path: /custom-path
   ```

### Dashboard shows "No data"

**Causes & Solutions:**

1. **Application just started:** Wait for some requests to generate data.

2. **Actuator not configured:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

3. **Micrometer not collecting:**
```yaml
management:
  metrics:
    enable:
      all: true
```

### Health check shows "DOWN"

**Symptoms:**
- J-Obs health indicator reports DOWN
- `/actuator/health` shows j-obs component as unhealthy

**Causes:**
1. **Repositories at capacity (>95%):**
   - Increase buffer sizes or reduce retention

2. **Internal error:**
   - Check logs for exceptions
   - Restart application

---

## Compatibility Matrix

| Spring Boot | Java | J-Obs Version | Status |
|-------------|------|---------------|--------|
| 3.4.x       | 17+  | 1.0.4+        | ✅ Tested |
| 3.3.x       | 17+  | 1.0.4+        | ✅ Tested |
| 3.2.x       | 17+  | 1.0.0+        | ✅ Tested |
| 3.1.x       | 17+  | 1.0.4+        | ⚠️ Should work |
| 3.0.x       | 17+  | 1.0.4+        | ⚠️ Should work |
| 2.7.x       | 11+  | -             | ❌ Not supported |

**Notes:**
- J-Obs requires Java 17+
- J-Obs requires Spring Boot 3.x (Jakarta EE)
- Spring Boot 2.x (javax namespace) is not supported

---

## Getting Help

If your issue isn't covered here:

1. **Check GitHub Issues:** https://github.com/JohnPitter/j-obs/issues
2. **Open a new issue** with:
   - J-Obs version
   - Spring Boot version
   - Java version
   - Full stack trace
   - Minimal reproduction steps

---

## Common Error Messages Quick Reference

| Error Message | Solution |
|---------------|----------|
| `WebSocketConfigurer not found` | Use v1.0.1+ or add websocket starter |
| `ServerContainer not available` | Use v1.0.10+ (auto-detected via `@ConditionalOnServerContainer`) or set `j-obs.enabled=false` |
| `POM is invalid` | Use v1.0.4+ |
| `Could not find artifact io.github.j-obs` | Change groupId to `io.github.johnpitter` |
| `BeanDefinitionOverrideException` | Remove duplicate bean definitions |
| `NoSuchMethodError` in OpenTelemetry | Check version compatibility |
| `NoClassDefFoundError: PrometheusMeterRegistry` | Use v1.0.10+ (supports both old and new Micrometer packages) |
| `NoSuchMethodError` in Prometheus classes | Use `j-obs-bom` (not `j-obs-parent`) or align Prometheus versions - see [Prometheus version conflicts](#prometheus-version-conflicts) |
| `GlobalOpenTelemetry.set has already been called` | Use v1.0.9+ or exclude Spring Boot tracing auto-config |
| `Context does not have an entry for key TracingContext` | Use v1.0.9+ (auto-fixed) or add ObservabilityConfig with NOOP registry |
| J-Obs configs load despite `j-obs.enabled=false` | Use v1.0.11+ where ALL configs respect `j-obs.enabled` |
