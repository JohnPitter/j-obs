package io.github.jobs.application;

import io.github.jobs.domain.servicemap.ServiceMap;

import java.time.Duration;

/**
 * Interface for building service maps from trace data.
 */
public interface ServiceMapBuilder {

    /**
     * Builds a service map from traces within the specified time window.
     *
     * @param window the time window to analyze
     * @return the built service map
     */
    ServiceMap build(Duration window);

    /**
     * Builds a service map using the default time window.
     *
     * @return the built service map
     */
    default ServiceMap build() {
        return build(Duration.ofHours(1));
    }

    /**
     * Returns the current cached service map if available.
     *
     * @return the cached service map or an empty map if not cached
     */
    ServiceMap getCached();

    /**
     * Forces a refresh of the service map.
     *
     * @return the refreshed service map
     */
    ServiceMap refresh();

    /**
     * Configuration for the service map builder.
     */
    record Config(
            Duration defaultWindow,
            Duration cacheExpiration,
            double errorThreshold,
            double latencyThreshold,
            int minRequestsForHealth
    ) {
        public static Config defaults() {
            return new Config(
                    Duration.ofHours(1),
                    Duration.ofMinutes(1),
                    5.0,  // 5% error rate threshold
                    1000, // 1 second latency threshold
                    10    // minimum requests for health calculation
            );
        }
    }
}
