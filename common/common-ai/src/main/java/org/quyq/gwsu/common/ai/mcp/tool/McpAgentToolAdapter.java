package org.quyq.gwsu.common.ai.mcp.tool;

import org.quyq.gwsu.common.ai.mcp.context.McpRequestContext;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointDescriptor;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.tool.ToolCallParam;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public final class McpAgentToolAdapter {

    public static final String REQUEST_CONTEXT_KEY = McpRequestContext.class.getName();

    private static final Logger log = LoggerFactory.getLogger(McpAgentToolAdapter.class);

    private final McpJsonMapper jsonMapper;
    private final McpToolResultMapper resultMapper;

    public McpAgentToolAdapter(McpJsonMapper jsonMapper, McpToolResultMapper resultMapper) {
        this.jsonMapper = jsonMapper;
        this.resultMapper = resultMapper;
    }

    public McpStatelessServerFeatures.AsyncToolSpecification adapt(
            McpEndpointDescriptor endpoint,
            NormalizedSkillTool normalizedTool) {
        Map<String, Object> outputSchema = normalizedTool.localTool().getOutputSchema();
        boolean structuredOutput = endpoint.enableOutputSchema()
                && outputSchema != null && !outputSchema.isEmpty();
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
                .name(normalizedTool.originalName())
                .description(normalizedTool.localTool().getDescription())
                .inputSchema(jsonMapper, writeJson(normalizedTool.localTool().getParameters()))
                .annotations(new McpSchema.ToolAnnotations(
                        null,
                        normalizedTool.localTool().isReadOnly(),
                        !normalizedTool.localTool().isReadOnly(),
                        false,
                        true,
                        false));
        if (structuredOutput) {
            toolBuilder.outputSchema(outputSchema);
        }
        McpSchema.Tool tool = toolBuilder.build();
        return McpStatelessServerFeatures.AsyncToolSpecification.builder()
                .tool(tool)
                .callHandler((transportContext, request) -> invoke(
                        endpoint, normalizedTool, transportContext, request, structuredOutput))
                .build();
    }

    private Mono<McpSchema.CallToolResult> invoke(
            McpEndpointDescriptor endpoint,
            NormalizedSkillTool normalizedTool,
            McpTransportContext transportContext,
            McpSchema.CallToolRequest request,
            boolean structuredOutput) {
        Map<String, Object> arguments = request.arguments() == null
                ? Map.of() : request.arguments();
        McpRequestContext requestContext = resolveRequestContext(transportContext);
        ToolUseBlock useBlock = ToolUseBlock.builder()
                .id(UUID.randomUUID().toString())
                .name(normalizedTool.exposedName())
                .input(arguments)
                .build();
        ToolCallParam callParam = ToolCallParam.builder()
                .toolUseBlock(useBlock)
                .input(arguments)
                .runtimeContext(requestContext.toRuntimeContext())
                .build();
        return Mono.defer(() -> {
                    Mono<ToolResultBlock> execution =
                            normalizedTool.localTool().callAsync(callParam);
                    return execution == null
                            ? Mono.error(new IllegalStateException("AgentTool.callAsync 返回 null"))
                            : execution;
                })
                .map(result -> resultMapper.map(result, structuredOutput))
                .onErrorResume(exception -> {
                    log.error("MCP 工具调用失败，endpointId={}，tool={}",
                            endpoint.endpointId(), normalizedTool.originalName(), exception);
                    return Mono.just(resultMapper.error("工具执行失败"));
                });
    }

    private McpRequestContext resolveRequestContext(McpTransportContext transportContext) {
        Object value = transportContext == null ? null : transportContext.get(REQUEST_CONTEXT_KEY);
        return value instanceof McpRequestContext context
                ? context : new McpRequestContext(null, null);
    }

    private String writeJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("AgentTool Schema 无法转换为 MCP Schema", exception);
        }
    }
}
