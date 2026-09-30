package org.quyq.gwsu.common.ai.mcp.server;

import org.quyq.gwsu.common.ai.mcp.config.McpProperties;
import org.quyq.gwsu.common.ai.mcp.context.McpRequestContext;
import org.quyq.gwsu.common.ai.mcp.context.McpRequestContextExtractor;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointDescriptor;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointRuntime;
import org.quyq.gwsu.common.ai.mcp.tool.McpAgentToolAdapter;
import org.quyq.gwsu.common.ai.skill.dynamic.AgentToolSource;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.web.ModuleEndpointPathResolver;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.StatelessAsyncSpecification;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class McpEndpointRuntimeFactory {

    private final McpProperties properties;
    private final ProjectUtils projectUtils;
    private final AgentScopeToolNormalizer toolNormalizer;
    private final McpAgentToolAdapter toolAdapter;
    private final McpJsonMapper jsonMapper;
    private final McpRequestContextExtractor requestContextExtractor;
    private final ModuleEndpointPathResolver pathResolver;

    public McpEndpointRuntimeFactory(
            McpProperties properties,
            ProjectUtils projectUtils,
            AgentScopeToolNormalizer toolNormalizer,
            McpAgentToolAdapter toolAdapter,
            McpJsonMapper jsonMapper,
            McpRequestContextExtractor requestContextExtractor,
            ModuleEndpointPathResolver pathResolver) {
        this.properties = properties;
        this.projectUtils = projectUtils;
        this.toolNormalizer = toolNormalizer;
        this.toolAdapter = toolAdapter;
        this.jsonMapper = jsonMapper;
        this.requestContextExtractor = requestContextExtractor;
        this.pathResolver = pathResolver;
    }

    public McpEndpointRuntime create(McpEndpointDescriptor descriptor) {
        String applicationName = projectUtils.getApplicationName();
        if (!StringUtils.hasText(applicationName)) {
            applicationName = "application";
        }
        List<NormalizedSkillTool> normalizedTools = toolNormalizer.normalize(
                applicationName,
                new AgentToolSource.AnnotatedBean(
                        descriptor.bean(), descriptor.enabledTools()));
        if (normalizedTools.isEmpty()) {
            throw new IllegalStateException("MCP Endpoint 未发现 @Tool："
                    + descriptor.targetClass().getName());
        }
        ensureUniqueToolNames(descriptor, normalizedTools);

        String path = pathResolver.resolve(
                descriptor.targetClass(), properties.endpointPath(descriptor.endpointId()));
        WebMvcStatelessServerTransport transport = WebMvcStatelessServerTransport.builder()
                .jsonMapper(jsonMapper)
                .messageEndpoint(path)
                .contextExtractor(request -> {
                    McpRequestContext context = Objects.requireNonNull(
                            requestContextExtractor.extract(request),
                            "McpRequestContextExtractor 不能返回 null");
                    return McpTransportContext.create(Map.of(
                            McpAgentToolAdapter.REQUEST_CONTEXT_KEY, context));
                })
                .build();
        List<McpStatelessServerFeatures.AsyncToolSpecification> toolSpecifications =
                normalizedTools.stream()
                        .map(tool -> toolAdapter.adapt(descriptor, tool))
                        .toList();
        String serverName = StringUtils.hasText(descriptor.name())
                ? descriptor.name() : applicationName + ":" + descriptor.endpointId();
        StatelessAsyncSpecification specification = McpServer.async(transport)
                .serverInfo(serverName, descriptor.version())
                .requestTimeout(properties.requestTimeout())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(false)
                        .build())
                .tools(toolSpecifications);
        if (StringUtils.hasText(descriptor.instructions())) {
            specification.instructions(descriptor.instructions());
        }
        McpStatelessAsyncServer server = specification.build();
        return new McpEndpointRuntime(
                descriptor, path, server, transport.getRouterFunction());
    }

    private void ensureUniqueToolNames(
            McpEndpointDescriptor descriptor,
            List<NormalizedSkillTool> tools) {
        Set<String> names = new HashSet<>();
        for (NormalizedSkillTool tool : tools) {
            if (!names.add(tool.originalName())) {
                throw new IllegalStateException("MCP Endpoint 工具名重复："
                        + descriptor.endpointId() + "/" + tool.originalName());
            }
        }
    }
}
