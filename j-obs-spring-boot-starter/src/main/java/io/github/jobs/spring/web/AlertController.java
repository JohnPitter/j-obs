package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controller for alert management HTML pages.
 */
@Controller
public class AlertController {

    private final JObsProperties properties;
    private final String alertsHtml;

    public AlertController(JObsProperties properties) {
        this.properties = properties;
        this.alertsHtml = loadResource("/static/j-obs/templates/alerts.html");
    }

    @GetMapping(value = "${j-obs.path:/j-obs}/alerts", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String alertsPage() {
        String basePath = properties.getPath();
        return alertsHtml.replace("{{BASE_PATH}}", basePath);
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
                <title>J-Obs - Alerts</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-900 text-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Alerts</h1>
                    <p class="text-gray-400 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="{{BASE_PATH}}" class="text-blue-400 hover:text-blue-300">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """;
    }
}
