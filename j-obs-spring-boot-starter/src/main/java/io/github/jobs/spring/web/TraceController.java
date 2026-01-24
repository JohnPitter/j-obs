package io.github.jobs.spring.web;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller for trace HTML pages.
 */
@Controller
public class TraceController {

    private final TraceRepository traceRepository;
    private final JObsProperties properties;
    private final String tracesListHtml;
    private final String traceDetailHtml;
    private final String traceNotFoundHtml;

    public TraceController(TraceRepository traceRepository, JObsProperties properties) {
        this.traceRepository = traceRepository;
        this.properties = properties;
        this.tracesListHtml = loadResource("/static/j-obs/templates/traces-list.html");
        this.traceDetailHtml = loadResource("/static/j-obs/templates/trace-detail.html");
        this.traceNotFoundHtml = loadResource("/static/j-obs/templates/trace-not-found.html");
    }

    @GetMapping("${j-obs.path:/j-obs}/traces")
    @ResponseBody
    public String tracesPage() {
        return tracesListHtml.replace("{{BASE_PATH}}", properties.getPath());
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return getDefaultListHtml();
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return getDefaultListHtml();
        }
    }

    private String getDefaultListHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>J-Obs - Traces</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Traces</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}" class="text-blue-400 hover:text-blue-300">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """;
    }

    @GetMapping("${j-obs.path:/j-obs}/traces/{traceId}")
    @ResponseBody
    public String traceDetailPage(@PathVariable String traceId) {
        return traceRepository.findByTraceId(traceId)
                .map(this::renderTraceDetail)
                .orElse(renderNotFound(traceId));
    }

    private String renderTraceDetail(Trace trace) {
        String basePath = properties.getPath();
        return traceDetailHtml
                .replace("{{BASE_PATH}}", basePath)
                .replace("{{TRACE_ID}}", trace.traceId())
                .replace("{{TRACE_ID_SHORT}}", trace.traceId().substring(0, 8))
                .replace("{{TRACE_ID_DISPLAY}}", trace.traceId().substring(0, 16) + "...");
    }

    private String renderNotFound(String traceId) {
        String basePath = properties.getPath();
        String safeTraceId = InputSanitizer.sanitizeTraceId(traceId);
        if (safeTraceId == null) {
            safeTraceId = "(invalid)";
        }
        return traceNotFoundHtml
                .replace("{{BASE_PATH}}", basePath)
                .replace("{{TRACE_ID}}", safeTraceId);
    }

    private String getDefaultDetailHtml() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>J-Obs - Trace Detail</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Trace Detail</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}/traces" class="text-blue-400 hover:text-blue-300">Back to Traces</a>
                </div>
            </body>
            </html>
            """;
    }
}
