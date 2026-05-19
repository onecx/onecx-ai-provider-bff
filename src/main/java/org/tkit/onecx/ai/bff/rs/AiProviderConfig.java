package org.tkit.onecx.ai.bff.rs;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Shell bff configuration
 */
@ConfigDocFilename("onecx-ai-provider-bff.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "onecx.ai.provider")
public interface AiProviderConfig {

    /**
     * Health check configuration
     */
    @WithName("health-check")
    HealthCheck healthCheck();

    /**
     * Cache configuration
     */
    interface HealthCheck {
        /**
         * Enable or disable caching
         */
        @WithDefault("true")
        @WithName("cache-enabled")
        boolean cacheEnabled();

        /**
         * Cache expiration time in seconds
         */
        @WithDefault("310S")
        @WithName("cache-expire-after")
        String cacheExpireAfter();

        /**
         * Enable or disable cache metrics
         */
        @WithDefault("true")
        @WithName("cache-metrics-enabled")
        boolean cacheMetricsEnabled();
    }
}
