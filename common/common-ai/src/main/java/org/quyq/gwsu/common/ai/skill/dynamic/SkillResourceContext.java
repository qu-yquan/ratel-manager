package org.quyq.gwsu.common.ai.skill.dynamic;

import io.agentscope.core.agent.RuntimeContext;

public record SkillResourceContext(RuntimeContext runtimeContext) {

    public static SkillResourceContext empty() {
        return new SkillResourceContext(null);
    }
}
