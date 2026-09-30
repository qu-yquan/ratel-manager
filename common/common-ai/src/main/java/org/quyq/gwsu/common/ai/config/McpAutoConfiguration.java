package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.mcp.config.McpProperties;
import org.quyq.gwsu.common.ai.mcp.context.DefaultMcpRequestContextExtractor;
import org.quyq.gwsu.common.ai.mcp.context.McpRequestContextExtractor;
import org.quyq.gwsu.common.ai.mcp.endpoint.DefaultMcpEndpointRegistry;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointBeanPostProcessor;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointCollector;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointRegistry;
import org.quyq.gwsu.common.ai.mcp.server.McpCompositeRouterFunction;
import org.quyq.gwsu.common.ai.mcp.server.McpEndpointRuntimeFactory;
import org.quyq.gwsu.common.ai.mcp.tool.McpAgentToolAdapter;
import org.quyq.gwsu.common.ai.mcp.tool.McpToolResultMapper;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.web.ModuleEndpointPathResolver;
import org.quyq.gwsu.common.security.utils.SecurityUtils;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@AutoConfiguration(after = {AgentscopeConfiguration.class, DynamicSkillConfiguration.class})
@ConditionalOnClass(WebMvcStatelessServerTransport.class)
@ConditionalOnProperty(
        prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".ai.mcp",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(McpProperties.class)
@ImportRuntimeHints(McpRuntimeHints.class)
public class McpAutoConfiguration {

    @Bean
    static McpEndpointCollector mcpEndpointCollector() {
        return new McpEndpointCollector();
    }

    @Bean
    static McpEndpointBeanPostProcessor mcpEndpointBeanPostProcessor(
            McpEndpointCollector collector) {
        return new McpEndpointBeanPostProcessor(collector);
    }

    @Bean
    @ConditionalOnMissingBean
    McpJsonMapper mcpJsonMapper() {
        return McpJsonMapper.createDefault();
    }

    @Bean
    @ConditionalOnMissingBean
    AgentScopeToolNormalizer mcpAgentScopeToolNormalizer() {
        return new AgentScopeToolNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    McpRequestContextExtractor mcpRequestContextExtractor(
            SecurityUtils securityUtils,
            McpProperties properties) {
        return new DefaultMcpRequestContextExtractor(securityUtils, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    McpToolResultMapper mcpToolResultMapper(McpJsonMapper jsonMapper) {
        return new McpToolResultMapper(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    McpAgentToolAdapter mcpAgentToolAdapter(
            McpJsonMapper jsonMapper,
            McpToolResultMapper resultMapper) {
        return new McpAgentToolAdapter(jsonMapper, resultMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    McpEndpointRuntimeFactory mcpEndpointRuntimeFactory(
            McpProperties properties,
            ProjectUtils projectUtils,
            AgentScopeToolNormalizer toolNormalizer,
            McpAgentToolAdapter toolAdapter,
            McpJsonMapper jsonMapper,
            McpRequestContextExtractor requestContextExtractor,
            ModuleEndpointPathResolver pathResolver) {
        return new McpEndpointRuntimeFactory(
                properties, projectUtils, toolNormalizer, toolAdapter,
                jsonMapper, requestContextExtractor, pathResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    McpEndpointRegistry mcpEndpointRegistry(
            McpEndpointCollector collector,
            McpEndpointRuntimeFactory runtimeFactory) {
        return new DefaultMcpEndpointRegistry(collector, runtimeFactory);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    RouterFunction<ServerResponse> mcpRouterFunction(
            McpEndpointRegistry endpointRegistry) {
        return new McpCompositeRouterFunction(endpointRegistry);
    }
}
