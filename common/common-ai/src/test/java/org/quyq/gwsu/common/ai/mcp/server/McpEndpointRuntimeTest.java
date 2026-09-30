package org.quyq.gwsu.common.ai.mcp.server;

import org.quyq.gwsu.common.ai.mcp.config.McpProperties;
import org.quyq.gwsu.common.ai.mcp.context.McpRequestContext;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointDescriptor;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointRuntime;
import org.quyq.gwsu.common.ai.mcp.tool.McpAgentToolAdapter;
import org.quyq.gwsu.common.ai.mcp.tool.McpToolResultMapper;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.web.ModuleEndpointPathResolver;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class McpEndpointRuntimeTest {

    @Test
    void shouldServeInitializeAndListToolsOverRouterFunction() throws Exception {
        McpEndpointRuntime runtime = createRuntime(false);
        MockMvc mockMvc = MockMvcBuilders.routerFunctions(runtime.routerFunction()).build();

        try {
            var initialize = mockMvc.perform(post("/mcp/echo")
                            .contentType("application/json")
                            .accept("application/json", "text/event-stream")
                            .content("""
                                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
                                      "protocolVersion":"2025-06-18",
                                      "capabilities":{},
                                      "clientInfo":{"name":"test","version":"1.0.0"}
                                    }}
                                    """))
                    .andReturn().getResponse();
            assertEquals(200, initialize.getStatus());
            assertTrue(initialize.getContentType().startsWith("application/json"));
            assertTrue(initialize.getContentAsString().contains("\"name\":\"Echo MCP\""));

            var tools = mockMvc.perform(post("/mcp/echo")
                            .contentType("application/json")
                            .accept("application/json", "text/event-stream")
                            .content("""
                                    {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
                                    """))
                    .andReturn().getResponse();
            assertEquals(200, tools.getStatus());
            assertTrue(tools.getContentAsString().contains("\"name\":\"Echo\""));

            var call = mockMvc.perform(post("/mcp/echo")
                            .contentType("application/json")
                            .accept("application/json", "text/event-stream")
                            .content("""
                                    {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{
                                      "name":"Echo","arguments":{"value":"hello"}
                                    }}
                                    """))
                    .andReturn().getResponse();
            assertEquals(200, call.getStatus());
            assertTrue(call.getContentAsString().contains("hello"));
            assertTrue(call.getContentAsString().contains("\"isError\":false"));
        } finally {
            runtime.close();
        }
    }

    @Test
    void shouldAddBusinessModulePrefixInSingleDeployment() {
        McpEndpointRuntime runtime = createRuntime(true);
        try {
            assertEquals("/security/mcp/echo", runtime.path());
            assertEquals("/security/mcp/echo", runtime.registryKey());
        } finally {
            runtime.close();
        }
    }

    private McpEndpointRuntime createRuntime(boolean single) {
        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getApplicationName()).thenReturn("security");
        Environment environment = mock(Environment.class);
        when(environment.getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class))
                .thenReturn(single);
        ModuleEndpointPathResolver pathResolver =
                new ModuleEndpointPathResolver(
                        environment, List.of(new TestModuleInfoProvider()));
        McpJsonMapper jsonMapper = McpJsonMapper.createDefault();
        McpEndpointRuntimeFactory factory = new McpEndpointRuntimeFactory(
                McpProperties.defaults(),
                projectUtils,
                new AgentScopeToolNormalizer(),
                new McpAgentToolAdapter(jsonMapper, new McpToolResultMapper(jsonMapper)),
                jsonMapper,
                request -> new McpRequestContext("user-1", "session-1"),
                pathResolver);
        EchoEndpoint endpoint = new EchoEndpoint();
        return factory.create(new McpEndpointDescriptor(
                "echo", "Echo MCP", "1.0.0", "", true,
                endpoint, EchoEndpoint.class, Set.of()));
    }

    private static final class TestModuleInfoProvider
            implements BusinessModuleInfoProvider {

        @Override
        public BusinessModuleInfo module() {
            return new BusinessModuleInfo("security", "安全服务");
        }
    }

    private static final class EchoEndpoint {

        @Tool(name = "Echo", description = "回显文本", readOnly = true)
        public Mono<String> echo(
                @ToolParam(name = "value", description = "内容") String value) {
            return Mono.just(value);
        }
    }
}
