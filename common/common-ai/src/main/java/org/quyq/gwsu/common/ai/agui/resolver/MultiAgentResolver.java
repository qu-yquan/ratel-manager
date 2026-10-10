package org.quyq.gwsu.common.ai.agui.resolver;


import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.processor.AgentResolver;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import lombok.RequiredArgsConstructor;

/**
 * @author Quyq
 * @date 2026/7/31
 * @description
 */
@RequiredArgsConstructor
public class MultiAgentResolver implements AgentResolver {

    private final AguiAgentRegistry registry;

    @Override
    public Agent resolveAgent(String agentId, String threadId) {
        return registry.getAgent(agentId)
                .orElseThrow(() -> new AguiException.AgentNotFoundException(agentId));
    }

    @Override
    public Agent resolveAgent(String agentId, String threadId, String userId) {
        return resolveAgent(agentId, threadId);
    }

    @Override
    public boolean hasMemory(RuntimeContext runtimeContext) {
        // 项目中的 AgentStateStore 按 userId/sessionId 管理上下文；首次请求没有 assistant
        // 消息时官方 Processor 会保留完整输入，后续请求仅提取上一轮后的新增消息。
        return true;
    }
}
