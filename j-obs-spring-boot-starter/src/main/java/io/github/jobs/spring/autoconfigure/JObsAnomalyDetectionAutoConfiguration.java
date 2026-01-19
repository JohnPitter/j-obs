package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.AnomalyDetector;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.anomaly.AnomalyDetectionConfig;
import io.github.jobs.spring.anomaly.DefaultAnomalyDetector;
import io.github.jobs.spring.web.AnomalyApiController;
import io.github.jobs.spring.web.AnomalyController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for Anomaly Detection.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "j-obs.anomaly-detection.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(TraceRepository.class)
@EnableConfigurationProperties(JObsProperties.class)
@EnableScheduling
public class JObsAnomalyDetectionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AnomalyDetectionConfig anomalyDetectionConfig(JObsProperties properties) {
        JObsProperties.AnomalyDetection props = properties.getAnomalyDetection();
        AnomalyDetectionConfig config = new AnomalyDetectionConfig();

        config.setEnabled(props.isEnabled());
        config.setDetectionInterval(props.getDetectionInterval());
        config.setBaselineWindow(props.getBaselineWindow());
        config.setMinSamplesForBaseline(props.getMinSamplesForBaseline());
        config.setLatencyZScoreThreshold(props.getLatencyZScoreThreshold());
        config.setLatencyMinIncreasePercent(props.getLatencyMinIncreasePercent());
        config.setErrorRateZScoreThreshold(props.getErrorRateZScoreThreshold());
        config.setErrorRateMinAbsolute(props.getErrorRateMinAbsolute());
        config.setTrafficZScoreThreshold(props.getTrafficZScoreThreshold());
        config.setAlertOnTrafficDecrease(props.isAlertOnTrafficDecrease());
        config.setRetentionPeriod(props.getRetentionPeriod());

        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public AnomalyDetector anomalyDetector(TraceRepository traceRepository, AnomalyDetectionConfig config) {
        return new DefaultAnomalyDetector(traceRepository, config);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnomalyController anomalyController(AnomalyDetector anomalyDetector, JObsProperties properties) {
        return new AnomalyController(anomalyDetector, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnomalyApiController anomalyApiController(AnomalyDetector anomalyDetector, AnomalyDetectionConfig config) {
        return new AnomalyApiController(anomalyDetector, config);
    }
}
