package io.github.jobs.domain.alert;

import java.time.Duration;

/**
 * Configuration for alert throttling and rate limiting.
 */
public record ThrottleConfig(
        int rateLimit,
        Duration ratePeriod,
        Duration cooldown,
        boolean grouping,
        Duration groupWait,
        Duration repeatInterval
) {
    public ThrottleConfig {
        if (rateLimit < 0) {
            throw new IllegalArgumentException("rateLimit must be non-negative");
        }
        if (ratePeriod == null) {
            ratePeriod = Duration.ofMinutes(1);
        }
        if (cooldown == null) {
            cooldown = Duration.ofMinutes(5);
        }
        if (groupWait == null) {
            groupWait = Duration.ofSeconds(30);
        }
        if (repeatInterval == null) {
            repeatInterval = Duration.ofHours(4);
        }
    }

    public static ThrottleConfig defaults() {
        return new ThrottleConfig(10, Duration.ofMinutes(1), Duration.ofMinutes(5), true, Duration.ofSeconds(30), Duration.ofHours(4));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int rateLimit = 10;
        private Duration ratePeriod = Duration.ofMinutes(1);
        private Duration cooldown = Duration.ofMinutes(5);
        private boolean grouping = true;
        private Duration groupWait = Duration.ofSeconds(30);
        private Duration repeatInterval = Duration.ofHours(4);

        public Builder rateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
            return this;
        }

        public Builder ratePeriod(Duration ratePeriod) {
            this.ratePeriod = ratePeriod;
            return this;
        }

        public Builder cooldown(Duration cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder grouping(boolean grouping) {
            this.grouping = grouping;
            return this;
        }

        public Builder groupWait(Duration groupWait) {
            this.groupWait = groupWait;
            return this;
        }

        public Builder repeatInterval(Duration repeatInterval) {
            this.repeatInterval = repeatInterval;
            return this;
        }

        public ThrottleConfig build() {
            return new ThrottleConfig(rateLimit, ratePeriod, cooldown, grouping, groupWait, repeatInterval);
        }
    }
}
