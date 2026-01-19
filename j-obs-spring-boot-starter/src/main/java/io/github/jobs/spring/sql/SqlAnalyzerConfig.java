package io.github.jobs.spring.sql;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for the SQL Analyzer.
 */
public class SqlAnalyzerConfig {

    private Duration slowQueryThreshold = Duration.ofSeconds(1);
    private Duration verySlowQueryThreshold = Duration.ofSeconds(5);
    private int nPlusOneMinQueries = 5;
    private double nPlusOneSimilarity = 0.9;
    private int largeResultSetThreshold = 1000;
    private boolean detectSelectStar = true;
    private boolean detectMissingLimit = true;
    private List<String> ignorePatterns = List.of();

    public Duration getSlowQueryThreshold() {
        return slowQueryThreshold;
    }

    public void setSlowQueryThreshold(Duration slowQueryThreshold) {
        this.slowQueryThreshold = slowQueryThreshold;
    }

    public Duration getVerySlowQueryThreshold() {
        return verySlowQueryThreshold;
    }

    public void setVerySlowQueryThreshold(Duration verySlowQueryThreshold) {
        this.verySlowQueryThreshold = verySlowQueryThreshold;
    }

    public int getNPlusOneMinQueries() {
        return nPlusOneMinQueries;
    }

    public void setNPlusOneMinQueries(int nPlusOneMinQueries) {
        this.nPlusOneMinQueries = nPlusOneMinQueries;
    }

    public double getNPlusOneSimilarity() {
        return nPlusOneSimilarity;
    }

    public void setNPlusOneSimilarity(double nPlusOneSimilarity) {
        this.nPlusOneSimilarity = nPlusOneSimilarity;
    }

    public int getLargeResultSetThreshold() {
        return largeResultSetThreshold;
    }

    public void setLargeResultSetThreshold(int largeResultSetThreshold) {
        this.largeResultSetThreshold = largeResultSetThreshold;
    }

    public boolean isDetectSelectStar() {
        return detectSelectStar;
    }

    public void setDetectSelectStar(boolean detectSelectStar) {
        this.detectSelectStar = detectSelectStar;
    }

    public boolean isDetectMissingLimit() {
        return detectMissingLimit;
    }

    public void setDetectMissingLimit(boolean detectMissingLimit) {
        this.detectMissingLimit = detectMissingLimit;
    }

    public List<String> getIgnorePatterns() {
        return ignorePatterns;
    }

    public void setIgnorePatterns(List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }
}
