package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.spring.web.ToolsApiController;
import io.github.jobs.spring.web.ToolsController;
import io.github.jobs.spring.web.template.TemplateService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for J-Obs developer tools.
 */
@AutoConfiguration(after = JObsTraceAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsToolsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TemplateService templateService(JObsProperties properties) {
        return new TemplateService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TemplateService.class)
    public ToolsController toolsController(JObsProperties properties, TemplateService templateService) {
        return new ToolsController(properties, templateService);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolsApiController toolsApiController(ObjectProvider<TraceRepository> traceRepositoryProvider) {
        return new ToolsApiController(traceRepositoryProvider.getIfAvailable());
    }
}
