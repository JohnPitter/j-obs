package io.github.jobs.spring.webflux;

import io.github.jobs.spring.web.RateLimiter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Reactive WebFilter for rate limiting J-Obs API endpoints in WebFlux applications.
 * <p>
 * Uses the same sliding window algorithm as the servlet-based rate limiter,
 * but adapted for the reactive programming model.
 */
public class ReactiveRateLimitFilter implements WebFilter, Ordered {

    private final RateLimiter rateLimiter;
    private final String basePath;

    public ReactiveRateLimitFilter(RateLimiter rateLimiter, String basePath) {
        this.rateLimiter = rateLimiter;
        this.basePath = basePath;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Only rate limit API endpoints
        if (!path.startsWith(basePath + "/api/")) {
            return chain.filter(exchange);
        }

        String clientKey = getClientKey(exchange);

        if (rateLimiter.tryAcquire(clientKey)) {
            // Add rate limit headers
            int remaining = rateLimiter.getRemainingRequests(clientKey);
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            return chain.filter(exchange);
        }

        // Rate limited
        return writeRateLimitResponse(exchange);
    }

    private String getClientKey(ServerWebExchange exchange) {
        // Try to get real IP from X-Forwarded-For header
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().add("Retry-After", "60");

        String body = "{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
