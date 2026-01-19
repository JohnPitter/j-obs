package io.github.jobs.spring.web.template;

import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading and rendering HTML templates.
 * Templates are loaded from classpath resources and cached.
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
    private static final String TEMPLATES_PATH = "/static/j-obs/templates/";
    private static final String CSS_PATH = "/static/j-obs/css/";

    private final JObsProperties properties;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    private final Map<String, String> cssCache = new ConcurrentHashMap<>();

    public TemplateService(JObsProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a new template context builder.
     */
    public TemplateContext.Builder context() {
        return TemplateContext.builder()
                .put("basePath", properties.getPath());
    }

    /**
     * Renders a template with the given context.
     *
     * @param templateName Name of the template (without .html extension)
     * @param context      Template context with variables
     * @return Rendered HTML string
     */
    public String render(String templateName, TemplateContext context) {
        String template = loadTemplate(templateName);
        return processTemplate(template, context);
    }

    /**
     * Renders a template with default context (basePath only).
     */
    public String render(String templateName) {
        return render(templateName, context().build());
    }

    /**
     * Gets the layout template with navigation and common elements.
     */
    public String getLayout() {
        return loadTemplate("layout");
    }

    /**
     * Gets the shared CSS content.
     */
    public String getSharedCss() {
        return loadCss("shared");
    }

    /**
     * Wraps content in the layout template.
     */
    public String wrapInLayout(String content, String title, String activeNav, TemplateContext context) {
        String layout = getLayout();
        TemplateContext layoutContext = context().build();

        return layout
                .replace("{{TITLE}}", title)
                .replace("{{ACTIVE_NAV}}", activeNav)
                .replace("{{CONTENT}}", content)
                .replace("{{SHARED_CSS}}", getSharedCss())
                .replace("{{BASE_PATH}}", properties.getPath());
    }

    private String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, name -> {
            String path = TEMPLATES_PATH + name + ".html";
            return loadResource(path);
        });
    }

    private String loadCss(String cssName) {
        return cssCache.computeIfAbsent(cssName, name -> {
            String path = CSS_PATH + name + ".css";
            return loadResource(path);
        });
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Resource not found: {}", path);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load resource: {}", path, e);
            return "";
        }
    }

    private String processTemplate(String template, TemplateContext context) {
        String result = template;

        // Replace all context variables
        for (Map.Entry<String, Object> entry : context.getVariables().entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }

        // Always replace basePath
        result = result.replace("{{BASE_PATH}}", properties.getPath());

        return result;
    }

    /**
     * Clears the template cache (useful for development).
     */
    public void clearCache() {
        templateCache.clear();
        cssCache.clear();
        log.info("Template cache cleared");
    }
}
