package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import io.github.jobs.spring.web.template.TemplateService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * UI Controller for Service Map page.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}")
public class ServiceMapController {

    private final JObsProperties properties;
    private final TemplateService templateService;
    private final String serviceMapHtml;

    public ServiceMapController(JObsProperties properties, TemplateService templateService) {
        this.properties = properties;
        this.templateService = templateService;
        this.serviceMapHtml = loadResource("/templates/service-map.html");
    }

    /**
     * Renders the service map visualization page.
     */
    @GetMapping(value = "/service-map", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String serviceMap() {
        String basePath = templateService.fullPath();
        return serviceMapHtml
                .replace("th:href=\"@{/j-obs}\"", "href=\"" + basePath + "\"")
                .replace("th:href=\"@{/j-obs/", "href=\"" + basePath + "/")
                .replace("'}\"", "\"");
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
                <title>J-Obs - Service Map</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-50 min-h-screen flex items-center justify-center">
                <div class="text-center">
                    <h1 class="text-3xl font-bold mb-4">J-Obs - Service Map</h1>
                    <p class="text-gray-500 mb-4">Template files not found. Please ensure the j-obs resources are properly packaged.</p>
                    <a href="%s" class="text-indigo-600 hover:text-indigo-500">Back to Dashboard</a>
                </div>
            </body>
            </html>
            """, templateService.fullPath());
    }
}
