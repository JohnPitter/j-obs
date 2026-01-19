package io.github.jobs.domain.sql;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a detected SQL issue in the application.
 */
public final class SqlIssue {

    private final String id;
    private final SqlProblemType type;
    private final String traceId;
    private final String endpoint;
    private final String query;
    private final String normalizedQuery;
    private final String dbSystem;
    private final long durationMs;
    private final int occurrences;
    private final Instant detectedAt;
    private final String suggestion;
    private final List<String> relatedSpanIds;

    private SqlIssue(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.type = Objects.requireNonNull(builder.type, "type is required");
        this.traceId = builder.traceId;
        this.endpoint = builder.endpoint;
        this.query = builder.query;
        this.normalizedQuery = builder.normalizedQuery;
        this.dbSystem = builder.dbSystem;
        this.durationMs = builder.durationMs;
        this.occurrences = builder.occurrences;
        this.detectedAt = builder.detectedAt != null ? builder.detectedAt : Instant.now();
        this.suggestion = builder.suggestion;
        this.relatedSpanIds = builder.relatedSpanIds != null ?
                List.copyOf(builder.relatedSpanIds) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public SqlProblemType type() {
        return type;
    }

    public String traceId() {
        return traceId;
    }

    public String endpoint() {
        return endpoint;
    }

    public String query() {
        return query;
    }

    public String normalizedQuery() {
        return normalizedQuery;
    }

    public String dbSystem() {
        return dbSystem;
    }

    public long durationMs() {
        return durationMs;
    }

    public int occurrences() {
        return occurrences;
    }

    public Instant detectedAt() {
        return detectedAt;
    }

    public String suggestion() {
        return suggestion;
    }

    public List<String> relatedSpanIds() {
        return relatedSpanIds;
    }

    public boolean isCritical() {
        return type.isCritical();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlIssue sqlIssue = (SqlIssue) o;
        return Objects.equals(id, sqlIssue.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SqlIssue{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", endpoint='" + endpoint + '\'' +
                ", occurrences=" + occurrences +
                ", durationMs=" + durationMs +
                '}';
    }

    public static final class Builder {
        private String id;
        private SqlProblemType type;
        private String traceId;
        private String endpoint;
        private String query;
        private String normalizedQuery;
        private String dbSystem;
        private long durationMs;
        private int occurrences = 1;
        private Instant detectedAt;
        private String suggestion;
        private List<String> relatedSpanIds;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(SqlProblemType type) {
            this.type = type;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder normalizedQuery(String normalizedQuery) {
            this.normalizedQuery = normalizedQuery;
            return this;
        }

        public Builder dbSystem(String dbSystem) {
            this.dbSystem = dbSystem;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder occurrences(int occurrences) {
            this.occurrences = occurrences;
            return this;
        }

        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }

        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        public Builder relatedSpanIds(List<String> relatedSpanIds) {
            this.relatedSpanIds = relatedSpanIds;
            return this;
        }

        public SqlIssue build() {
            return new SqlIssue(this);
        }
    }
}
