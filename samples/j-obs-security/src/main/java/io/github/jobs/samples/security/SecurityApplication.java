package io.github.jobs.samples.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Security-focused J-Obs sample application.
 *
 * Demonstrates J-Obs security features including:
 * - Basic Auth
 * - Form-based login
 * - API Key authentication
 * - Role-based access control
 *
 * Access the dashboard at http://localhost:8082/j-obs
 * Default credentials: admin/admin (ADMIN role), user/user (USER role)
 */
@SpringBootApplication
public class SecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}
