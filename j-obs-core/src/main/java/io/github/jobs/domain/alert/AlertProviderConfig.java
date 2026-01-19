package io.github.jobs.domain.alert;

import java.util.List;
import java.util.Map;

/**
 * Describes the configuration schema for an alert provider.
 */
public record AlertProviderConfig(
        String providerName,
        String displayName,
        String description,
        List<ConfigField> fields
) {

    public record ConfigField(
            String name,
            String displayName,
            String description,
            FieldType type,
            boolean required,
            String defaultValue,
            Map<String, String> options
    ) {
        public ConfigField(String name, String displayName, String description, FieldType type, boolean required) {
            this(name, displayName, description, type, required, null, null);
        }
    }

    public enum FieldType {
        STRING,
        PASSWORD,
        NUMBER,
        BOOLEAN,
        LIST,
        SELECT
    }
}
