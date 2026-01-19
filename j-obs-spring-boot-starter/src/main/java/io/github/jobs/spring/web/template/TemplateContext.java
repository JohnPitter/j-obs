package io.github.jobs.spring.web.template;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context holder for template variables.
 */
public class TemplateContext {

    private final Map<String, Object> variables;

    private TemplateContext(Map<String, Object> variables) {
        this.variables = Collections.unmodifiableMap(variables);
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public String getString(String key) {
        Object value = variables.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Object> variables = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            variables.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> map) {
            variables.putAll(map);
            return this;
        }

        public Builder putIfNotNull(String key, Object value) {
            if (value != null) {
                variables.put(key, value);
            }
            return this;
        }

        public TemplateContext build() {
            return new TemplateContext(new LinkedHashMap<>(variables));
        }
    }
}
