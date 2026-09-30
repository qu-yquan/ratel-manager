package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

public final class DynamicSkillEventListener implements InitializingBean, DisposableBean {

    private final CacheUtils cacheUtils;
    private final RedisSkillRegistrySynchronizer synchronizer;
    private final SkillRegistryProperties properties;
    private RedisMessageListenerContainer listenerContainer;

    public DynamicSkillEventListener(
            CacheUtils cacheUtils,
            RedisSkillRegistrySynchronizer synchronizer,
            SkillRegistryProperties properties) {
        this.cacheUtils = cacheUtils;
        this.synchronizer = synchronizer;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.enabled()) {
            return;
        }
        listenerContainer = cacheUtils.withRebel(() -> cacheUtils.addListener(
                SkillRegistryKeys.EVENT,
                (message, pattern) -> synchronizer.synchronize()));
    }

    @Override
    public void destroy() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.destroy();
        }
    }
}
