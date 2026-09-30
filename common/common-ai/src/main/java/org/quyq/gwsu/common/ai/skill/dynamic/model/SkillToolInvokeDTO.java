package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.Map;

public record SkillToolInvokeDTO(
        String skillId,
        String toolName,
        String toolCallId,
        Map<String, Object> arguments,
        SkillRuntimeContextDTO runtimeContext
) {
    public SkillToolInvokeDTO {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        runtimeContext = runtimeContext == null ? SkillRuntimeContextDTO.empty() : runtimeContext;
    }
}
