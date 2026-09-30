package org.quyq.gwsu.common.ai.mcp.server;

import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointRegistry;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointRuntime;

import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

/**
 * 稳定注册到 Spring MVC 的组合路由，实际 Endpoint 在所有单例创建完成后填充。
 */
public final class McpCompositeRouterFunction implements RouterFunction<ServerResponse> {

    private final McpEndpointRegistry endpointRegistry;

    public McpCompositeRouterFunction(McpEndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        for (McpEndpointRuntime runtime : endpointRegistry.endpoints()) {
            Optional<HandlerFunction<ServerResponse>> handler =
                    runtime.routerFunction().route(request);
            if (handler.isPresent()) {
                return handler;
            }
        }
        return Optional.empty();
    }
}
