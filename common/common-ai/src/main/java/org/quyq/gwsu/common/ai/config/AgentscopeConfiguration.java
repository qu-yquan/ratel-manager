package org.quyq.gwsu.common.ai.config;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.harness.agent.DistributedStore;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import org.quyq.gwsu.common.ai.agui.converter.ApprovalInterruptEventConverter;
import org.quyq.gwsu.common.ai.agui.serde.AgentscopeAguiJackson3Module;
import org.quyq.gwsu.common.ai.agui.adapter.RatelAguiAgentAdapter;
import org.quyq.gwsu.common.ai.agui.registration.AguiAgentRegistrar;
import org.quyq.gwsu.common.ai.agui.resolver.MultiAgentResolver;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.distributed.redis.CacheRedisAgentStateStore;
import org.quyq.gwsu.common.ai.distributed.redis.CacheRedisDistributedStore;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * @author Quyq
 * @date 2026/4/22
 * @description
 */
@AutoConfiguration
@ConditionalOnClass(ReActAgent.class)
public class AgentscopeConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer agentscopeAguiJsonMapperBuilderCustomizer() {
        return builder -> builder.addModule(new AgentscopeAguiJackson3Module());
    }


    @Bean
    @ConditionalOnMissingBean
    public AgentStateStore agentStateStore(CacheUtils cacheUtils) {
        return new CacheRedisAgentStateStore(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public AguiRequestProcessor aguiRequestProcessor(ObjectProvider<AguiAgentRegistrar> registrars) {
        AguiAgentRegistry registry = new AguiAgentRegistry();
        registrars.orderedStream().forEach(registrar -> registrar.register(registry));

        return AguiRequestProcessor.builder()
                .agentResolver(new MultiAgentResolver(registry))
                .adapterFactory(RatelAguiAgentAdapter::new)
                .runtimeContextResolver(request ->
                        request.getNativeRequest() instanceof RuntimeContext runtimeContext
                                ? runtimeContext
                                : RuntimeContext.builder().build())
                .config(AguiAdapterConfig.builder()
                        .addEventConverter(new ApprovalInterruptEventConverter())
                        .enableReasoning(true)
                        .emitRunFinishedAfterError(false)
                        .emitSubagentEventsAsNative(false)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Toolkit agentscopeToolkit() {
        return new Toolkit(ToolkitConfig.builder()
                .parallel(true)
                .build());
    }


    @Bean
    public WebToolUtils webToolUtils(CacheUtils cacheUtils) {
        return new WebToolUtils(cacheUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedStore distributedStore(AgentStateStore agentStateStore, CacheUtils cacheUtils) {
        return new CacheRedisDistributedStore(agentStateStore, cacheUtils);
    }


}
