package io.github.jobs.spring.web;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.domain.DependencyCheckResult;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Main controller that serves the J-Obs dashboard.
 * Handles the HTML pages and redirects based on dependency status.
 */
@Controller
public class JObsController {

    private final DependencyChecker dependencyChecker;
    private final JObsProperties properties;
    private final String dashboardHtml;
    private final String requirementsHtml;

    public JObsController(DependencyChecker dependencyChecker, JObsProperties properties) {
        this.dependencyChecker = dependencyChecker;
        this.properties = properties;
        this.dashboardHtml = loadResource("/static/j-obs/index.html");
        this.requirementsHtml = loadResource("/static/j-obs/requirements.html");
    }

    @GetMapping("${j-obs.path:/j-obs}")
    @ResponseBody
    public String dashboard() {
        DependencyCheckResult result = dependencyChecker.check();

        if (result.isComplete()) {
            return renderDashboard(result);
        } else {
            return renderRequirements(result);
        }
    }

    @GetMapping("${j-obs.path:/j-obs}/requirements")
    @ResponseBody
    public String requirements() {
        DependencyCheckResult result = dependencyChecker.check();
        return renderRequirements(result);
    }

    @GetMapping("${j-obs.path:/j-obs}/dashboard")
    @ResponseBody
    public String dashboardDirect() {
        DependencyCheckResult result = dependencyChecker.check();
        return renderDashboard(result);
    }

    private String renderDashboard(DependencyCheckResult result) {
        return dashboardHtml
                .replace("{{STATUS}}", result.isComplete() ? "healthy" : "degraded")
                .replace("{{FOUND_COUNT}}", String.valueOf(result.foundCount()))
                .replace("{{TOTAL_COUNT}}", String.valueOf(result.totalCount()))
                .replace("{{MISSING_OPTIONAL}}", String.valueOf(result.missingOptionalCount()))
                .replace("{{BASE_PATH}}", escapeHtml(properties.getPath()));
    }

    private String renderRequirements(DependencyCheckResult result) {
        StringBuilder dependencyRows = new StringBuilder();

        result.statuses().forEach(status -> {
            String icon = status.isFound() ? "check-circle" :
                         (status.dependency().isRequired() ? "x-circle" : "alert-circle");
            String colorClass = status.isFound() ? "text-green-500" :
                               (status.dependency().isRequired() ? "text-red-500" : "text-yellow-500");
            String statusText = status.isFound() ?
                               (status.version().isPresent() ? "v" + escapeHtml(status.version().get()) : "Detected") :
                               "Not found";

            dependencyRows.append(String.format("""
                <tr class="border-b border-gray-700">
                    <td class="py-3 px-4">
                        <div class="flex items-center gap-2">
                            <svg class="w-5 h-5 %s" data-icon="%s"></svg>
                            <span>%s</span>
                        </div>
                    </td>
                    <td class="py-3 px-4 text-gray-400">%s</td>
                    <td class="py-3 px-4">
                        <span class="%s">%s</span>
                    </td>
                    <td class="py-3 px-4">
                        <span class="px-2 py-1 rounded text-xs %s">%s</span>
                    </td>
                </tr>
                """,
                colorClass, icon,
                escapeHtml(status.dependency().displayName()),
                escapeHtml(status.dependency().mavenCoordinates()),
                colorClass, statusText,
                status.dependency().isRequired() ? "bg-red-900/50 text-red-300" : "bg-yellow-900/50 text-yellow-300",
                status.dependency().isRequired() ? "Required" : "Optional"
            ));
        });

        StringBuilder missingSnippets = new StringBuilder();
        result.missingRequired().forEach(status -> {
            missingSnippets.append(String.format("""
                <div class="bg-gray-800 rounded-lg p-4 mb-4">
                    <h4 class="font-medium text-red-400 mb-2">%s</h4>
                    <p class="text-gray-400 text-sm mb-3">%s</p>
                    <pre class="bg-gray-900 p-3 rounded text-sm overflow-x-auto"><code class="text-green-400">&lt;dependency&gt;
    &lt;groupId&gt;%s&lt;/groupId&gt;
    &lt;artifactId&gt;%s&lt;/artifactId&gt;
&lt;/dependency&gt;</code></pre>
                </div>
                """,
                escapeHtml(status.dependency().displayName()),
                escapeHtml(status.dependency().description() != null ? status.dependency().description() : ""),
                escapeHtml(status.dependency().groupId()),
                escapeHtml(status.dependency().artifactId())
            ));
        });

        return requirementsHtml
                .replace("{{DEPENDENCY_ROWS}}", dependencyRows.toString())
                .replace("{{MISSING_SNIPPETS}}", missingSnippets.toString())
                .replace("{{MISSING_REQUIRED_COUNT}}", String.valueOf(result.missingRequiredCount()))
                .replace("{{MISSING_OPTIONAL_COUNT}}", String.valueOf(result.missingOptionalCount()))
                .replace("{{STATUS_CLASS}}", result.isComplete() ? "text-green-500" : "text-red-500")
                .replace("{{STATUS_TEXT}}", result.isComplete() ? "All requirements met" : result.missingRequiredCount() + " required dependencies missing")
                .replace("{{BASE_PATH}}", escapeHtml(properties.getPath()));
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return getDefaultHtml(path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return getDefaultHtml(path);
        }
    }

    private String getDefaultHtml(String path) {
        // Minimal fallback HTML if resource files are not found
        String title = path.contains("requirements") ? "Requirements Check" : "Dashboard";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>J-Obs - %s</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}" class="text-blue-400 hover:text-blue-300">Retry</a>
                </div>
            </body>
            </html>
            """.formatted(title);
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
