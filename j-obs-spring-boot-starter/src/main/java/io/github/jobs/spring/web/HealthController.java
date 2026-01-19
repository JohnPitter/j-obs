package io.github.jobs.spring.web;

import io.github.jobs.application.HealthRepository;
import io.github.jobs.domain.health.HealthCheckResult;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controller for health visualization pages.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}/health")
public class HealthController {

    private final HealthRepository healthRepository;
    private final JObsProperties properties;
    private final String healthHtml;

    public HealthController(HealthRepository healthRepository, JObsProperties properties) {
        this.healthRepository = healthRepository;
        this.properties = properties;
        this.healthHtml = loadResource("/static/j-obs/templates/health.html");
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String healthPage() {
        String basePath = properties.getPath();
        HealthCheckResult health = healthRepository.getHealth();

        return healthHtml
                .replace("{{BASE_PATH}}", basePath)
                .replace("{{STATUS}}", health.status().name())
                .replace("{{STATUS_DISPLAY_NAME}}", health.status().displayName())
                .replace("{{IS_HEALTHY}}", health.isHealthy() ? "true" : "false")
                .replace("{{SUMMARY}}", health.summary());
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return getDefaultHtml();
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return getDefaultHtml();
        }
    }

    private String getDefaultHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>J-Obs - Health</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Health</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}" class="text-blue-400 hover:text-blue-300">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """;
    }
}
