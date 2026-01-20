package io.github.jobs.spring.autoconfigure;

import io.github.jobs.application.AlertEventRepository;
import io.github.jobs.application.AlertRepository;
import io.github.jobs.application.AlertService;
import io.github.jobs.application.HealthRepository;
import io.github.jobs.application.LogRepository;
import io.github.jobs.application.MetricRepository;
import io.github.jobs.domain.alert.ThrottleConfig;
import io.github.jobs.infrastructure.InMemoryAlertEventRepository;
import io.github.jobs.spring.alert.*;
import io.github.jobs.spring.alert.provider.*;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Providers;
import io.github.jobs.spring.autoconfigure.JObsProperties.Alerts.Throttling;
import io.github.jobs.spring.web.AlertEventApiController;
import io.github.jobs.spring.web.AlertProviderApiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Auto-configuration for J-Obs alert notification system.
 * <p>
 * This configuration is activated when {@code j-obs.alerts.enabled} is {@code true} (default)
 * and runs after {@link JObsAlertAutoConfiguration} to ensure alert rules are available.
 * <p>
 * Components configured:
 * <ul>
 *   <li>{@link AlertEventRepository} - Storage for fired alert events</li>
 *   <li>{@link AlertDispatcher} - Routes alerts to configured providers</li>
 *   <li>{@link AlertEngine} - Evaluates alert conditions against metrics/logs/health</li>
 *   <li>{@link AlertScheduler} - Periodic alert evaluation scheduler</li>
 *   <li>{@link AlertGrouper} - Groups similar alerts to reduce noise</li>
 *   <li>{@link AlertThrottler} - Rate limiting and cooldown for notifications</li>
 * </ul>
 * <p>
 * Notification providers (registered automatically):
 * <ul>
 *   <li>{@link TelegramAlertProvider} - Telegram Bot API</li>
 *   <li>{@link TeamsAlertProvider} - Microsoft Teams Webhook</li>
 *   <li>{@link SlackAlertProvider} - Slack Webhook</li>
 *   <li>{@link EmailAlertProvider} - SMTP Email</li>
 *   <li>{@link WebhookAlertProvider} - Generic HTTP Webhook</li>
 * </ul>
 * <p>
 * Configuration properties under {@code j-obs.alerts}:
 * <ul>
 *   <li>{@code enabled} - Enable/disable alerts (default: true)</li>
 *   <li>{@code evaluation-interval} - How often to check conditions (default: 15s)</li>
 *   <li>{@code throttling.*} - Rate limiting and grouping settings</li>
 *   <li>{@code providers.*} - Provider-specific configuration</li>
 * </ul>
 *
 * @see JObsAlertAutoConfiguration
 * @see JObsProperties.Alerts
 */
@AutoConfiguration(after = JObsAlertAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "j-obs.alerts.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JObsProperties.class)
public class JObsAlertNotificationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JObsAlertNotificationAutoConfiguration.class);

    private final JObsProperties properties;

    public JObsAlertNotificationAutoConfiguration(JObsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertEventRepository alertEventRepository() {
        return new InMemoryAlertEventRepository(properties.getAlerts().getMaxEvents());
    }

    @Bean
    @ConditionalOnMissingBean
    public ThrottleConfig throttleConfig() {
        Throttling throttling = properties.getAlerts().getThrottling();
        return ThrottleConfig.builder()
                .rateLimit(throttling.getRateLimit())
                .ratePeriod(throttling.getRatePeriod())
                .cooldown(throttling.getCooldown())
                .grouping(throttling.isGrouping())
                .groupWait(throttling.getGroupWait())
                .repeatInterval(throttling.getRepeatInterval())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertThrottler alertThrottler(ThrottleConfig throttleConfig) {
        return new AlertThrottler(throttleConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertDispatcher alertDispatcher(AlertThrottler alertThrottler) {
        AlertDispatcher dispatcher = new AlertDispatcher(alertThrottler);

        // Register providers with configured timeout
        Alerts alerts = properties.getAlerts();
        Providers providers = alerts.getProviders();
        java.time.Duration httpTimeout = alerts.getHttpTimeout();

        // Telegram
        TelegramAlertProvider telegramProvider = new TelegramAlertProvider(providers.getTelegram(), httpTimeout);
        dispatcher.registerProvider(telegramProvider);

        // Teams
        TeamsAlertProvider teamsProvider = new TeamsAlertProvider(providers.getTeams(), httpTimeout);
        dispatcher.registerProvider(teamsProvider);

        // Slack
        SlackAlertProvider slackProvider = new SlackAlertProvider(providers.getSlack(), httpTimeout);
        dispatcher.registerProvider(slackProvider);

        // Email
        EmailAlertProvider emailProvider = new EmailAlertProvider(providers.getEmail());
        dispatcher.registerProvider(emailProvider);

        // Webhook
        WebhookAlertProvider webhookProvider = new WebhookAlertProvider(providers.getWebhook(), httpTimeout);
        dispatcher.registerProvider(webhookProvider);

        log.info("Alert notification dispatcher configured with {} providers (timeout: {}s)",
                dispatcher.getAllProviders().size(), httpTimeout.toSeconds());

        return dispatcher;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AlertRepository.class, MetricRepository.class, LogRepository.class, HealthRepository.class})
    public AlertEngine alertEngine(
            AlertRepository alertRepository,
            AlertEventRepository alertEventRepository,
            MetricRepository metricRepository,
            LogRepository logRepository,
            HealthRepository healthRepository
    ) {
        return new AlertEngine(
                alertRepository,
                alertEventRepository,
                metricRepository,
                logRepository,
                healthRepository
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "jobsAlertTaskScheduler")
    public TaskScheduler jobsAlertTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("j-obs-alert-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    public AlertGrouper alertGrouper(
            AlertDispatcher alertDispatcher,
            TaskScheduler jobsAlertTaskScheduler
    ) {
        Throttling throttling = properties.getAlerts().getThrottling();
        java.util.concurrent.ScheduledExecutorService executor;
        if (jobsAlertTaskScheduler instanceof ThreadPoolTaskScheduler) {
            executor = ((ThreadPoolTaskScheduler) jobsAlertTaskScheduler).getScheduledThreadPoolExecutor();
        } else {
            executor = java.util.concurrent.Executors.newScheduledThreadPool(2);
        }
        return new AlertGrouper(
                throttling,
                alertDispatcher,
                executor
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AlertEngine.class)
    public AlertScheduler alertScheduler(
            AlertEngine alertEngine,
            AlertDispatcher alertDispatcher,
            AlertGrouper alertGrouper,
            AlertEventRepository alertEventRepository,
            TaskScheduler jobsAlertTaskScheduler
    ) {
        return new AlertScheduler(
                alertEngine,
                alertDispatcher,
                alertGrouper,
                alertEventRepository,
                jobsAlertTaskScheduler,
                properties.getAlerts()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AlertService.class)
    @ConditionalOnBean(AlertEngine.class)
    public DefaultAlertService alertService(
            AlertRepository alertRepository,
            AlertEventRepository alertEventRepository,
            AlertEngine alertEngine,
            AlertDispatcher alertDispatcher
    ) {
        return new DefaultAlertService(
                alertRepository,
                alertEventRepository,
                alertEngine,
                alertDispatcher
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AlertService.class)
    public AlertEventApiController alertEventApiController(
            AlertEventRepository alertEventRepository,
            AlertService alertService
    ) {
        return new AlertEventApiController(alertEventRepository, alertService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AlertService.class)
    public AlertProviderApiController alertProviderApiController(AlertService alertService) {
        return new AlertProviderApiController(alertService);
    }
}
