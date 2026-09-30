package org.quyq.gwsu.common.ai.mcp.endpoint;

import java.util.Collection;
import java.util.Optional;

public interface McpEndpointRegistry {

    Optional<McpEndpointRuntime> find(String path);

    Collection<McpEndpointRuntime> endpoints();
}
