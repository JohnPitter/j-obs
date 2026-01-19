package io.github.jobs.domain.sql;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SqlQueryTest {

    @Test
    void shouldNormalizeSqlStatement() {
        String sql = "SELECT * FROM users WHERE id = 123 AND name = 'John'";
        String normalized = SqlQuery.normalize(sql);

        assertEquals("SELECT * FROM USERS WHERE ID = ? AND NAME = ?", normalized);
    }

    @Test
    void shouldNormalizeInLists() {
        String sql = "SELECT * FROM users WHERE id IN (1, 2, 3, 4)";
        String normalized = SqlQuery.normalize(sql);

        assertTrue(normalized.contains("IN (?)"));
    }

    @Test
    void shouldHandleNullAndEmpty() {
        assertNull(SqlQuery.normalize(null));
        assertEquals("", SqlQuery.normalize(""));
    }

    @Test
    void shouldExtractSelectOperation() {
        assertEquals("SELECT", SqlQuery.extractOperation("SELECT * FROM users"));
        assertEquals("SELECT", SqlQuery.extractOperation("  select id from users"));
    }

    @Test
    void shouldExtractInsertOperation() {
        assertEquals("INSERT", SqlQuery.extractOperation("INSERT INTO users (name) VALUES ('John')"));
    }

    @Test
    void shouldExtractUpdateOperation() {
        assertEquals("UPDATE", SqlQuery.extractOperation("UPDATE users SET name = 'John'"));
    }

    @Test
    void shouldExtractDeleteOperation() {
        assertEquals("DELETE", SqlQuery.extractOperation("DELETE FROM users WHERE id = 1"));
    }

    @Test
    void shouldReturnUnknownForInvalidOperation() {
        assertEquals("UNKNOWN", SqlQuery.extractOperation(null));
        assertEquals("UNKNOWN", SqlQuery.extractOperation(""));
    }

    @Test
    void shouldExtractTableFromSelect() {
        assertEquals("users", SqlQuery.extractTable("SELECT * FROM users WHERE id = 1"));
        assertEquals("orders", SqlQuery.extractTable("SELECT id, name FROM orders"));
    }

    @Test
    void shouldExtractTableFromInsert() {
        assertEquals("users", SqlQuery.extractTable("INSERT INTO users (name) VALUES ('John')"));
    }

    @Test
    void shouldExtractTableFromUpdate() {
        assertEquals("users", SqlQuery.extractTable("UPDATE users SET name = 'John'"));
    }

    @Test
    void shouldExtractTableFromDelete() {
        assertEquals("users", SqlQuery.extractTable("DELETE FROM users WHERE id = 1"));
    }

    @Test
    void shouldReturnNullForNoTable() {
        assertNull(SqlQuery.extractTable(null));
        assertNull(SqlQuery.extractTable(""));
    }

    @Test
    void shouldDetectSelectStar() {
        SqlQuery query = createQuery("SELECT * FROM users", "SELECT");
        assertTrue(query.usesSelectStar());
    }

    @Test
    void shouldNotDetectSelectStarForSpecificColumns() {
        SqlQuery query = createQuery("SELECT id, name FROM users", "SELECT");
        assertFalse(query.usesSelectStar());
    }

    @Test
    void shouldDetectMissingLimit() {
        SqlQuery query = createQuery("SELECT * FROM users", "SELECT");
        assertTrue(query.missingLimit());
    }

    @Test
    void shouldNotDetectMissingLimitWhenPresent() {
        SqlQuery queryWithLimit = createQuery("SELECT * FROM users LIMIT 10", "SELECT");
        assertFalse(queryWithLimit.missingLimit());

        SqlQuery queryWithTop = createQuery("SELECT TOP 10 * FROM users", "SELECT");
        assertFalse(queryWithTop.missingLimit());

        SqlQuery queryWithFetch = createQuery("SELECT * FROM users FETCH FIRST 10 ROWS ONLY", "SELECT");
        assertFalse(queryWithFetch.missingLimit());
    }

    @Test
    void shouldNotDetectMissingLimitForNonSelect() {
        SqlQuery query = createQuery("INSERT INTO users (name) VALUES ('John')", "INSERT");
        assertFalse(query.missingLimit());
    }

    @Test
    void shouldCalculateSimilarity() {
        SqlQuery query1 = createQueryWithNormalized("SELECT * FROM users WHERE id = 1", "SELECT * FROM USERS WHERE ID = ?");
        SqlQuery query2 = createQueryWithNormalized("SELECT * FROM users WHERE id = 2", "SELECT * FROM USERS WHERE ID = ?");

        assertEquals(1.0, query1.similarityTo(query2));
    }

    @Test
    void shouldCalculateDifferentSimilarity() {
        SqlQuery query1 = createQueryWithNormalized("SELECT * FROM users", "SELECT * FROM USERS");
        SqlQuery query2 = createQueryWithNormalized("SELECT * FROM orders", "SELECT * FROM ORDERS");

        double similarity = query1.similarityTo(query2);
        assertTrue(similarity < 1.0);
        assertTrue(similarity > 0.0);
    }

    @Test
    void shouldReturnZeroSimilarityForNull() {
        SqlQuery query1 = createQuery("SELECT * FROM users", "SELECT");

        assertEquals(0.0, query1.similarityTo(null));
    }

    private SqlQuery createQuery(String statement, String operation) {
        return new SqlQuery(
                "span-1",
                "trace-1",
                statement,
                SqlQuery.normalize(statement),
                operation,
                "postgresql",
                "testdb",
                100,
                Instant.now(),
                "GET /api/users"
        );
    }

    private SqlQuery createQueryWithNormalized(String statement, String normalized) {
        return new SqlQuery(
                "span-1",
                "trace-1",
                statement,
                normalized,
                "SELECT",
                "postgresql",
                "testdb",
                100,
                Instant.now(),
                "GET /api/users"
        );
    }
}
