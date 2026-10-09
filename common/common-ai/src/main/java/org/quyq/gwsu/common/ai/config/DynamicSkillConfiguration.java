package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.SkillVisibilityPolicy;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.DynamicSkillEventListener;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.DynamicSkillRegistrar;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.RedisSkillRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.RedisSkillRegistrySynchronizer;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillProviderIdentityResolver;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillRegistryProperties;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.DynamicSkillResourceService;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.LocalSkillResourceRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.RedisDynamicAgentSkillRepository;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;
import org.quyq.gwsu.common.ai.skill.dynamic.runtime.AgentRuntimeManager;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.DynamicSkillToolBinder;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.LocalSkillToolRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.RemoteSkillToolInvoker;
import org.quyq.gwsu.common.ai.skill.dynamic.web.DynamicSkillInternalHandler;

import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.handler.GlobalExceptionFunctionHandler;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@AutoConfiguration(after = AgentscopeConfiguration.class)
@ConditionalOnClass(name = "io.agentscope.core.skill.repository.RuntimeContextSkillRepository")
@EnableScheduling
@EnableConfigurationProperties(SkillRegistryProperties.class)
@ImportRuntimeHints(DynamicSkillRuntimeHints.class)
public class DynamicSkillConfiguration {

    private static final String INTERNAL_AI_BASE_PATH = "/internal/ai";

    @Bean
    @ConditionalOnMissingBean
    public SkillProviderIdentityResolver skillProviderIdentityResolver(ProjectUtils projectUtils) {
        return new SkillProviderIdentityResolver(projectUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentScopeToolNormalizer agentScopeToolNormalizer() {
        return new AgentScopeToolNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalSkillToolRegistry localSkillToolRegistry() {
        return new LocalSkillToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalSkillResourceRegistry localSkillResourceRegistry() {
        return new LocalSkillResourceRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisSkillRegistry redisSkillRegistry(CacheUtils cacheUtils) {
        return new RedisSkillRegistry(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public SkillCatalogHolder skillCatalogHolder() {
        return new SkillCatalogHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentRuntimeManager agentRuntimeManager() {
        return new AgentRuntimeManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public SkillVisibilityPolicy skillVisibilityPolicy() {
        // 未配置业务权限解析器时，只暴露无需额外权限的 Skill，避免误放行。
        return (manifest, context) -> !StringUtils.hasText(manifest.requiredPermission());
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSkillRegistrar dynamicSkillRegistrar(
            List<AgentSkillProvider> providers,
            SkillProviderIdentityResolver identityResolver,
            AgentScopeToolNormalizer toolNormalizer,
            LocalSkillToolRegistry localToolRegistry,
            LocalSkillResourceRegistry localResourceRegistry,
            RedisSkillRegistry redisRegistry,
            SkillRegistryProperties properties,
            ObjectMapper objectMapper) {
        return new DynamicSkillRegistrar(
                providers, identityResolver, toolNormalizer, localToolRegistry,
                localResourceRegistry, redisRegistry, properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisSkillRegistrySynchronizer redisSkillRegistrySynchronizer(
            RedisSkillRegistry redisRegistry,
            SkillCatalogHolder catalogHolder,
            ApplicationEventPublisher eventPublisher,
            SkillRegistryProperties properties) {
        return new RedisSkillRegistrySynchronizer(
                redisRegistry, catalogHolder, eventPublisher, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSkillEventListener dynamicSkillEventListener(
            CacheUtils cacheUtils,
            RedisSkillRegistrySynchronizer synchronizer,
            SkillRegistryProperties properties) {
        return new DynamicSkillEventListener(cacheUtils, synchronizer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSkillResourceService dynamicSkillResourceService(
            SkillProviderIdentityResolver identityResolver,
            LocalSkillResourceRegistry localResourceRegistry,
            RestClient.Builder restClientBuilder,
            SkillRegistryProperties properties) {
        return new DynamicSkillResourceService(
                identityResolver, localResourceRegistry, restClientBuilder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisDynamicAgentSkillRepository redisDynamicAgentSkillRepository(
            SkillCatalogHolder catalogHolder,
            SkillVisibilityPolicy visibilityPolicy,
            DynamicSkillResourceService resourceService) {
        return new RedisDynamicAgentSkillRepository(catalogHolder, visibilityPolicy, resourceService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RemoteSkillToolInvoker remoteSkillToolInvoker(
            RestClient.Builder restClientBuilder,
            SkillRegistryProperties properties) {
        return new RemoteSkillToolInvoker(restClientBuilder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSkillToolBinder dynamicSkillToolBinder(
            SkillCatalogHolder catalogHolder,
            SkillProviderIdentityResolver identityResolver,
            LocalSkillToolRegistry localToolRegistry,
            RemoteSkillToolInvoker remoteInvoker) {
        return new DynamicSkillToolBinder(
                catalogHolder, identityResolver, localToolRegistry, remoteInvoker);
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicSkillInternalHandler dynamicSkillInternalHandler(
            LocalSkillToolRegistry localToolRegistry,
            LocalSkillResourceRegistry localResourceRegistry,
            SkillCatalogHolder catalogHolder ,
            ObjectMapper objectMapper) {
        return new DynamicSkillInternalHandler(
                localToolRegistry, localResourceRegistry, catalogHolder , objectMapper);
    }

    @Bean
    public RouterFunction<ServerResponse> dynamicSkillInternalRoutes(
            DynamicSkillInternalHandler handler) {
        return RouterFunctions
                .route(
                        RequestPredicates.POST(INTERNAL_AI_BASE_PATH + "/tools/invoke")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        handler::invoke)
                .andRoute(
                        RequestPredicates.POST(INTERNAL_AI_BASE_PATH + "/skills/resources/read")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        handler::readResource)
                .filter(new GlobalExceptionFunctionHandler());
    }
}
