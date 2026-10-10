package org.quyq.gwsu.common.ai.agui.registration;

import io.agentscope.core.agui.registry.AguiAgentRegistry;

/**
 * 项目内 AG-UI 智能体注册扩展点。
 */
@FunctionalInterface
public interface AguiAgentRegistrar {

    /**
     * 向 AgentScope 官方注册表注册智能体。
     *
     * @param registry 官方 AG-UI 智能体注册表
     */
    void register(AguiAgentRegistry registry);
}
