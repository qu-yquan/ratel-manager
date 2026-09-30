package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillBuilder;
import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillDefinition;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderCatalogManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.LocalSkillResourceRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillDigestUtils;
import org.quyq.gwsu.common.ai.skill.dynamic.support.SkillResourcePathValidator;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.LocalSkillToolRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.SkillToolNaming;
import org.quyq.gwsu.common.core.constants.CoreConstants;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class DynamicSkillRegistrar implements SmartInitializingSingleton {

    private final List<AgentSkillProvider> providers;
    private final SkillProviderIdentity identity;
    private final AgentScopeToolNormalizer toolNormalizer;
    private final LocalSkillToolRegistry localToolRegistry;
    private final LocalSkillResourceRegistry localResourceRegistry;
    private final RedisSkillRegistry redisRegistry;
    private final SkillRegistryProperties properties;
    private final ObjectMapper objectMapper;

    private volatile SkillProviderCatalogManifest catalog;
    private volatile Map<String, String> contents = Map.of();

    public DynamicSkillRegistrar(
            List<AgentSkillProvider> providers,
            SkillProviderIdentityResolver identityResolver,
            AgentScopeToolNormalizer toolNormalizer,
            LocalSkillToolRegistry localToolRegistry,
            LocalSkillResourceRegistry localResourceRegistry,
            RedisSkillRegistry redisRegistry,
            SkillRegistryProperties properties,
            ObjectMapper objectMapper) {
        this.providers = List.copyOf(providers);
        this.identity = identityResolver.identity();
        this.toolNormalizer = toolNormalizer;
        this.localToolRegistry = localToolRegistry;
        this.localResourceRegistry = localResourceRegistry;
        this.redisRegistry = redisRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.enabled()) {
            return;
        }
        rebuildAndPublish();
    }

    public synchronized void rebuildAndPublish() {
        SkillProviderCatalogManifest previous = catalog;
        CatalogBuildResult result = buildCatalog();
        catalog = result.catalog();
        contents = result.contents();
        boolean changed = previous == null
                || !previous.catalogRevision().equals(catalog.catalogRevision())
                || !previous.toolkitRevision().equals(catalog.toolkitRevision());
        redisRegistry.publish(identity, catalog, contents, properties.catalogTtl(),
                properties.heartbeatTtl(), changed);
        log.info("动态 AI 能力注册完成，applicationName={}, skills={}, standaloneTools={}, tools={}, "
                        + "catalogRevision={}, toolkitRevision={}",
                identity.applicationName(), catalog.registrations().size(),
                catalog.standaloneTools().size(),
                catalog.registrations().stream().mapToInt(v -> v.tools().size()).sum()
                        + catalog.standaloneTools().size(),
                catalog.catalogRevision(), catalog.toolkitRevision());
    }

    @Scheduled(fixedDelayString = "${" + CoreConstants.Yaml.PROJECT_CONFIG_PREFIX
            + ".ai.dynamic-skill.heartbeat-interval:10s}")
    public void heartbeat() {
        SkillProviderCatalogManifest current = catalog;
        if (!properties.enabled() || current == null) {
            return;
        }
        // 同时续期目录和正文，避免存活实例引用到已过期目录。
        redisRegistry.publish(identity, current, contents, properties.catalogTtl(),
                properties.heartbeatTtl(), false);
    }

    @Scheduled(
            initialDelayString = "${" + CoreConstants.Yaml.PROJECT_CONFIG_PREFIX
                    + ".ai.dynamic-skill.registration-refresh-initial-delay:20s}",
            fixedDelayString = "${" + CoreConstants.Yaml.PROJECT_CONFIG_PREFIX
                    + ".ai.dynamic-skill.registration-refresh-interval:20s}")
    public void refreshRegistration() {
        if (properties.enabled()) {
            rebuildAndPublish();
        }
    }

    private CatalogBuildResult buildCatalog() {
        Map<String, io.agentscope.core.tool.AgentTool> localTools = new LinkedHashMap<>();
        Map<String, SkillResourceProvider> localResources = new LinkedHashMap<>();
        Map<String, String> skillContents = new LinkedHashMap<>();
        List<SkillRegistrationManifest> registrations = new ArrayList<>();
        List<StandaloneToolManifest> standaloneTools = new ArrayList<>();
        List<AgentSkillBuilder.Configuration> configurations = providers.stream()
                .map(this::configure)
                .toList();

        configurations.stream()
                .flatMap(configuration -> configuration.skills().stream())
                .sorted(Comparator.comparing(value -> value.skill().skillId()))
                .forEach(registration -> {
                    SkillDefinition skill = registration.skill();
                    SkillToolNaming.validateSkillId(identity.applicationName(), skill.skillId());
                    if (registrations.stream().anyMatch(v -> v.skillId().equals(skill.skillId()))) {
                        throw new IllegalStateException("当前服务存在重复 skillId：" + skill.skillId());
                    }
                    List<NormalizedSkillTool> normalizedTools = registration.toolSources().stream()
                            .flatMap(source -> toolNormalizer.normalize(
                                    identity.applicationName(), source).stream())
                            .toList();
                    normalizedTools.forEach(tool -> {
                        if (localTools.putIfAbsent(tool.exposedName(), tool.localTool()) != null) {
                            throw new IllegalStateException("当前服务存在重复动态工具名：" + tool.exposedName());
                        }
                    });
                    validateResources(registration.resourceProvider().list());
                    String contentKey = SkillRegistryKeys.content(
                            identity.applicationName(), skill.skillId(), skill.version());
                    String content = skill.contentLoader().get();
                    if (!StringUtils.hasText(content)) {
                        throw new IllegalStateException("Skill 正文不能为空：" + skill.skillId());
                    }
                    skillContents.put(contentKey, content);
                    localResources.put(skill.skillId(), registration.resourceProvider());
                    registrations.add(new SkillRegistrationManifest(
                            identity.applicationName(), skill.skillId(),
                            skill.description(), skill.version(), contentKey, skill.requiredPermission(),
                            registration.resourceProvider().list(),
                            normalizedTools.stream().map(NormalizedSkillTool::manifest).toList()));
                });

        configurations.stream()
                .flatMap(configuration -> configuration.tools().stream())
                .flatMap(source -> toolNormalizer.normalize(identity.applicationName(), source).stream())
                .sorted(Comparator.comparing(NormalizedSkillTool::exposedName))
                .forEach(tool -> {
                    if (localTools.putIfAbsent(tool.exposedName(), tool.localTool()) != null) {
                        throw new IllegalStateException("当前服务存在重复动态工具名：" + tool.exposedName());
                    }
                    standaloneTools.add(new StandaloneToolManifest(
                            identity.applicationName(), tool.manifest()));
                });

        localToolRegistry.replaceProviderTools(identity.applicationName(), localTools);
        localResourceRegistry.replaceProviderResources(identity.applicationName(), localResources);
        String catalogPayload = json(registrations) + json(standaloneTools)
                + json(skillContents.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> SkillDigestUtils.sha256(entry.getValue()),
                        (left, right) -> left,
                        java.util.TreeMap::new)));
        String toolkitPayload = json(registrations.stream()
                .map(value -> Map.of(
                        "applicationName", value.applicationName(),
                        "skillId", value.skillId(),
                        "tools", value.tools()))
                .toList()) + json(standaloneTools);
        String catalogRevision = SkillDigestUtils.sha256(catalogPayload);
        String toolkitRevision = SkillDigestUtils.sha256(toolkitPayload);
        return new CatalogBuildResult(
                new SkillProviderCatalogManifest(identity.applicationName(),
                        catalogRevision, toolkitRevision, registrations, standaloneTools),
                Map.copyOf(skillContents));
    }

    private AgentSkillBuilder.Configuration configure(AgentSkillProvider provider) {
        AgentSkillBuilder builder = new AgentSkillBuilder(identity.applicationName());
        provider.configure(builder);
        return builder.build();
    }

    private void validateResources(List<SkillResourceManifest> resources) {
        for (SkillResourceManifest resource : resources) {
            SkillResourcePathValidator.validate(resource.path());
            if (!StringUtils.hasText(resource.digest())) {
                throw new IllegalStateException("动态技能资源 digest 不能为空：" + resource.path());
            }
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            throw new IllegalStateException("计算动态技能版本失败", ex);
        }
    }

    private record CatalogBuildResult(
            SkillProviderCatalogManifest catalog,
            Map<String, String> contents
    ) {
    }
}
