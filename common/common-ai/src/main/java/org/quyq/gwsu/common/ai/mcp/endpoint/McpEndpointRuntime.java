package org.quyq.gwsu.common.ai.mcp.endpoint;

import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Objects;

public record McpEndpointRuntime(
        McpEndpointDescriptor descriptor,
        String path,
        McpStatelessAsyncServer server,
        RouterFunction<ServerResponse> routerFunction
) implements AutoCloseable {

    public McpEndpointRuntime {
        Objects.requireNonNull(descriptor, "descriptor 不能为空");
        Objects.requireNonNull(path, "path 不能为空");
        Objects.requireNonNull(server, "server 不能为空");
        Objects.requireNonNull(routerFunction, "routerFunction 不能为空");
    }

    public String registryKey() {
        return path;
    }

    @Override
    public void close() {
        server.close();
    }
}
