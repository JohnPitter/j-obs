package io.github.jobs.spring.web;

import io.github.jobs.application.LogRepository;
import io.github.jobs.domain.log.LogLevel;
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
 * Controller for log visualization pages.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}/logs")
public class LogController {

    private final LogRepository logRepository;
    private final JObsProperties properties;
    private final String logsHtml;

    public LogController(LogRepository logRepository, JObsProperties properties) {
        this.logRepository = logRepository;
        this.properties = properties;
        this.logsHtml = loadResource("/static/j-obs/templates/logs.html");
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String logsPage() {
        String basePath = properties.getPath();
        var stats = logRepository.stats();

        return logsHtml
                .replace("{{BASE_PATH}}", basePath)
                .replace("{{ERROR_COUNT}}", String.valueOf(stats.errorCount()))
                .replace("{{WARN_COUNT}}", String.valueOf(stats.warnCount()))
                .replace("{{INFO_COUNT}}", String.valueOf(stats.infoCount()))
                .replace("{{DEBUG_COUNT}}", String.valueOf(stats.debugCount()))
                .replace("{{TRACE_COUNT}}", String.valueOf(stats.traceCount()));
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
                <title>J-Obs - Logs</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Logs</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}" class="text-blue-400 hover:text-blue-300">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """;
    }
}
