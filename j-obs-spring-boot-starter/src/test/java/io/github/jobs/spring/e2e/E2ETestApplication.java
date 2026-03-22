package io.github.jobs.spring.e2e;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for E2E tests.
 * Relies on J-Obs auto-configuration (spring.factories) to create all beans.
 * No component scan needed — auto-configuration handles controller creation.
 */
@SpringBootApplication
public class E2ETestApplication {
}
