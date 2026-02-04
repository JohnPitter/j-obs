package io.github.jobs.samples.webflux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * Reactive service demonstrating non-blocking operations.
 */
@Service
public class ReactiveService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveService.class);
    private final Random random = new Random();

    public Mono<String> fetchData(String id) {
        log.info("Fetching data for id: {}", id);
        return Mono.delay(Duration.ofMillis(random.nextInt(100) + 50))
            .map(tick -> "Data for " + id)
            .doOnSuccess(data -> log.info("Data fetched: {}", data));
    }

    public Flux<String> fetchMultipleData(List<String> ids) {
        log.info("Fetching data for {} ids", ids.size());
        return Flux.fromIterable(ids)
            .flatMap(this::fetchData)
            .doOnComplete(() -> log.info("All data fetched"));
    }

    public Mono<String> processWithRetry(String input) {
        log.info("Processing with retry: {}", input);
        return Mono.defer(() -> {
                if (random.nextInt(3) == 0) {
                    log.warn("Simulating transient failure");
                    return Mono.error(new RuntimeException("Transient failure"));
                }
                return Mono.just("Processed: " + input);
            })
            .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(100)))
            .doOnSuccess(result -> log.info("Processing completed: {}", result));
    }

    public Mono<String> cachedOperation(String key) {
        log.info("Cache operation for key: {}", key);
        return Mono.delay(Duration.ofMillis(50))
            .map(tick -> "Cached value for " + key)
            .cache(Duration.ofMinutes(5));
    }
}
