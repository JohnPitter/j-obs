package io.github.jobs.samples.minimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simple REST controller demonstrating basic HTTP tracing.
 * All endpoints are automatically traced by J-Obs.
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public Map<String, String> hello() {
        log.info("Hello endpoint called");
        return Map.of("message", "Hello from J-Obs Minimal!");
    }

    @GetMapping("/hello/{name}")
    public Map<String, String> helloName(@PathVariable String name) {
        log.info("Hello endpoint called with name: {}", name);
        return Map.of("message", "Hello, " + name + "!");
    }

    @GetMapping("/slow")
    public Map<String, Object> slowEndpoint() throws InterruptedException {
        int delay = ThreadLocalRandom.current().nextInt(100, 500);
        log.info("Slow endpoint called, sleeping for {}ms", delay);
        Thread.sleep(delay);
        return Map.of(
            "message", "This was a slow response",
            "delayMs", delay
        );
    }

    @GetMapping("/error")
    public Map<String, String> errorEndpoint() {
        log.error("Error endpoint called - simulating an error");
        throw new RuntimeException("Simulated error for testing");
    }
}
