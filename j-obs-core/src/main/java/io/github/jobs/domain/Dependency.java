package io.github.jobs.domain;

import java.util.List;
import java.util.Objects;

/**
 * Represents a dependency that J-Obs requires or optionally uses.
 * This is a value object - immutable and identified by its properties.
 * <p>
 * Supports alternative class names for dependencies that have changed
 * their package structure across versions (e.g., Micrometer 1.13+ changed
 * from {@code io.micrometer.prometheus} to {@code io.micrometer.prometheusmetrics}).
 */
public final class Dependency {

    private final String className;
    private final List<String> alternativeClassNames;
    private final String displayName;
    private final String artifactId;
    private final String groupId;
    private final boolean required;
    private final String documentationUrl;
    private final String description;

    private Dependency(Builder builder) {
        this.className = Objects.requireNonNull(builder.className, "className is required");
        this.alternativeClassNames = builder.alternativeClassNames != null
                ? List.copyOf(builder.alternativeClassNames)
                : List.of();
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName is required");
        this.artifactId = Objects.requireNonNull(builder.artifactId, "artifactId is required");
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId is required");
        this.required = builder.required;
        this.documentationUrl = builder.documentationUrl;
        this.description = builder.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String className() {
        return className;
    }

    /**
     * Returns alternative class names that should be tried if the primary class name is not found.
     * This supports libraries that changed their package structure across versions.
     *
     * @return immutable list of alternative class names, may be empty
     */
    public List<String> alternativeClassNames() {
        return alternativeClassNames;
    }

    /**
     * Returns all class names to try (primary first, then alternatives).
     *
     * @return list of all class names to check
     */
    public List<String> allClassNames() {
        if (alternativeClassNames.isEmpty()) {
            return List.of(className);
        }
        var all = new java.util.ArrayList<String>(alternativeClassNames.size() + 1);
        all.add(className);
        all.addAll(alternativeClassNames);
        return List.copyOf(all);
    }

    public String displayName() {
        return displayName;
    }

    public String artifactId() {
        return artifactId;
    }

    public String groupId() {
        return groupId;
    }

    public boolean isRequired() {
        return required;
    }

    public String documentationUrl() {
        return documentationUrl;
    }

    public String description() {
        return description;
    }

    public String mavenCoordinates() {
        return groupId + ":" + artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "displayName='" + displayName + '\'' +
                ", required=" + required +
                '}';
    }

    public static final class Builder {
        private String className;
        private List<String> alternativeClassNames;
        private String displayName;
        private String artifactId;
        private String groupId;
        private boolean required = true;
        private String documentationUrl;
        private String description;

        private Builder() {}

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        /**
         * Sets alternative class names to try if the primary class is not found.
         * Useful for libraries that changed their package structure across versions.
         *
         * @param alternativeClassNames alternative fully qualified class names
         * @return this builder
         */
        public Builder alternativeClassNames(String... alternativeClassNames) {
            this.alternativeClassNames = List.of(alternativeClassNames);
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Dependency build() {
            return new Dependency(this);
        }
    }
}
