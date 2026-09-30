package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalSkillResourceRegistry {

    private final ConcurrentHashMap<String, SkillResourceProvider> providers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, SkillResourceProvider>> providerResources = new ConcurrentHashMap<>();

    public synchronized void replaceProviderResources(
            String applicationName,
            Map<String, SkillResourceProvider> replacements) {
        Map<String, SkillResourceProvider> oldProviders = providerResources.remove(applicationName);
        if (oldProviders != null) {
            oldProviders.forEach(providers::remove);
        }
        replacements.forEach((skillId, provider) -> {
            SkillResourceProvider previous = providers.putIfAbsent(skillId, provider);
            if (previous != null && previous != provider) {
                throw new IllegalStateException("动态技能资源提供者冲突：" + skillId);
            }
        });
        providerResources.put(applicationName, Map.copyOf(replacements));
    }

    public SkillResourceProvider requiredProvider(String skillId) {
        SkillResourceProvider provider = providers.get(skillId);
        if (provider == null) {
            throw new IllegalStateException("本地动态技能资源提供者不存在：" + skillId);
        }
        return provider;
    }
}
