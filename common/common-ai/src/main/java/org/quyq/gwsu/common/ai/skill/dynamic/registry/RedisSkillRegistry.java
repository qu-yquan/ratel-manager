package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillInstanceHeartbeat;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderCatalogManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.time.Duration;
import java.util.Set;

@RequiredArgsConstructor
public final class RedisSkillRegistry {

    private final CacheUtils cacheUtils;

    public void publish(
            SkillProviderIdentity identity,
            SkillProviderCatalogManifest catalog,
            java.util.Map<String, String> skillContents,
            Duration catalogTtl,
            Duration heartbeatTtl,
            boolean notify) {
        cacheUtils.withRebel(() -> {
            cacheUtils.set(
                    SkillRegistryKeys.provider(identity.applicationName(), catalog.catalogRevision()),
                    catalog,
                    catalogTtl);
            skillContents.forEach((key, content) -> cacheUtils.set(key, content, catalogTtl));
            heartbeat(identity, catalog, heartbeatTtl);
            if (notify) {
                cacheUtils.convertAndSend(SkillRegistryKeys.EVENT, catalog.toolkitRevision());
            }
            return null;
        });
    }

    public void heartbeat(
            SkillProviderIdentity identity,
            SkillProviderCatalogManifest catalog,
            Duration heartbeatTtl) {
        cacheUtils.withRebel(() -> {
            String member = SkillRegistryKeys.instanceMember(identity);
            cacheUtils.sAdd(SkillRegistryKeys.INSTANCES, member);
            SkillInstanceHeartbeat previous = cacheUtils.get(SkillRegistryKeys.instance(member));
            long now = System.currentTimeMillis();
            cacheUtils.set(
                    SkillRegistryKeys.instance(member),
                    new SkillInstanceHeartbeat(
                            identity.applicationName(), identity.instanceId(),
                            catalog.catalogRevision(), catalog.toolkitRevision(),
                            previous == null ? now : previous.startedAt(), now),
                    heartbeatTtl);
            return null;
        });
    }

    public Set<String> instanceMembers() {
        return cacheUtils.withRebel(() -> cacheUtils.sMembers(SkillRegistryKeys.INSTANCES));
    }

    public SkillInstanceHeartbeat heartbeat(String member) {
        return cacheUtils.withRebel(() -> cacheUtils.get(SkillRegistryKeys.instance(member)));
    }

    public SkillProviderCatalogManifest catalog(SkillInstanceHeartbeat heartbeat) {
        return cacheUtils.withRebel(() -> cacheUtils.get(SkillRegistryKeys.provider(
                heartbeat.applicationName(), heartbeat.catalogRevision())));
    }

    public String content(String contentKey) {
        return cacheUtils.withRebel(() -> cacheUtils.get(contentKey));
    }

    public void removeStaleMember(String member) {
        cacheUtils.withRebel(() -> {
            cacheUtils.sRemove(SkillRegistryKeys.INSTANCES, member);
            return null;
        });
    }
}
