package org.quyq.gwsu.common.ai.mcp.tool;

import org.quyq.gwsu.common.ai.mcp.context.McpRequestContext;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointDescriptor;
import org.quyq.gwsu.common.ai.skill.dynamic.AgentToolSource;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpAgentToolAdapterTest {

    @Test
    void shouldExposeOriginalNameAndPassRuntimeContext() {
        ContextAwareTool bean = new ContextAwareTool();
        NormalizedSkillTool normalized = new AgentScopeToolNormalizer()
                .normalize("security", new AgentToolSource.AnnotatedBean(bean))
                .getFirst();
        McpJsonMapper jsonMapper = McpJsonMapper.createDefault();
        McpAgentToolAdapter adapter = new McpAgentToolAdapter(
                jsonMapper, new McpToolResultMapper(jsonMapper));
        McpEndpointDescriptor descriptor = new McpEndpointDescriptor(
                "test", "Test", "1.0.0", "", true,
                bean, ContextAwareTool.class, Set.of());

        McpStatelessServerFeatures.AsyncToolSpecification specification =
                adapter.adapt(descriptor, normalized);
        McpTransportContext transportContext = McpTransportContext.create(Map.of(
                McpAgentToolAdapter.REQUEST_CONTEXT_KEY,
                new McpRequestContext("user-1", "session-1")));
        McpSchema.CallToolResult result = specification.callHandler()
                .apply(transportContext,
                        new McpSchema.CallToolRequest(
                                "Echo", Map.of("value", "hello")))
                .block();

        assertEquals("Echo", specification.tool().name());
        assertEquals("security_ContextAware_Echo", normalized.exposedName());
        assertFalse(result.isError(), result.toString());
        McpSchema.TextContent content = assertInstanceOf(
                McpSchema.TextContent.class, result.content().getFirst());
        assertTrue(content.text().contains("user-1/session-1/hello"));
    }

    private static final class ContextAwareTool {

        @Tool(name = "Echo", description = "回显上下文", readOnly = true)
        public Mono<String> echo(
                @ToolParam(name = "value", description = "内容") String value,
                RuntimeContext runtimeContext) {
            return Mono.just("%s/%s/%s".formatted(
                    runtimeContext.getUserId(),
                    runtimeContext.getSessionId(),
                    value));
        }
    }
}
