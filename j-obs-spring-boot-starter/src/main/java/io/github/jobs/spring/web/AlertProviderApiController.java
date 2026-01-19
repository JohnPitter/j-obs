package io.github.jobs.spring.web;

import io.github.jobs.application.AlertService;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for alert notification providers.
 *
 * <p>Provides endpoints to query and test alert notification providers.
 * Providers are notification channels (Telegram, Slack, Teams, Email, Webhook)
 * used to send alert notifications.</p>
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/alert-providers} - List all providers</li>
 *   <li>{@code GET /api/alert-providers/configured} - List configured providers</li>
 *   <li>{@code GET /api/alert-providers/{name}} - Get specific provider</li>
 *   <li>{@code POST /api/alert-providers/{name}/test} - Send test notification</li>
 * </ul>
 *
 * <h2>Available Providers</h2>
 * <ul>
 *   <li>telegram - Telegram Bot API</li>
 *   <li>slack - Slack incoming webhooks</li>
 *   <li>teams - Microsoft Teams webhooks</li>
 *   <li>email - SMTP email</li>
 *   <li>webhook - Generic HTTP webhook</li>
 * </ul>
 *
 * @author J-Obs Team
 * @since 1.0.0
 * @see AlertApiController
 * @see AlertEventApiController
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/alert-providers")
public class AlertProviderApiController {

    private final AlertService alertService;

    public AlertProviderApiController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public AlertProvidersResponse getProviders() {
        List<AlertProvider> providers = alertService.getProviders();
        List<AlertProviderDto> dtos = providers.stream()
                .map(AlertProviderDto::from)
                .toList();

        long configured = providers.stream()
                .filter(AlertProvider::isConfigured)
                .filter(AlertProvider::isEnabled)
                .count();

        return new AlertProvidersResponse(dtos, providers.size(), configured);
    }

    @GetMapping(value = "/configured", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AlertProviderDto> getConfiguredProviders() {
        return alertService.getConfiguredProviders().stream()
                .map(AlertProviderDto::from)
                .toList();
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlertProviderDto> getProvider(@PathVariable String name) {
        return alertService.findProviderByName(name)
                .map(AlertProviderDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{name}/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<TestResultDto>> testProvider(@PathVariable String name) {
        return alertService.findProviderByName(name)
                .map(provider -> {
                    if (!provider.isConfigured()) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.badRequest().body(
                                        new TestResultDto(false, "Provider is not configured", null)
                                )
                        );
                    }

                    return alertService.testProvider(name)
                            .thenApply(result -> ResponseEntity.ok(new TestResultDto(
                                    result.success(),
                                    result.message(),
                                    result.errorDetails()
                            )));
                })
                .orElse(CompletableFuture.completedFuture(
                        ResponseEntity.notFound().build()
                ));
    }

    // DTOs

    public record AlertProvidersResponse(
            List<AlertProviderDto> providers,
            long total,
            long configured
    ) {}

    public record AlertProviderDto(
            String name,
            String displayName,
            boolean enabled,
            boolean configured
    ) {
        public static AlertProviderDto from(AlertProvider provider) {
            return new AlertProviderDto(
                    provider.getName(),
                    provider.getDisplayName(),
                    provider.isEnabled(),
                    provider.isConfigured()
            );
        }
    }

    public record TestResultDto(
            boolean success,
            String message,
            String errorDetails
    ) {}
}
