package io.github.jobs.spring.web;

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
 * UI controller for anomaly detection pages.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}")
public class AnomalyController {

    private final JObsProperties properties;
    private final String anomaliesHtml;

    public AnomalyController(JObsProperties properties) {
        this.properties = properties;
        this.anomaliesHtml = loadResource("/templates/anomalies.html");
    }

    @GetMapping(value = "/anomalies", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String anomaliesPage() {
        String basePath = properties.getPath();
        return anomaliesHtml
                .replace("th:href=\"${basePath}\"", "href=\"" + basePath + "\"")
                .replace("th:href=\"${basePath + ", "href=\"" + basePath)
                .replace("'}\"", "\"")
                .replace("th:href=\"@{/j-obs}\"", "href=\"" + basePath + "\"")
                .replace("th:href=\"@{/j-obs/", "href=\"" + basePath + "/");
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
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>J-Obs - Anomaly Detection</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-100 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Anomaly Detection</h1>
                    <p class="text-gray-500 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="%s" class="text-indigo-600 hover:text-indigo-500">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """, properties.getPath());
    }
}
