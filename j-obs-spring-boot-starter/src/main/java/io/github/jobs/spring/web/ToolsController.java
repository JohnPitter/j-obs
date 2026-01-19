package io.github.jobs.spring.web;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import io.github.jobs.spring.web.template.TemplateContext;
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
 * Controller for the Tools page with advanced observability features.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}/tools")
public class ToolsController {

    private final JObsProperties properties;
    private final TemplateService templateService;
    private final String toolsPageWrapper;
    private final String toolsContent;
    private final String toolsJs;

    public ToolsController(JObsProperties properties, TemplateService templateService) {
        this.properties = properties;
        this.templateService = templateService;
        this.toolsPageWrapper = loadResource("/static/j-obs/templates/tools-page-wrapper.html");
        this.toolsContent = loadResource("/static/j-obs/templates/tools.html");
        this.toolsJs = loadResource("/static/j-obs/js/tools.js");
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String toolsPage() {
        String basePath = properties.getPath();
        String css = templateService.getSharedCss();

        String content = toolsContent.replace("{{BASE_PATH}}", basePath);
        String js = toolsJs.replace("{{BASE_PATH}}", basePath);

        return toolsPageWrapper
                .replace("{{BASE_PATH}}", basePath)
                .replace("{{CSS}}", css)
                .replace("{{CONTENT}}", content)
                .replace("{{JS}}", js);
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
