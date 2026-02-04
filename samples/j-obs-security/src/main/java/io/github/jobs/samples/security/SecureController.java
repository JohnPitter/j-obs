package io.github.jobs.samples.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected REST controller demonstrating security tracing.
 * Shows how J-Obs tracks authentication and authorization events.
 */
@RestController
@RequestMapping("/api")
public class SecureController {

    private static final Logger log = LoggerFactory.getLogger(SecureController.class);

    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        log.info("Public endpoint accessed");
        return Map.of("message", "This is a public endpoint - no authentication required");
    }

    @GetMapping("/protected")
    public Map<String, Object> protectedEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Protected endpoint accessed by user: {}", auth.getName());
        return Map.of(
            "message", "This endpoint requires authentication",
            "user", auth.getName(),
            "authorities", auth.getAuthorities().toString()
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin endpoint accessed by user: {}", auth.getName());
        return Map.of(
            "message", "This endpoint requires ADMIN role",
            "user", auth.getName(),
            "authorities", auth.getAuthorities().toString()
        );
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Map<String, Object> userEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User endpoint accessed by user: {}", auth.getName());
        return Map.of(
            "message", "This endpoint requires USER or ADMIN role",
            "user", auth.getName(),
            "authorities", auth.getAuthorities().toString()
        );
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Whoami endpoint accessed by user: {}", auth.getName());
        return Map.of(
            "username", auth.getName(),
            "authenticated", auth.isAuthenticated(),
            "authorities", auth.getAuthorities().toString(),
            "principal", auth.getPrincipal().toString()
        );
    }
}
