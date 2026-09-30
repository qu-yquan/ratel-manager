package org.quyq.gwsu.common.ai.skill.dynamic.model;

public record SkillResourceReadDTO(
        String skillId,
        String path,
        SkillRuntimeContextDTO runtimeContext
) {
    public SkillResourceReadDTO {
        runtimeContext = runtimeContext == null ? SkillRuntimeContextDTO.empty() : runtimeContext;
    }
}
