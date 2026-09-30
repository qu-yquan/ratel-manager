package org.quyq.gwsu.common.ai.mcp.context;

import org.springframework.web.servlet.function.ServerRequest;

@FunctionalInterface
public interface McpRequestContextExtractor {

    McpRequestContext extract(ServerRequest request);
}
