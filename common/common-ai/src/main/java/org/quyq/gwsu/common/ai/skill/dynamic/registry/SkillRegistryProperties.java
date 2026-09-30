package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".ai.dynamic-skill")
public record SkillRegistryProperties(
        Boolean enabled,
        Duration heartbeatInterval,
        Duration heartbeatTtl,
        Duration catalogTtl,
        Duration reconciliationInterval,
        Duration resourceCacheTtl,
        int resourceCacheMaxEntries,
        int resourceMaxBytes,
        Duration remoteConnectTimeout,
        Duration remoteReadTimeout
) {
    public SkillRegistryProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(10) : heartbeatInterval;
        heartbeatTtl = heartbeatTtl == null ? Duration.ofSeconds(30) : heartbeatTtl;
        catalogTtl = catalogTtl == null ? Duration.ofHours(1) : catalogTtl;
        reconciliationInterval = reconciliationInterval == null ? Duration.ofSeconds(20) : reconciliationInterval;
        resourceCacheTtl = resourceCacheTtl == null ? Duration.ofMinutes(10) : resourceCacheTtl;
        resourceCacheMaxEntries = resourceCacheMaxEntries <= 0 ? 256 : resourceCacheMaxEntries;
        resourceMaxBytes = resourceMaxBytes <= 0 ? 2 * 1024 * 1024 : resourceMaxBytes;
        remoteConnectTimeout = remoteConnectTimeout == null ? Duration.ofSeconds(3) : remoteConnectTimeout;
        remoteReadTimeout = remoteReadTimeout == null ? Duration.ofMinutes(2) : remoteReadTimeout;
    }

    public static SkillRegistryProperties defaults() {
        return new SkillRegistryProperties(
                Boolean.TRUE, null, null, null, null, null, 0, 0, null, null);
    }
}
