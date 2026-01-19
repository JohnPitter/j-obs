package io.github.jobs.domain;

import java.util.Objects;

/**
 * Represents a dependency that J-Obs requires or optionally uses.
 * This is a value object - immutable and identified by its properties.
 */
public final class Dependency {

    private final String className;
    private final String displayName;
    private final String artifactId;
    private final String groupId;
    private final boolean required;
    private final String documentationUrl;
    private final String description;

    private Dependency(Builder builder) {
        this.className = Objects.requireNonNull(builder.className, "className is required");
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
