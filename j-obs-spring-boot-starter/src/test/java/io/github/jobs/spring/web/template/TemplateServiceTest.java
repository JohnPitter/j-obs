package io.github.jobs.spring.web.template;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TemplateService context-path resolution.
 */
class TemplateServiceTest {

    private static JObsProperties defaultProperties() {
        JObsProperties props = new JObsProperties();
        // default path is /j-obs
        return props;
    }

    // ==================== fullPath ====================

    @Test
    void fullPath_shouldReturnJustJObsPath_whenNoContextPathConfigured() {
        MockEnvironment env = new MockEnvironment();
        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.fullPath()).isEqualTo("/j-obs");
    }

    @Test
    void fullPath_shouldPrependServletContextPath() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("server.servlet.context-path", "/my-app");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.fullPath()).isEqualTo("/my-app/j-obs");
    }

    @Test
    void fullPath_shouldNormalizeTrailingSlashOnServletContextPath() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("server.servlet.context-path", "/my-app/");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.fullPath()).isEqualTo("/my-app/j-obs");
    }

    @Test
    void fullPath_shouldFallBackToWebFluxBasePath_whenServletContextPathAbsent() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.webflux.base-path", "/api");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.fullPath()).isEqualTo("/api/j-obs");
    }

    @Test
    void fullPath_shouldPreferServletContextPath_overWebFluxBasePath() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("server.servlet.context-path", "/servlet");
        env.setProperty("spring.webflux.base-path", "/webflux");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.fullPath()).isEqualTo("/servlet/j-obs");
    }

    // ==================== getContextPath ====================

    @Test
    void getContextPath_shouldReturnEmptyString_whenNoContextPathConfigured() {
        MockEnvironment env = new MockEnvironment();
        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.getContextPath()).isEqualTo("");
    }

    @Test
    void getContextPath_shouldReturnNormalizedServletContextPath() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("server.servlet.context-path", "/my-app/");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.getContextPath()).isEqualTo("/my-app");
    }

    @Test
    void getContextPath_shouldReturnWebFluxBasePath_whenServletContextPathAbsent() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.webflux.base-path", "/api");

        TemplateService service = new TemplateService(defaultProperties(), env);

        assertThat(service.getContextPath()).isEqualTo("/api");
    }
}
