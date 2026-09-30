package org.quyq.gwsu.common.ai.skill.dynamic;

import java.util.List;
import java.util.Objects;

public record SkillRegistration(
        SkillDefinition skill,
        List<AgentToolSource> toolSources,
        SkillResourceProvider resourceProvider
) {
    public SkillRegistration {
        Objects.requireNonNull(skill, "skill 不能为空");
        toolSources = toolSources == null ? List.of() : List.copyOf(toolSources);
        resourceProvider = resourceProvider == null ? SkillResourceProvider.empty() : resourceProvider;
    }
}
