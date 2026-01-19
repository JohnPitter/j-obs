package io.github.jobs.spring.alert;

import io.github.jobs.domain.alert.AlertEvent;
import io.github.jobs.domain.alert.AlertNotificationResult;
import io.github.jobs.domain.alert.AlertProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches alert notifications to configured providers.
 */
public class AlertDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);

    private final Map<String, AlertProvider> providers = new ConcurrentHashMap<>();
    private final AlertThrottler throttler;

    public AlertDispatcher(AlertThrottler throttler) {
        this.throttler = throttler;
    }

    /**
     * Registers a provider.
     */
    public void registerProvider(AlertProvider provider) {
        providers.put(provider.getName(), provider);
        log.info("Registered alert provider: {} (configured: {}, enabled: {})",
                provider.getName(), provider.isConfigured(), provider.isEnabled());
    }

    /**
     * Unregisters a provider.
     */
    public void unregisterProvider(String name) {
        providers.remove(name);
    }

    /**
     * Returns all registered providers.
     */
    public List<AlertProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /**
     * Returns configured and enabled providers.
     */
    public List<AlertProvider> getConfiguredProviders() {
        return providers.values().stream()
                .filter(AlertProvider::isConfigured)
                .filter(AlertProvider::isEnabled)
                .toList();
    }

    /**
     * Finds a provider by name.
     */
    public Optional<AlertProvider> findProvider(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * Dispatches an alert event to all configured providers.
     * Uses atomic throttle check to prevent race conditions.
     */
    public CompletableFuture<List<AlertNotificationResult>> dispatch(AlertEvent event) {
        // Use atomic tryAcquire to prevent race conditions
        if (!throttler.tryAcquire(event)) {
            log.debug("Alert {} throttled, skipping notification", event.alertId());
            return CompletableFuture.completedFuture(List.of(
                    AlertNotificationResult.failure("throttler", "Alert was throttled")
            ));
        }

        List<AlertProvider> activeProviders = getConfiguredProviders();
        if (activeProviders.isEmpty()) {
            log.debug("No configured providers, skipping notification for {}", event.alertId());
            return CompletableFuture.completedFuture(List.of());
        }

        log.info("Dispatching alert {} to {} provider(s)", event.alertId(), activeProviders.size());

        List<CompletableFuture<AlertNotificationResult>> futures = activeProviders.stream()
                .map(provider -> sendToProvider(provider, event))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * Dispatches an alert event to a specific provider.
     */
    public CompletableFuture<AlertNotificationResult> dispatchTo(AlertEvent event, String providerName) {
        AlertProvider provider = providers.get(providerName);
        if (provider == null) {
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(providerName, "Provider not found: " + providerName)
            );
        }
        return sendToProvider(provider, event);
    }

    /**
     * Tests a provider by sending a test notification.
     */
    public CompletableFuture<AlertNotificationResult> testProvider(String providerName) {
        AlertProvider provider = providers.get(providerName);
        if (provider == null) {
            return CompletableFuture.completedFuture(
                    AlertNotificationResult.failure(providerName, "Provider not found: " + providerName)
            );
        }
        return provider.test();
    }

    private CompletableFuture<AlertNotificationResult> sendToProvider(AlertProvider provider, AlertEvent event) {
        return provider.send(event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to send alert to {}: {}", provider.getName(), error.getMessage());
                    } else if (result.success()) {
                        log.debug("Successfully sent alert to {}", provider.getName());
                    } else {
                        log.warn("Failed to send alert to {}: {}", provider.getName(), result.errorDetails());
                    }
                });
    }
}
