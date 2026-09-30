package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogEntry;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogSnapshot;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillInstanceHeartbeat;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderCatalogManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillDigestUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;

import io.agentscope.core.skill.AgentSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class RedisSkillRegistrySynchronizer {

    private final RedisSkillRegistry redisRegistry;
    private final SkillCatalogHolder catalogHolder;
    private final ApplicationEventPublisher eventPublisher;
    private final SkillRegistryProperties properties;

    public RedisSkillRegistrySynchronizer(
            RedisSkillRegistry redisRegistry,
            SkillCatalogHolder catalogHolder,
            ApplicationEventPublisher eventPublisher,
            SkillRegistryProperties properties) {
        this.redisRegistry = redisRegistry;
        this.catalogHolder = catalogHolder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "${" + CoreConstants.Yaml.PROJECT_CONFIG_PREFIX
                    + ".ai.dynamic-skill.reconciliation-initial-delay:3s}",
            fixedDelayString = "${" + CoreConstants.Yaml.PROJECT_CONFIG_PREFIX
                    + ".ai.dynamic-skill.reconciliation-interval:20s}")
    public synchronized void synchronize() {
        if (!properties.enabled()) {
            return;
        }
        try {
            doSynchronize();
        } catch (RuntimeException ex) {
            log.error("同步 Redis 动态技能目录失败，继续保留上一份本地快照", ex);
        }
    }

    private void doSynchronize() {
        Map<String, SkillInstanceHeartbeat> latestByApplication = new LinkedHashMap<>();
        for (String member : redisRegistry.instanceMembers()) {
            SkillInstanceHeartbeat heartbeat = redisRegistry.heartbeat(member);
            if (heartbeat == null) {
                redisRegistry.removeStaleMember(member);
                continue;
            }
            latestByApplication.merge(heartbeat.applicationName(), heartbeat,
                    (left, right) -> {
                        if (left.startedAt() != right.startedAt()) {
                            return left.startedAt() > right.startedAt() ? left : right;
                        }
                        return left.heartbeatAt() >= right.heartbeatAt() ? left : right;
                    });
        }

        List<SkillProviderCatalogManifest> providers = latestByApplication.values().stream()
                .map(redisRegistry::catalog)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SkillProviderCatalogManifest::applicationName))
                .toList();
        Map<String, SkillCatalogEntry> entries = new LinkedHashMap<>();
        Map<String, StandaloneToolManifest> standaloneTools = new LinkedHashMap<>();
        List<String> catalogRevisions = new ArrayList<>();
        List<String> toolkitRevisions = new ArrayList<>();
        for (SkillProviderCatalogManifest provider : providers) {
            catalogRevisions.add(provider.applicationName() + '=' + provider.catalogRevision());
            toolkitRevisions.add(provider.applicationName() + '=' + provider.toolkitRevision());
            for (SkillRegistrationManifest manifest : provider.registrations()) {
                String content = redisRegistry.content(manifest.contentKey());
                if (content == null) {
                    throw new IllegalStateException("动态 Skill 正文不存在：" + manifest.skillId());
                }
                AgentSkill agentSkill = AgentSkill.builder()
                        .name(manifest.skillId())
                        .description(manifest.description())
                        .source("redis-dynamic-skill")
                        .skillContent(content)
                        .build();
                if (entries.putIfAbsent(manifest.skillId(), new SkillCatalogEntry(manifest, agentSkill)) != null) {
                    throw new IllegalStateException("跨服务动态 skillId 冲突：" + manifest.skillId());
                }
            }
            for (StandaloneToolManifest manifest : provider.standaloneTools()) {
                String toolName = manifest.tool().exposedName();
                if (standaloneTools.putIfAbsent(toolName, manifest) != null) {
                    throw new IllegalStateException("跨服务独立动态工具名冲突：" + toolName);
                }
            }
        }
        SkillCatalogSnapshot replacement = new SkillCatalogSnapshot(
                SkillDigestUtils.sha256(String.join("|", catalogRevisions)),
                SkillDigestUtils.sha256(String.join("|", toolkitRevisions)),
                entries,
                standaloneTools);
        SkillCatalogSnapshot previous = catalogHolder.current();
        if (previous.catalogRevision().equals(replacement.catalogRevision())
                && previous.toolkitRevision().equals(replacement.toolkitRevision())) {
            return;
        }
        catalogHolder.replace(replacement);
        log.info("动态 AI 能力本地快照已更新，skills={}, standaloneTools={}, "
                        + "catalogRevision={}, toolkitRevision={}",
                entries.size(), standaloneTools.size(),
                replacement.catalogRevision(), replacement.toolkitRevision());
        if (!previous.toolkitRevision().equals(replacement.toolkitRevision())) {
            eventPublisher.publishEvent(new DynamicSkillToolkitChangedEvent(
                    previous.toolkitRevision(), replacement.toolkitRevision()));
        }
    }
}
