package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.SqlAnalyzer;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.sql.DefaultSqlAnalyzer;
import io.github.jobs.spring.sql.SqlAnalyzerConfig;
import io.github.jobs.spring.web.SqlAnalyzerApiController;
import io.github.jobs.spring.web.SqlAnalyzerController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs SQL Analyzer.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(TraceRepository.class)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsSqlAnalyzerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SqlAnalyzerConfig sqlAnalyzerConfig(JObsProperties properties) {
        JObsProperties.SqlAnalyzer config = properties.getSqlAnalyzer();
        SqlAnalyzerConfig analyzerConfig = new SqlAnalyzerConfig();
        analyzerConfig.setSlowQueryThreshold(config.getSlowQueryThreshold());
        analyzerConfig.setVerySlowQueryThreshold(config.getVerySlowQueryThreshold());
        analyzerConfig.setNPlusOneMinQueries(config.getNPlusOneMinQueries());
        analyzerConfig.setNPlusOneSimilarity(config.getNPlusOneSimilarity());
        analyzerConfig.setLargeResultSetThreshold(config.getLargeResultSetThreshold());
        analyzerConfig.setDetectSelectStar(config.isDetectSelectStar());
        analyzerConfig.setDetectMissingLimit(config.isDetectMissingLimit());
        analyzerConfig.setIgnorePatterns(config.getIgnorePatterns());
        return analyzerConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlAnalyzer sqlAnalyzer(TraceRepository traceRepository, SqlAnalyzerConfig config) {
        return new DefaultSqlAnalyzer(traceRepository, config);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlAnalyzerController sqlAnalyzerController(SqlAnalyzer sqlAnalyzer, JObsProperties properties) {
        return new SqlAnalyzerController(sqlAnalyzer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlAnalyzerApiController sqlAnalyzerApiController(SqlAnalyzer sqlAnalyzer, JObsProperties properties) {
        return new SqlAnalyzerApiController(sqlAnalyzer, properties);
    }
}
