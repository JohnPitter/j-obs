package io.github.jobs.domain.sql;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlIssueTest {

    @Test
    void shouldBuildSqlIssue() {
        Instant now = Instant.now();
        SqlIssue issue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.N_PLUS_ONE)
                .traceId("trace-123")
                .endpoint("GET /api/users")
                .query("SELECT * FROM users WHERE id = ?")
                .normalizedQuery("SELECT * FROM USERS WHERE ID = ?")
                .dbSystem("postgresql")
                .durationMs(1500)
                .occurrences(10)
                .detectedAt(now)
                .suggestion("Use JOIN instead")
                .relatedSpanIds(List.of("span-1", "span-2"))
                .build();

        assertEquals("issue-1", issue.id());
        assertEquals(SqlProblemType.N_PLUS_ONE, issue.type());
        assertEquals("trace-123", issue.traceId());
        assertEquals("GET /api/users", issue.endpoint());
        assertEquals("SELECT * FROM users WHERE id = ?", issue.query());
        assertEquals("SELECT * FROM USERS WHERE ID = ?", issue.normalizedQuery());
        assertEquals("postgresql", issue.dbSystem());
        assertEquals(1500, issue.durationMs());
        assertEquals(10, issue.occurrences());
        assertEquals(now, issue.detectedAt());
        assertEquals("Use JOIN instead", issue.suggestion());
        assertEquals(2, issue.relatedSpanIds().size());
    }

    @Test
    void shouldRequireIdAndType() {
        assertThrows(NullPointerException.class, () ->
                SqlIssue.builder()
                        .type(SqlProblemType.N_PLUS_ONE)
                        .build()
        );

        assertThrows(NullPointerException.class, () ->
                SqlIssue.builder()
                        .id("issue-1")
                        .build()
        );
    }

    @Test
    void shouldDefaultOccurrencesToOne() {
        SqlIssue issue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.SLOW_QUERY)
                .build();

        assertEquals(1, issue.occurrences());
    }

    @Test
    void shouldDefaultDetectedAtToNow() {
        Instant before = Instant.now();
        SqlIssue issue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.SLOW_QUERY)
                .build();
        Instant after = Instant.now();

        assertNotNull(issue.detectedAt());
        assertTrue(issue.detectedAt().compareTo(before) >= 0);
        assertTrue(issue.detectedAt().compareTo(after) <= 0);
    }

    @Test
    void shouldDefaultRelatedSpanIdsToEmptyList() {
        SqlIssue issue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.SLOW_QUERY)
                .build();

        assertNotNull(issue.relatedSpanIds());
        assertTrue(issue.relatedSpanIds().isEmpty());
    }

    @Test
    void shouldIdentifyCriticalIssue() {
        SqlIssue criticalIssue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.N_PLUS_ONE)
                .build();

        SqlIssue warningIssue = SqlIssue.builder()
                .id("issue-2")
                .type(SqlProblemType.SLOW_QUERY)
                .build();

        assertTrue(criticalIssue.isCritical());
        assertFalse(warningIssue.isCritical());
    }

    @Test
    void shouldCompareByIdForEquality() {
        SqlIssue issue1 = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.N_PLUS_ONE)
                .build();

        SqlIssue issue2 = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.SLOW_QUERY) // Different type
                .build();

        SqlIssue issue3 = SqlIssue.builder()
                .id("issue-2")
                .type(SqlProblemType.N_PLUS_ONE)
                .build();

        assertEquals(issue1, issue2); // Same ID = equal
        assertNotEquals(issue1, issue3); // Different ID = not equal
    }

    @Test
    void shouldHaveConsistentHashCode() {
        SqlIssue issue1 = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.N_PLUS_ONE)
                .build();

        SqlIssue issue2 = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.SLOW_QUERY)
                .build();

        assertEquals(issue1.hashCode(), issue2.hashCode());
    }

    @Test
    void shouldHaveReadableToString() {
        SqlIssue issue = SqlIssue.builder()
                .id("issue-1")
                .type(SqlProblemType.N_PLUS_ONE)
                .endpoint("GET /api/users")
                .occurrences(5)
                .durationMs(1000)
                .build();

        String str = issue.toString();
        assertTrue(str.contains("issue-1"));
        assertTrue(str.contains("N_PLUS_ONE"));
        assertTrue(str.contains("GET /api/users"));
    }
}
