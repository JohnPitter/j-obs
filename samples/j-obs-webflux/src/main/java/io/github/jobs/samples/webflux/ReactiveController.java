package io.github.jobs.samples.webflux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Reactive REST controller demonstrating WebFlux tracing.
 * All reactive streams are automatically traced by J-Obs.
 */
@RestController
@RequestMapping("/api")
public class ReactiveController {

    private static final Logger log = LoggerFactory.getLogger(ReactiveController.class);

    @GetMapping("/hello")
    public Mono<Map<String, String>> hello() {
        log.info("Reactive hello endpoint called");
        return Mono.just(Map.of("message", "Hello from J-Obs WebFlux!"));
    }

    @GetMapping("/hello/{name}")
    public Mono<Map<String, String>> helloName(@PathVariable String name) {
        log.info("Reactive hello endpoint called with name: {}", name);
        return Mono.just(Map.of("message", "Hello, " + name + "!"));
    }

    @GetMapping("/delayed/{seconds}")
    public Mono<Map<String, Object>> delayedResponse(@PathVariable int seconds) {
        log.info("Delayed endpoint called with {}s delay", seconds);
        return Mono.delay(Duration.ofSeconds(seconds))
            .map(tick -> Map.<String, Object>of(
                "message", "Response after delay",
                "delaySeconds", seconds
            ))
            .doOnSuccess(result -> log.info("Delayed response completed"));
    }

    @GetMapping("/stream/{count}")
    public Flux<Map<String, Object>> streamNumbers(@PathVariable int count) {
        log.info("Stream endpoint called for {} items", count);
        return Flux.range(1, count)
            .delayElements(Duration.ofMillis(100))
            .map(i -> Map.<String, Object>of(
                "number", i,
                "timestamp", System.currentTimeMillis()
            ))
            .doOnComplete(() -> log.info("Stream completed"));
    }

    @GetMapping("/parallel")
    public Mono<Map<String, Object>> parallelOperations() {
        log.info("Parallel operations endpoint called");

        Mono<String> operation1 = Mono.delay(Duration.ofMillis(100))
            .map(tick -> "Result from operation 1")
            .doOnSubscribe(sub -> log.info("Starting operation 1"));

        Mono<String> operation2 = Mono.delay(Duration.ofMillis(150))
            .map(tick -> "Result from operation 2")
            .doOnSubscribe(sub -> log.info("Starting operation 2"));

        Mono<String> operation3 = Mono.delay(Duration.ofMillis(200))
            .map(tick -> "Result from operation 3")
            .doOnSubscribe(sub -> log.info("Starting operation 3"));

        return Mono.zip(operation1, operation2, operation3)
            .map(tuple -> Map.<String, Object>of(
                "op1", tuple.getT1(),
                "op2", tuple.getT2(),
                "op3", tuple.getT3(),
                "message", "All parallel operations completed"
            ))
            .doOnSuccess(result -> log.info("All parallel operations completed"));
    }

    @GetMapping("/error")
    public Mono<Map<String, String>> errorEndpoint() {
        log.error("Error endpoint called - simulating reactive error");
        return Mono.error(new RuntimeException("Simulated reactive error for testing"));
    }
}
