package org.quyq.gwsu.common.ai.skill.dynamic.model;

import io.agentscope.core.agent.RuntimeContext;

/**
 * 允许在动态 Skill 内部接口间传递的最小运行上下文。
 *
 * <p>AgentState、ToolExecutionContext 和 extra 均属于进程内状态，不参与远程传输。</p>
 */
public record SkillRuntimeContextDTO(
        String userId,
        String sessionId
) {

    public static SkillRuntimeContextDTO empty() {
        return new SkillRuntimeContextDTO(null, null);
    }

    public static SkillRuntimeContextDTO from(RuntimeContext context) {
        if (context == null) {
            return empty();
        }
        return new SkillRuntimeContextDTO(context.getUserId(), context.getSessionId());
    }

    public RuntimeContext toRuntimeContext() {
        return RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
}
