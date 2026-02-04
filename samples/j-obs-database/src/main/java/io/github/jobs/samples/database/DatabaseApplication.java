package io.github.jobs.samples.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Database/SQL Analyzer J-Obs sample application.
 *
 * Demonstrates J-Obs SQL analysis features including:
 * - N+1 query detection
 * - Slow query detection
 * - Missing index suggestions
 * - JPA/Hibernate integration
 *
 * Access the dashboard at http://localhost:8087/j-obs
 * SQL Analyzer available at http://localhost:8087/j-obs/sql-analyzer
 */
@SpringBootApplication
public class DatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatabaseApplication.class, args);
    }
}
