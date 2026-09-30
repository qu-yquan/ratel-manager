package org.quyq.gwsu.common.ai.mcp.context;

import io.agentscope.core.agent.RuntimeContext;

public record McpRequestContext(String userId, String sessionId) {

    public RuntimeContext toRuntimeContext() {
        return RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
}
