package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.ServiceMapBuilder;
import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.servicemap.DefaultServiceMapBuilder;
import io.github.jobs.spring.web.ServiceMapApiController;
import io.github.jobs.spring.web.ServiceMapController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Service Map feature.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "j-obs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JObsServiceMapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TraceRepository.class)
    public ServiceMapBuilder serviceMapBuilder(TraceRepository traceRepository, JObsProperties properties) {
        ServiceMapBuilder.Config config = new ServiceMapBuilder.Config(
                properties.getServiceMap().getDefaultWindow(),
                properties.getServiceMap().getCacheExpiration(),
                properties.getServiceMap().getErrorThreshold(),
                properties.getServiceMap().getLatencyThreshold(),
                properties.getServiceMap().getMinRequestsForHealth()
        );
        return new DefaultServiceMapBuilder(traceRepository, config);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ServiceMapBuilder.class)
    public ServiceMapController serviceMapController() {
        return new ServiceMapController();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ServiceMapBuilder.class)
    public ServiceMapApiController serviceMapApiController(ServiceMapBuilder serviceMapBuilder) {
        return new ServiceMapApiController(serviceMapBuilder);
    }
}
